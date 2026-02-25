package no.bankid.esign.merchant.b2b.feignclients;

import com.nimbusds.oauth2.sdk.token.DPoPAccessToken;
import no.bankid.esign.merchant.b2b.dpop.DPoPGenerator;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static no.bankid.esign.merchant.b2b.feignclients.InterceptingFeignClient.injectAccessOrBasicAndDPoPProof;

public class FeignClientWithDPoPProofAndAccessToken<ClientType> {

    private final ClientType api;
    private final DPoPGenerator dPoP;

    public FeignClientWithDPoPProofAndAccessToken(DPoPGenerator dPoP, ClientType api) {
        this.api = api;
        this.dPoP = dPoP;
    }

    /**
     * Injects Basic auth header and DPoP proof header.
     */
    public ClientType withBasicAndDPoP(String clientId, String clientSecret, URI endpoint) {
        return withBasicOrAT_DPoPAndMethod(basicAuth(clientId,clientSecret), null, endpoint, "POST");
    }

    /**
     * Injects Access token header and DPoP proof header
     */
    public ClientType withATAndDPoP(String accessToken, URI endpoint) {
        return withBasicOrAT_DPoPAndMethod(null, accessToken, endpoint, "POST");
    }

    /**
     * Injects only DPoP proof header, used for private_key_jwt where only DPoP in header, other authentication in body
     */
    public ClientType withDPoP(URI endpoint) {
        return withBasicOrAT_DPoPAndMethod(null, null, endpoint, "POST");
    }

    public ClientType withBasicOrAT_DPoPAndMethod(String basicAuth, String accessToken, URI endpoint, String method) {
        injectAccessOrBasicAndDPoPProof(
            basicAuth, accessToken,
            dPoP.generate(method,
                endpoint,
                accessToken == null ? null : new DPoPAccessToken(accessToken)));

        return api;
    }

    private static String basicAuth(String clientId, String clientSecret) {
        return Base64.getEncoder().encodeToString(
            (clientId + ":" + clientSecret).getBytes(StandardCharsets.UTF_8));
    }

    public ClientType theApi() {
        return api;
    }
}
