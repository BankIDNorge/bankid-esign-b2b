package no.bankid.esign.merchant.b2b.dpop;

import com.nimbusds.oauth2.sdk.token.AccessToken;

import java.net.URI;

public interface DPoPGenerator {

    String generate(String method, URI uri, AccessToken access);
}
