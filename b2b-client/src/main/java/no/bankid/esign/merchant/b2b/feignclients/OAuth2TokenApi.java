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
     * Sends the grant_type and scope as form parameters, authorizes using Basic Auth, that is
     * client_id,client_secret.
     *
     * @param grantType should always be "client_credentials"
     */

    @RequestLine("POST")
    @Headers("Content-Type: application/x-www-form-urlencoded")
    TokenResponse getTokenBasic(
            @Param("grant_type") String grantType,
            @Param("scope") String scope);

    /**
     * Sends the client_assertion_type, client_assertion and scope as form parameters.
     * <p>
     * See <a
     * href="https://openid.net/specs/openid-connect-core-1_0.html#ClientAuthentication">OpenID
     * spec</a> for details.
     *
     * @param clientAssertionType should always be
     * "urn:ietf:params:oauth:client-assertion-type:jwt-bearer"
     * @param clientAssertion should be a JWT signed with the private key of the client
     */
    @RequestLine("POST")
    @Headers("Content-Type: application/x-www-form-urlencoded")
    TokenResponse getTokenPrivateKeyJwt(
            @Param("client_assertion_type") String clientAssertionType,
            @Param("client_assertion") String clientAssertion,
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
