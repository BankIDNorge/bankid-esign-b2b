package no.bankid.esign.merchant.b2b;

import eu.europa.esig.dss.enumerations.SignatureAlgorithm;
import no.bankid.esign.feign.api.b2b.csc.model.CscCredentialsList200Response;
import no.bankid.esign.feign.api.b2b.csc.model.CscCredentialsListRequest;
import no.bankid.esign.feign.api.b2b.csc.model.CscSignaturesSignHashRequest;
import no.bankid.esign.merchant.b2b.feignclients.OAuth2TokenApi;
import org.bouncycastle.asn1.ocsp.BasicOCSPResponse;
import org.bouncycastle.cert.ocsp.BasicOCSPResp;
import org.bouncycastle.cert.ocsp.OCSPException;
import org.bouncycastle.cert.ocsp.OCSPRespBuilder;

import java.io.IOException;
import java.util.Base64;
import java.util.List;

import static no.bankid.esign.merchant.b2b.environment.Globals.VARS;

/**
 * A simple class holding the csc credentials list result and which uses credentialId and certs from that when signing.
 * <p>
 * When calling computeSignatures, the implementation shall ensure that the credentials list has been loaded,
 * and then use the credential ID from that to call the signHash endpoint.
 * <p>
 * The implementation should also administrate the access token, see {@link CSCAdapter::createAdapter()}
 */
public interface CSCAdapter {
    CscCredentialsList200Response getCredentialsList200Response();

    OCSPAndSignedHashes computeSignatures(SignatureAlgorithm signatureAlgorithm, List<String> b64Hashes);

    /**
     * Holder of the OCSP response and the signed hashes, as returned by the CSC signHash endpoint,
     * with the addition of the full OCSP response (including the basic OCSP response and the signature) in base64 format.
     *
     * @param basicOcspB64
     * @param signedHashesB64
     */
    record OCSPAndSignedHashes(String basicOcspB64, List<String> signedHashesB64) {
        String fullOcspB64() {
            byte[] fullOcspBytesFromBasicBytes = getFullOcspBytesFromBasicBytes(Base64.getDecoder().decode(basicOcspB64));
            return Base64.getEncoder().encodeToString(fullOcspBytesFromBasicBytes);
        }
    }

    /**
     * Adding response succesful to a basic OCSP response, to create a full OCSP response as expected by some validators.
     */
    static byte[] getFullOcspBytesFromBasicBytes(byte[] basicOcspBytes) {
        BasicOCSPResponse basicResponse = BasicOCSPResponse.getInstance(basicOcspBytes);
        BasicOCSPResp basicOCSPResp = new BasicOCSPResp(basicResponse);
        byte[] ocspBytes;
        try {
            ocspBytes = new OCSPRespBuilder().build(OCSPRespBuilder.SUCCESSFUL, basicOCSPResp).getEncoded();
        } catch (OCSPException | IOException e) {
            throw new RuntimeException(e);
        }
        return ocspBytes;
    }

    /**
     * Create a CSCAdapter which initially creates an access token.
     * This access token might of course expire, you should handle that case.
     * Usage in this example code is to create a new adapter for each operation,
     * so token expiry is not supposed to be an issue.
     */
    static CSCAdapter createAdapter() {
        return new CSCAdapter() {
            final OAuth2TokenApi.TokenResponse tokenResponse = VARS.getAccessTokenFromKeycloak();

            CscCredentialsList200Response credentialsList200Response = null;

            @Override
            public CscCredentialsList200Response getCredentialsList200Response() {
                if (credentialsList200Response != null) {
                    return credentialsList200Response;
                }

                // We ask for "everything" in the credentials list request, to get all the information we need in one call
                var credentialsListRequest = new CscCredentialsListRequest()
                        .certificates("chain")
                        .credentialInfo(true)
                        .certInfo(true);
                credentialsList200Response = VARS.b2bSigner
                        .b2BCscApi()
                        .withATAndDPoP(tokenResponse.access_token, VARS.b2bSigner.cscCredentialsListUri())
                        .cscCredentialsList(credentialsListRequest);

                System.out.println("CSC credentials list : " + credentialsList200Response);

                assert credentialsList200Response.getCredentialInfos() != null;
                return credentialsList200Response;
            }

            @Override
            public OCSPAndSignedHashes computeSignatures(SignatureAlgorithm signatureAlgorithm, List<String> b64Hashes) {
                ensureLoaded();
                // We need to get the credentials list to know which credential ID to use for signing
                // We take the first credential, and for BankID B2B CSC there should only be one credential.
                // The credential ID is needed for the signHash request

                // Remark: the credential ID does not have to be the same between calls, so doing a refresh
                // should at least be done after a restart or error. An update of the signer cert on server side causes
                // new credential IDs to be issued.

                String credentialID = credentialsList200Response.getCredentialInfos().getFirst().getCredentialID();
                var signaturesSignHashRequest = new CscSignaturesSignHashRequest()
                        .hashes(b64Hashes)
                        .credentialID(credentialID)
                        .signAlgo(CSCAlgorithms.getSafeOid(signatureAlgorithm))
                        .hashAlgorithmOID(CSCAlgorithms.getSafeOid(signatureAlgorithm.getDigestAlgorithm()));

                var signaturesSignHash200Response = VARS.b2bSigner
                        .b2BCscApi()
                        .withATAndDPoP(tokenResponse.access_token, VARS.b2bSigner.cscSignHashUri())
                        .cscSignaturesSignHash(signaturesSignHashRequest);

                System.out.println("CSC signHashes : " + signaturesSignHash200Response);

                return new OCSPAndSignedHashes(
                        signaturesSignHash200Response.getOcspResponse(),
                        signaturesSignHash200Response.getSignatures()
                );
            }

            private void ensureLoaded() {
                if (credentialsList200Response == null) {
                    getCredentialsList200Response();
                }
            }
        };
    }
}
