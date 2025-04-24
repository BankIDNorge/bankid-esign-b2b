package no.bankid.esign.merchant.b2b.feignclients;

import feign.Feign;
import feign.RequestLine;
import feign.jackson.JacksonDecoder;
import feign.jackson.JacksonEncoder;

public interface OpenIDWellKnownApi {

    @RequestLine("GET /.well-known/openid-configuration")
    OpenIDConfig getOpenIDConfig();

    class OpenIDConfig {

        public String issuer;
        public String authorization_endpoint;
        public String token_endpoint;
        public String jwks_uri;
        public String userinfo_endpoint;
        public String[] dpop_signing_alg_values_supported;

        // Add other fields as needed
    }

    static OpenIDWellKnownApi create(String rootUrl) {
        return Feign.builder()
            .client(new InterceptingFeignClient("OpenIDWellKnownApi"))
            .encoder(new JacksonEncoder())
            .decoder(new JacksonDecoder())
            .target(OpenIDWellKnownApi.class, rootUrl);
    }
}
