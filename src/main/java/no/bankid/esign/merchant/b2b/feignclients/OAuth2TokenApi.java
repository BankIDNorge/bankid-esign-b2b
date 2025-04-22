package no.bankid.esign.merchant.b2b.feignclients;

import feign.Feign;
import feign.Headers;
import feign.Param;
import feign.RequestLine;
import feign.form.FormEncoder;
import feign.jackson.JacksonDecoder;
import no.bankid.esign.merchant.b2b.dpop.DPoPGenerator;

public interface OAuth2TokenApi {

    /**
     * The client secret is allowed to be in the body,
     * client_secret_basic demands the client_secret in the header.
     * client_secret_post demands the client_secret in the body.
     * Keycloak seems to support both (automatically).
     * @param grantType
     * @param scope
     * @return
     */
    @RequestLine("POST")
    @Headers("Content-Type: application/x-www-form-urlencoded")
    TokenResponse getToken(
        @Param("grant_type") String grantType,
        @Param("scope") String scope);

    class TokenResponse {
        public String access_token;
        public String token_type;
        public String refresh_token;
        public int expires_in;
        public String scope;
        // Add other fields as needed
    }

    static FeignClientWithDPoPProofAndAccessToken<OAuth2TokenApi> create(String tokenEndpointUrl,
        DPoPGenerator dPoP) {
        OAuth2TokenApi oAuth2TokenApi = Feign.builder()
            .client(new InterceptingFeignClient("OAuth2TokenApi"))
            .decoder(new JacksonDecoder())
            .encoder(new FormEncoder())
            .target(OAuth2TokenApi.class, tokenEndpointUrl);
        return new FeignClientWithDPoPProofAndAccessToken<>(dPoP, oAuth2TokenApi);
    }
}
