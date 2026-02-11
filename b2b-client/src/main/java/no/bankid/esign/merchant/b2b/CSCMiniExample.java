package no.bankid.esign.merchant.b2b;

import eu.europa.esig.dss.enumerations.DigestAlgorithm;
import eu.europa.esig.dss.enumerations.EncryptionAlgorithm;
import eu.europa.esig.dss.enumerations.SignatureAlgorithm;
import no.bankid.esign.feign.api.b2b.csc.model.CscCredentialsList200Response;
import no.bankid.esign.feign.api.b2b.csc.model.CscInfoRequest;
import org.bouncycastle.jce.provider.BouncyCastleProvider;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.cert.X509Certificate;
import java.security.spec.MGF1ParameterSpec;
import java.security.spec.PSSParameterSpec;
import java.util.Base64;
import java.util.List;

import static java.security.cert.CertificateFactory.getInstance;
import static no.bankid.esign.merchant.b2b.environment.Globals.VARS;

public class CSCMiniExample {

    static {
        // Needs bouncycastle to use NoneWithRSAandMGF1
        BouncyCastleProvider provider = new BouncyCastleProvider();
        Security.addProvider(provider);
    }

    public static void main(String[] args) throws Exception {
        var test = new CSCMiniExample();
        VARS.showPublicKeyJwk();

        test.doSomeCscInfo();
        test.doSomeCscSignHash();
    }

    /**
     * Example of how to call the CSC signHash endpoint.
     * Also validates the signature using the public key from the credentials list endpoint.
     * <p>
     * REMARK that the CSC signHash endpoint returns a signature in PKCS#1 v1.5 format.
     * What is actually happening on the serverside when signing is only an encryption, i.e. actual  signature
     * algorithm used is noneWithRSA or noneWithRSASSA-PSS, depending on the signature algorithm you choose to sign with.
     * <p>
     * Therefore in order to validate the signature, we may either use
     * hash and noneWith...
     * or we use the original data and RSASSA-PSS with the same parameters as used by the server.
     * <p>
     * Parameters
     */
    private void doSomeCscSignHash() {
        String stringToHash = "Tristesser";
        String hash1 = sha512(stringToHash);
        CSCAdapter adapter = CSCAdapter.createAdapter();

        SignatureAlgorithm signatureAlgorithm = SignatureAlgorithm.getAlgorithm(EncryptionAlgorithm.RSASSA_PSS, DigestAlgorithm.SHA512);

        var ocspAndSignature = adapter.computeSignatures(signatureAlgorithm, List.of(hash1));

        String signature = ocspAndSignature.signedHashesB64().getFirst();
        System.out.println("Signature for hash1: " + signature);
        CscCredentialsList200Response credentialsList200Response = adapter.getCredentialsList200Response();
        var signerCert = getSignerCertificateFromCredentialsList(credentialsList200Response);

        // Show how to validate signature using both the original data and the hash, to show the difference in parameters for the signature instance.
        PublicKey signerPublicKey = signerCert.getPublicKey();
        validatePSSSHA512Signature(stringToHash.getBytes(StandardCharsets.UTF_8), signature, signerPublicKey, DataToSignType.ORIGINAL_DATA_BYTES);
        validatePSSSHA512Signature(Base64.getDecoder().decode(hash1), signature, signerPublicKey, DataToSignType.HASH_BYTES);

        System.out.println("Signer:" + signerCert.getSubjectX500Principal().getName("RFC1779"));
        System.out.println("Signer returned from credentials list endpoint: " + credentialsList200Response.getCredentialInfos().getFirst().getCert().getSubjectDN());
        // Code and ideas for validating the ocspResponse may be found in https://github.com/BankIDNorge/bankid-open-b2b-examples repository.
    }

    private void validatePSSSHA512Signature(byte[] tbs, String pkcs1SignatureB64, PublicKey publicKey, DataToSignType dataToSignType) {
        try {
            Signature sig = Signature.getInstance(dataToSignType == DataToSignType.ORIGINAL_DATA_BYTES ? "RSASSA-PSS" : "NoneWithRSAandMGF1");
            // Default parameters for RSASSA-PSS with SHA-512,
            // for SHA-384 use MGF1ParameterSpec.SHA384 and salt length 48,
            // for SHA-256 use MGF1ParameterSpec.SHA256 and salt length 32
            sig.setParameter(new PSSParameterSpec("SHA-512", "MGF1", MGF1ParameterSpec.SHA512, 64, 1));
            sig.initVerify(publicKey);
            sig.update(tbs);
            boolean valid = sig.verify(Base64.getDecoder().decode(pkcs1SignatureB64));
            System.out.println("Signature valid: " + valid + " for data type: " + dataToSignType);
        } catch (NoSuchAlgorithmException | InvalidAlgorithmParameterException | SignatureException |
                 InvalidKeyException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Example of how to call the CSC info endpoint, which returns information about the credentials available for signing, supported algorithms etc.
     */
    private void doSomeCscInfo() {
        CscInfoRequest infoRequest = new CscInfoRequest().lang("nn");
        var infoPost = VARS.b2bSigner.b2BCscApi().theApi().cscInfo(infoRequest);
        System.out.println("CSC Info endpoint response using post request: " + infoPost);
    }

    /**
     * Helper method to compute the sha256 hash of a string, and return it in base64 format.
     */
    public static String sha512(String someString) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-512");
            byte[] hash = digest.digest(someString.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hash);
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    /**
     * Helper to extract the public key from the first certificate in credentialsListResponse.
     */
    private X509Certificate getSignerCertificateFromCredentialsList(CscCredentialsList200Response credentialsListResponse) {
        try {
            var firstCredential = credentialsListResponse.getCredentialInfos().getFirst();
            var certObj = firstCredential.getCert();
            var certChain = certObj.getCertificates(); // List<String> Base64-encoded certs, one leaf, zero or more intermediates, one root
            String leafCertBase64 = certChain.getFirst();
            byte[] certBytes = Base64.getDecoder().decode(leafCertBase64);
            var certificateFactory = getInstance("X.509");
            return (X509Certificate) certificateFactory.generateCertificate(new ByteArrayInputStream(certBytes));
        } catch (Exception e) {
            throw new RuntimeException("Failed to extract certificate", e);
        }
    }

    private enum DataToSignType {
        HASH_BYTES,
        ORIGINAL_DATA_BYTES
    }
}
