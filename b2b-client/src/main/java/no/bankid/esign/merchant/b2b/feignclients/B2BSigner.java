package no.bankid.esign.merchant.b2b.feignclients;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import feign.Feign;
import feign.jackson.JacksonDecoder;
import feign.jackson.JacksonEncoder;
import no.bankid.esign.feign.api.b2b.csc.api.CscCredentialsApi;
import no.bankid.esign.feign.api.b2b.csc.api.CscInfoApi;
import no.bankid.esign.feign.api.b2b.csc.api.CscSignaturesApi;
import no.bankid.esign.feign.api.b2b.csc.model.*;
import no.bankid.esign.feign.api.b2b.v0.api.B2bAdEsApi;
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
    final FeignClientWithDPoPProofAndAccessToken<B2bAdEsApi> b2bAdesApi;
    final FeignClientWithDPoPProofAndAccessToken<B2bCscApi> b2bCscApi;


    public FeignClientWithDPoPProofAndAccessToken<B2bSignApi> b2BSignApi() {
        return b2bSignApi;
    }

    public FeignClientWithDPoPProofAndAccessToken<B2bAdEsApi> b2bAdesApi() {
        return b2bAdesApi;
    }

    public FeignClientWithDPoPProofAndAccessToken<B2bCscApi> b2BCscApi() {
        return b2bCscApi;
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

    public URI padesSignUri() {
        return padesSignUri;
    }

    public URI xadesSignUri() {
        return xadesSignUri;
    }

    public URI cscInfoUri() {
        return cscInfoUri;
    }

    public URI cscCredentialsInfoUri() {
        return cscCredentialsInfoUri;
    }

    public URI cscCredentialsListUri() {
        return cscCredentialsListUri;
    }

    public URI cscSignHashUri() {
        return cscSignHashUri;
    }


    final String b2bSignerRoot;
    final URI sdoFromTextUri;
    final URI sdoFromCmsUri;
    final URI sdosFromDocsUri;
    final URI cmsesFromHashesUri;
    final URI padesCmsesFromHashesUri;

    final URI padesSignUri;
    final URI xadesSignUri;

    final URI cscInfoUri;
    final URI cscCredentialsInfoUri;
    final URI cscCredentialsListUri;
    final URI cscSignHashUri;

    public B2BSigner(String b2bSignerRootUrl, DPoPGenerator dPoP) {
        this.b2bSignerRoot = b2bSignerRootUrl;
        this.sdoFromTextUri = URI.create(b2bSignerRoot + "/v0/b2b/sdo_from_text");
        this.sdoFromCmsUri = URI.create(b2bSignerRoot + "/v0/b2b/sdo_from_cmses");
        this.sdosFromDocsUri = URI.create(b2bSignerRoot + "/v0/b2b/sdos_from_docs");
        this.cmsesFromHashesUri = URI.create(b2bSignerRoot + "/v0/b2b/cmses_from_hashes");
        this.padesCmsesFromHashesUri = URI.create(b2bSignerRoot + "/v0/b2b/pades/cmses_from_hashes");

        this.padesSignUri = URI.create(b2bSignerRoot + "/v0/b2b/ades/pades");
        this.xadesSignUri = URI.create(b2bSignerRoot + "/v0/b2b/ades/xades");

        this.cscCredentialsInfoUri = URI.create(b2bSignerRoot + "/v0/b2b/csc/v2/credentials/info");
        this.cscCredentialsListUri = URI.create(b2bSignerRoot + "/v0/b2b/csc/v2/credentials/list");
        this.cscInfoUri = URI.create(b2bSignerRoot + "/v0/b2b/csc/v2/info");
        this.cscSignHashUri = URI.create(b2bSignerRoot + "/v0/b2b/csc/v2/signatures/signHash");

        this.b2bSignApi = new FeignClientWithDPoPProofAndAccessToken<>(dPoP, Feign.builder()
            .client(new InterceptingFeignClient("B2BSigner"))
                .encoder(new JacksonEncoder(feignObjectMapper()))
            .decoder(new JacksonDecoder())
            .target(B2bSignApi.class, b2bSignerRoot));

        this.b2bAdesApi = new FeignClientWithDPoPProofAndAccessToken<>(dPoP, Feign.builder()
                .client(new InterceptingFeignClient("B2BAdes"))
                .encoder(new JacksonEncoder(feignObjectMapper()))
                .decoder(new JacksonDecoder())
                .target(B2bAdEsApi.class, b2bSignerRoot));

        FeignClientWithDPoPProofAndAccessToken<CscInfoApi> cscInfoApi = new FeignClientWithDPoPProofAndAccessToken<>(dPoP, Feign.builder()
                .client(new InterceptingFeignClient("CscInfoApi"))
                .encoder(new JacksonEncoder(feignObjectMapper()))
                .decoder(new JacksonDecoder())
                .target(CscInfoApi.class, b2bSignerRoot));
        FeignClientWithDPoPProofAndAccessToken<CscCredentialsApi> cscCredentialsApi = new FeignClientWithDPoPProofAndAccessToken<>(dPoP, Feign.builder()
                .client(new InterceptingFeignClient("CscCredentialsApi"))
                .encoder(new JacksonEncoder(feignObjectMapper()))
                .decoder(new JacksonDecoder())
                .target(CscCredentialsApi.class, b2bSignerRoot));
        FeignClientWithDPoPProofAndAccessToken<CscSignaturesApi> cscSignaturesApi = new FeignClientWithDPoPProofAndAccessToken<>(dPoP, Feign.builder()
                .client(new InterceptingFeignClient("CscSignaturesApi"))
                .encoder(new JacksonEncoder(feignObjectMapper()))
                .decoder(new JacksonDecoder())
                .target(CscSignaturesApi.class, b2bSignerRoot));

        this.b2bCscApi = new FeignClientWithDPoPProofAndAccessToken<>(dPoP,
                new B2bCscApi() {
                    @Override
                    public CscCredentialsList200Response cscCredentialsList(CscCredentialsListRequest credentialsListRequest) {
                        return cscCredentialsApi.theApi().cscCredentialsList(credentialsListRequest);
                    }

                    @Override
                    public ApiResponse<CscCredentialsList200Response> cscCredentialsListWithHttpInfo(CscCredentialsListRequest credentialsListRequest) {
                        return cscCredentialsApi.theApi().cscCredentialsListWithHttpInfo(credentialsListRequest);
                    }

                    @Override
                    public CscInfoGet200Response cscInfo(CscInfoRequest infoRequest) {
                        return cscInfoApi.theApi().cscInfo(infoRequest);
                    }

                    @Override
                    public ApiResponse<CscInfoGet200Response> cscInfoWithHttpInfo(CscInfoRequest infoRequest) {
                        return cscInfoApi.theApi().cscInfoWithHttpInfo(infoRequest);
                    }

                    @Override
                    public CscInfoGet200Response cscInfoGet(String lang) {
                        return cscInfoApi.theApi().cscInfoGet(lang);
                    }

                    @Override
                    public ApiResponse<CscInfoGet200Response> cscInfoGetWithHttpInfo(String lang) {
                        return cscInfoApi.theApi().cscInfoGetWithHttpInfo(lang);
                    }

                    @Override
                    public CscInfoGet200Response cscInfoGet(CscInfoGetQueryParams queryParams) {
                        return cscInfoApi.theApi().cscInfoGet(queryParams);
                    }

                    @Override
                    public ApiResponse<CscInfoGet200Response> cscInfoGetWithHttpInfo(CscInfoGetQueryParams queryParams) {
                        return cscInfoApi.theApi().cscInfoGetWithHttpInfo(queryParams);
                    }

                    @Override
                    public CscSignaturesSignHash200Response cscSignaturesSignHash(CscSignaturesSignHashRequest signaturesSignHashRequest) {
                        return cscSignaturesApi.theApi().cscSignaturesSignHash(signaturesSignHashRequest);
                    }

                    @Override
                    public ApiResponse<CscSignaturesSignHash200Response> cscSignaturesSignHashWithHttpInfo(CscSignaturesSignHashRequest signaturesSignHashRequest) {
                        return cscSignaturesApi.theApi().cscSignaturesSignHashWithHttpInfo(signaturesSignHashRequest);
                    }
                });
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
