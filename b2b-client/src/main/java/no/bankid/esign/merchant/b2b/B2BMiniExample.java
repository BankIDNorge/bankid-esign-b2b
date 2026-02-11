package no.bankid.esign.merchant.b2b;

import no.bankid.esign.feign.api.b2b.v0.model.*;
import no.bankid.esign.merchant.b2b.feignclients.OAuth2TokenApi.TokenResponse;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;

import static no.bankid.esign.merchant.b2b.environment.Globals.VARS;

public class B2BMiniExample {

    public static void main(String[] args) throws Exception {
        var test = new B2BMiniExample();
        VARS.showPublicKeyJwk();
        test.doASimpleTextSigning();
        test.doBuildSdoFromCms(0,2,1,3,2,2,2);
        test.doBuildSdoFromCms(0,2,4,1,3,5);
        test.doSomePdfSigning("small-ex.pdf", "csc-pades-demo.pdf");

        String hash1 = sha256("Olav den hellige og æren.");
        String hash2 = sha256("Juletrær har vi egentlig nok av");
        test.doAHashSigningStandardFormat(hash1, hash2);
        test.doAHashSigningPAdESFormat(hash1, hash2);
    }

    /**
     * Builds an SDO from a text string using BankID OIDC b2b signing.
     * The SDO is signed by BankID merchant certificate connected to the clientId in the
     * access token got from the BankID OIDC server.
     */
    private void doASimpleTextSigning() {
        // Here is what we will sign:
        SdoFromTextRequest sdoFromTextRequest = new SdoFromTextRequest()
            .textToSign("Olav den hellige og æren.")
            .description("OlavDenHellige");

        // Call BankID OIDC which builds an access token (possibly bound to a DPoP proof)
        // The client id and secret are provided by the bankIDOIDCServer
        // Stronger authentication then just a client_secret may be required,
        // ex: private_key_jwt which uses a private key pr. clientId,
        //     public keys are registered in or accessible from the OIDC server
        TokenResponse tokenResponse = VARS.getAccessTokenFromKeycloak();

        // Call the signer endpoint
        String sdoB64 = VARS.b2bSigner.b2BSignApi()
                .withATAndDPoP(tokenResponse.access_token, VARS.b2bSigner.sdoFromTextUri())
            .sdoFromText(sdoFromTextRequest)
            .getSdo();

        // Dump the SDO as base64 and as pretty printed xml
        dumpSdo(sdoB64);
    }

    /**
     * Builds SDOs from pdf documents using BankID OIDC b2b signing.
     */
    private void doSomePdfSigning(String... fns) throws Exception {
        // Here is what we will sign:
        SdosFromDocumentsRequest sdosFromDocumentsRequest = new SdosFromDocumentsRequest();

        for (String fn : fns) {
            Path filePath = Paths.get("src/main/resources/" + fn);

            byte[] fileContent = null;
            try {
                fileContent = Files.readAllBytes(filePath);
            } catch (IOException e) {
                throw new RuntimeException("Working directory should be the 'b2b-client' directory to find the example files", e);
            }

            TbsDocument tbsDocumentsItem = new TbsDocument()
                    .pdf(Base64.getEncoder().encodeToString(fileContent))
                    .description("Filen er " + fn);
            sdosFromDocumentsRequest.addTbsDocumentsItem(tbsDocumentsItem);
        }

        TokenResponse tokenResponse = VARS.getAccessTokenFromKeycloak();
        // Call the signer endpoint
        VARS.b2bSigner
            .b2BSignApi()
                .withATAndDPoP(tokenResponse.access_token, VARS.b2bSigner.sdosFromDocsUri())
            .sdosFromDocuments(sdosFromDocumentsRequest)
            .forEach(hashAndSdo -> dumpSdo(hashAndSdo.getSdo()));
    }

    private void doAHashSigningPAdESFormat(String ... hashes) {
        TokenResponse tokenResponse = VARS.getAccessTokenFromKeycloak();

        CmsesFromHashes200Response cmsesFromHashes200Response = VARS.b2bSigner.b2BSignApi()
                .withATAndDPoP(tokenResponse.access_token, VARS.b2bSigner.padesCmsesFromHashesUri())
                .padesCmsesFromHashes(Arrays.asList(hashes));
        dumpCmsFromHashes("Pades", cmsesFromHashes200Response);
    }

    private void doAHashSigningStandardFormat(String ... hashes) {
        TokenResponse tokenResponse = VARS.getAccessTokenFromKeycloak();

        CmsesFromHashes200Response cmsesFromHashes200Response = VARS.b2bSigner.b2BSignApi()
                .withATAndDPoP(tokenResponse.access_token, VARS.b2bSigner.cmsesFromHashesUri())
                .cmsesFromHashes(Arrays.asList(hashes));

        dumpCmsFromHashes("BankID", cmsesFromHashes200Response);

    }

    private static void dumpCmsFromHashes(String type, CmsesFromHashes200Response cmsesFromHashes200Response) {
        cmsesFromHashes200Response.getSignResults().forEach(hashAndCms -> {
            System.out.println(type + " Hash: " + hashAndCms.getSignedHash());
            System.out.println(type + " CMS: " + hashAndCms.getCms());
        });
        System.out.println(type + " OCSP: " + cmsesFromHashes200Response.getClientOcsp());
    }

    /**
     * Builds an SDO from a set of existing signatures and ocsp responses.
     * All signatures are over the same data.
     * <p>
     * The SDO is sealed by the BankID merchant certificate connected to the clientId in the access token
     * got from the BankID OIDC server.
     */
    private void doBuildSdoFromCms(int ... includedSignatures) {
        TbsDocument tbsDocument = new TbsDocument()
            .textToSign(CmsAndOcspExampleData.TEXT_SIGNED)
            .description(CmsAndOcspExampleData.TEXT_DESCRIPTION);

        List<CmsAndOcsp> signatureList = new ArrayList<>();
        for (int includedSignature : includedSignatures) {
            signatureList.add(CmsAndOcspExampleData.cmsAndOcsp[includedSignature]);
        }

        SdoFromCmsesRequest sdoFromCmsRequest = new SdoFromCmsesRequest()
            .cmsAndOcsps(signatureList)
            .document(tbsDocument);

        TokenResponse tokenResponse = VARS.getAccessTokenFromKeycloak();

        // Call the signer endpoint
        String sdoB64 = VARS.b2bSigner.b2BSignApi()
                .withATAndDPoP(tokenResponse.access_token, VARS.b2bSigner.sdoFromCmsUri())
            .sdoFromCmses(sdoFromCmsRequest)
            .getSdo();

        dumpSdo(sdoB64);

    }

    private static void dumpSdo(String sdoB64) {
        // Dump the SDO as base64 and as pretty printed xml
        System.out.println("\nsdo as base64:n\n" + sdoB64);

        System.out.println("\nAnd as xml: \n" + PrettyPrint.prettyPrintXml(
            new String(Base64.getDecoder().decode(sdoB64), StandardCharsets.UTF_8)));
    }

    public static String sha256(String someString) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(someString.getBytes(StandardCharsets.ISO_8859_1));
            return Base64.getEncoder().encodeToString(hash);
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

}
