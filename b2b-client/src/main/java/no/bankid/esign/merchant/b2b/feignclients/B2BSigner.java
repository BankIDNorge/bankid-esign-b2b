package no.bankid.esign.merchant.b2b.feignclients;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import feign.Feign;
import feign.jackson.JacksonDecoder;
import feign.jackson.JacksonEncoder;
import no.bankid.esign.feign.api.b2b.v0.api.B2bSignApi;
import no.bankid.esign.feign.api.b2b.v0.model.BidXml;
import no.bankid.esign.feign.api.b2b.v0.model.MimeType;
import no.bankid.esign.feign.api.b2b.v0.model.SdoFromCmsesRequest;
import no.bankid.esign.feign.api.b2b.v0.model.TbsDocument;
import no.bankid.esign.merchant.b2b.dpop.DPoPGenerator;
import org.jetbrains.annotations.NotNull;

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

    public URI cmsesFromHashesUri() {
        return cmsesFromHashesUri;
    }

    public URI padesCmsesFromHashesUri() {
        return padesCmsesFromHashesUri;
    }


    final String b2bSignerRoot;
    final URI sdoFromTextUri;
    final URI sdoFromCmsUri;
    final URI sdosFromDocsUri;
    final URI cmsesFromHashesUri;
    final URI padesCmsesFromHashesUri;


    public B2BSigner(String b2bSignerRootUrl, DPoPGenerator dPoP) {
        this.b2bSignerRoot = b2bSignerRootUrl;
        this.sdoFromTextUri = URI.create(b2bSignerRoot + "/v0/b2b/sdo_from_text");
        this.sdoFromCmsUri = URI.create(b2bSignerRoot + "/v0/b2b/sdo_from_cmses");
        this.sdosFromDocsUri = URI.create(b2bSignerRoot + "/v0/b2b/sdos_from_docs");
        this.cmsesFromHashesUri = URI.create(b2bSignerRoot + "/v0/b2b/cmses_from_hashes");
        this.padesCmsesFromHashesUri = URI.create(b2bSignerRoot + "/v0/b2b/pades/cmses_from_hashes");

        this.b2bSignApi = new FeignClientWithDPoPProofAndAccessToken<>(dPoP, Feign.builder()
            .client(new InterceptingFeignClient("B2BSigner"))
                .encoder(new JacksonEncoder(feignObjectMapper()))
            .decoder(new JacksonDecoder())
            .target(B2bSignApi.class, b2bSignerRoot));
    }

    /**
     * Openapi generation using fein library sets ALWAYS on every property
     * This method returns an objectmapper skipping generation of null values when serializing to json
     */
    private ObjectMapper feignObjectMapper() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        objectMapper.addMixIn(TbsDocument.class, TbsDocumentWithNoNullOutput.class);
        objectMapper.addMixIn(SdoFromCmsesRequest.class, SdoFromCmsesRequestNullOutput.class);

        return objectMapper;

    }

    /**
     * Helper class for objectmapper overriding serialization set in original class.
     * Override those methods that are marked with Include.ALWAYS in super if null-generation shall be skipped
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public abstract static class TbsDocumentWithNoNullOutput extends TbsDocument {
        @NotNull
        @Override
        @JsonInclude(JsonInclude.Include.NON_NULL)
        public String getTextToSign() {
            return super.getTextToSign();
        }

        @NotNull
        @Override
        @JsonInclude(JsonInclude.Include.NON_NULL)
        public String getDescription() {
            return super.getDescription();
        }

        @NotNull
        @Override
        @JsonInclude(JsonInclude.Include.NON_NULL)
        public String getPdf() {
            return super.getPdf();
        }

        @NotNull
        @Override
        @JsonInclude(JsonInclude.Include.NON_NULL)
        public BidXml getBidxml() {
            return super.getBidxml();
        }
    }

    /**
     * Helper class for objectmapper overriding serialization set in original class.
     * Override those methods that are marked with Include.ALWAYS in super if null-generation shall be skipped
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class SdoFromCmsesRequestNullOutput extends SdoFromCmsesRequest {

        @NotNull
        @Override
        @JsonInclude(JsonInclude.Include.NON_NULL)
        public TbsDocument getDocument() {
            return super.getDocument();
        }

        @NotNull
        @Override
        @JsonInclude(JsonInclude.Include.NON_NULL)
        public MimeType getMimeType() {
            return super.getMimeType();
        }

        @NotNull
        @Override
        @JsonInclude(JsonInclude.Include.NON_NULL)
        public String getDescription() {
            return super.getDescription();
        }
    }
}
