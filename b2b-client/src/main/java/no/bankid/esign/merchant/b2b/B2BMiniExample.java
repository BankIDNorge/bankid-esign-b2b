package no.bankid.esign.merchant.b2b;

import no.bankid.esign.feign.api.b2b.v0.model.*;
import no.bankid.esign.merchant.b2b.dpop.DPoPGenerator;
import no.bankid.esign.merchant.b2b.dpop.NullDPopGenerator;
import no.bankid.esign.merchant.b2b.dpop.RS256DPopGenerator;
import no.bankid.esign.merchant.b2b.environment.BankIDOIDCServers;
import no.bankid.esign.merchant.b2b.environment.BankIDOIDCServers.OIDCServerSpec;
import no.bankid.esign.merchant.b2b.feignclients.*;
import no.bankid.esign.merchant.b2b.feignclients.OAuth2TokenApi.TokenResponse;
import no.bankid.esign.merchant.b2b.feignclients.OpenIDWellKnownApi.OpenIDConfig;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;

public class B2BMiniExample {

    public static void main(String[] args) throws Exception {
        B2BMiniExample test = new B2BMiniExample();
        InterceptingFeignClient.traceTokens = true;
        test.doASimpleTextSigning();
        test.doBuildSdoFromCms(0,2,1,3,2,2,2);
        test.doBuildSdoFromCms(0,2,4,1,3,5);
        test.doSomePdfSigning("small-ex.pdf", "csc-pades-demo.pdf");
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
        TokenResponse tokenResponse = getAccessTokenFromKeycloak();

        // Call the signer endpoint
        String sdoB64 = b2bSigner.b2BSignApi()
            .withATAndDPoP(tokenResponse.access_token, b2bSigner.sdoFromTextUri())
            .sdoFromText(sdoFromTextRequest)
            .getSdo();

        // Dump the SDO as base64 and as pretty printed xml
        dumpSdo(sdoB64);
    }

    private void doSomePdfSigning(String... fns) throws Exception {
        // Here is what we will sign:
        SdosFromDocumentsRequest sdosFromDocumentsRequest = new SdosFromDocumentsRequest();

        for (int i = 0; i < fns.length; i++) {
            String fn = fns[i];
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

        TokenResponse tokenResponse = getAccessTokenFromKeycloak();
        // Call the signer endpoint
        b2bSigner
            .b2BSignApi()
            .withATAndDPoP(tokenResponse.access_token, b2bSigner.sdosFromDocsUri())
            .sdosFromDocuments(sdosFromDocumentsRequest)
            .forEach(hashAndSdo -> dumpSdo(hashAndSdo.getSdo()));
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
        for (int i = 0; i < includedSignatures.length; i++) {
            signatureList.add(CmsAndOcspExampleData.cmsAndOcsp[includedSignatures[i]]);
        }

        SdoFromCmsesRequest sdoFromCmsRequest = new SdoFromCmsesRequest()
            .cmsAndOcsps(signatureList)
            .document(tbsDocument);

        TokenResponse tokenResponse = getAccessTokenFromKeycloak();

        // Call the signer endpoint
        String sdoB64 = b2bSigner.b2BSignApi()
            .withATAndDPoP(tokenResponse.access_token, b2bSigner.sdoFromCmsUri())
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

    /**
     * Get an access token from the BankID OIDC server.
     * For this example Basic Authentication is sufficient for the token endpoint.
     * <p>
     * For production, private_key_jwt authentication method will be required.
     */
    private TokenResponse getAccessTokenFromKeycloak() {
        return oAuth2TokenApi
            .withBasicAndDPoP(
                bankIDOIDCServer.clientId(),
                bankIDOIDCServer.clientSecret(),
                URI.create(openIDConfig.token_endpoint))
            .getToken(
            "client_credentials",
            "esign/b2b"
        );
    }

    // TODO: when changing bankIDOIDCServer, remember to change urls in other components such as MBE AND Composite IDP
    private final OIDCServerSpec bankIDOIDCServer = BankIDOIDCServers.bankIDOidcServer;

    final OpenIDConfig openIDConfig;
    final FeignClientWithDPoPProofAndAccessToken<OAuth2TokenApi> oAuth2TokenApi;
    final boolean canUseDPoP;
    final B2BSigner b2bSigner;

    /**
     * Initialize the urls and clients needed for the test
     */
    public B2BMiniExample() {
        // Get config from BankID OIDC well known endpoint
        this.openIDConfig = OpenIDWellKnownApi.create(bankIDOIDCServer.oidcRoot()).getOpenIDConfig();

        this.canUseDPoP = // requests gives "DPoP proof is missing" if supported and missing
            openIDConfig.dpop_signing_alg_values_supported != null &&
                Arrays.asList(openIDConfig.dpop_signing_alg_values_supported).contains("RS256");
        DPoPGenerator dPoPGenerator = canUseDPoP ? new RS256DPopGenerator() : new NullDPopGenerator();

        this.oAuth2TokenApi = OAuth2TokenApi.create(openIDConfig.token_endpoint, dPoPGenerator);


        this.b2bSigner = new B2BSigner(bankIDOIDCServer.b2bSignerRootUrl(), dPoPGenerator);
    }
}
