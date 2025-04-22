package no.bankid.esign.merchant.b2b.feignclients;

import static no.bankid.esign.merchant.b2b.feignclients.InterceptingFeignClient.injectAccessOrBasicAndDPoPProof;

import com.nimbusds.oauth2.sdk.token.DPoPAccessToken;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import no.bankid.esign.merchant.b2b.dpop.DPoPGenerator;

public class FeignClientWithDPoPProofAndAccessToken<ClientType> {

    private final ClientType api;
    private final DPoPGenerator dPoP;

    public FeignClientWithDPoPProofAndAccessToken(DPoPGenerator dPoP, ClientType api) {
        this.api = api;
        this.dPoP = dPoP;
    }

    public ClientType withBasicAndDPoP(String clientId, String clientSecret, URI endpoint) {
        return withBasicOrAT_DPoPAndMethod(basicAuth(clientId,clientSecret), null, endpoint, "POST");
    }

    // Default only POST methods supported
    public ClientType withATAndDPoP(String accessToken, URI endpoint) {
        return withBasicOrAT_DPoPAndMethod(null, accessToken, endpoint, "POST");
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
}
