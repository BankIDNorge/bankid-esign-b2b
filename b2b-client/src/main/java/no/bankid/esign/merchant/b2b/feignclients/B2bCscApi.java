package no.bankid.esign.merchant.b2b.feignclients;

import no.bankid.esign.feign.api.b2b.csc.api.CscCredentialsApi;
import no.bankid.esign.feign.api.b2b.csc.api.CscInfoApi;
import no.bankid.esign.feign.api.b2b.csc.api.CscSignaturesApi;

public interface B2bCscApi extends CscInfoApi, CscCredentialsApi, CscSignaturesApi {
}
