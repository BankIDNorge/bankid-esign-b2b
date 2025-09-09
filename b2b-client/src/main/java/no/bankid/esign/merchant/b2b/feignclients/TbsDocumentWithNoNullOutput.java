package no.bankid.esign.merchant.b2b.feignclients;

import com.fasterxml.jackson.annotation.JsonInclude;
import no.bankid.esign.feign.api.b2b.v0.model.BidXml;
import no.bankid.esign.feign.api.b2b.v0.model.TbsDocument;
import org.jetbrains.annotations.NotNull;

@JsonInclude(JsonInclude.Include.NON_NULL)
public abstract class TbsDocumentWithNoNullOutput extends TbsDocument {
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
