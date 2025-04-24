package no.bankid.esign.merchant.b2b.dpop;

import com.nimbusds.oauth2.sdk.token.AccessToken;

import java.net.URI;

public class NullDPopGenerator implements DPoPGenerator {

    @Override
    public String generate(String method, URI uri, AccessToken access) {
        return null;
    }
}
