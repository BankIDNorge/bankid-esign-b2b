package no.bankid.esign.merchant.b2b.feignclients;

import feign.Feign;
import feign.jackson.JacksonDecoder;
import feign.jackson.JacksonEncoder;
import no.bankid.esign.feign.api.b2b.v0.api.B2bSignApi;
import no.bankid.esign.merchant.b2b.dpop.DPoPGenerator;

import java.net.URI;

public class B2BSigner {

    final FeignClientWithDPoPProofAndAccessToken<B2bSignApi> b2bSignApi;

    public FeignClientWithDPoPProofAndAccessToken<B2bSignApi> b2BSignApi() {
        return b2bSignApi;
    }

    public URI sdoFromTextUri() {
        return sdoFromTextUri;
    }

    public URI sdoFromCmsUri() {
        return sdoFromCmsUri;
    }
    public URI sdosFromDocsUri() {
        return sdosFromDocsUri;
    }

    final String b2bSignerRoot;
    final URI sdoFromTextUri;
    final URI sdoFromCmsUri;
    final URI sdosFromDocsUri;

    public B2BSigner(String b2bSignerRootUrl, DPoPGenerator dPoP) {
        this.b2bSignerRoot = b2bSignerRootUrl;
        this.sdoFromTextUri = URI.create(b2bSignerRoot + "/v0/b2b/sdo_from_text");
        this.sdoFromCmsUri = URI.create(b2bSignerRoot + "/v0/b2b/sdo_from_cmses");
        this.sdosFromDocsUri = URI.create(b2bSignerRoot + "/v0/b2b/sdos_from_docs");
        this.b2bSignApi = new FeignClientWithDPoPProofAndAccessToken<>(dPoP, Feign.builder()
            .client(new InterceptingFeignClient("B2BSigner"))
            .encoder(new JacksonEncoder())
            .decoder(new JacksonDecoder())
            .target(B2bSignApi.class, b2bSignerRoot));
    }

}
