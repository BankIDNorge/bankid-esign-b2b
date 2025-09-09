package no.bankid.esign.merchant.b2b.feignclients;

import com.fasterxml.jackson.annotation.JsonInclude;
import no.bankid.esign.feign.api.b2b.v0.model.MimeType;
import no.bankid.esign.feign.api.b2b.v0.model.SdoFromCmsesRequest;
import org.jetbrains.annotations.NotNull;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class SdoFromCmsesRequestNullOutput extends SdoFromCmsesRequest {
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
