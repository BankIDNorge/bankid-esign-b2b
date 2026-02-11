package no.bankid.esign.merchant.b2b.environment;

import no.bankid.esign.merchant.b2b.PrettyPrint;
import no.bankid.esign.merchant.b2b.dpop.DPoPGenerator;
import no.bankid.esign.merchant.b2b.dpop.NullDPopGenerator;
import no.bankid.esign.merchant.b2b.dpop.RS256DPopGenerator;
import no.bankid.esign.merchant.b2b.feignclients.*;

import java.net.URI;
import java.util.Arrays;

import static no.bankid.esign.merchant.b2b.environment.BankIDOIDCServers.AuthType.PRIVATE_KEY_JWT;
import static no.bankid.esign.merchant.b2b.environment.BankIDOIDCServers.bankIDOidcServer;

public enum Globals {
    VARS;

    public final OpenIDWellKnownApi.OpenIDConfig openIDConfig;
    public final FeignClientWithDPoPProofAndAccessToken<OAuth2TokenApi> oAuth2TokenApi;
    public final boolean canUseDPoP;
    public final B2BSigner b2bSigner;

    Globals() {
        // Get config from BankID OIDC well known endpoint
        this.openIDConfig = OpenIDWellKnownApi.create(bankIDOidcServer.oidcRoot()).getOpenIDConfig();

        this.canUseDPoP = // requests gives "DPoP proof is missing" if supported and missing
                openIDConfig.dpop_signing_alg_values_supported != null &&
                        Arrays.asList(openIDConfig.dpop_signing_alg_values_supported).contains("RS256");
        DPoPGenerator dPoPGenerator = canUseDPoP ? new RS256DPopGenerator() : new NullDPopGenerator();

        this.oAuth2TokenApi = OAuth2TokenApi.create(openIDConfig.token_endpoint, dPoPGenerator);
        this.b2bSigner = new B2BSigner(bankIDOidcServer.b2bSignerRootUrl(), dPoPGenerator);

        InterceptingFeignClient.traceTokens = true;
    }

    public OAuth2TokenApi.TokenResponse getAccessTokenFromKeycloak() {
        if (bankIDOidcServer.authType() == PRIVATE_KEY_JWT) {
            return getAccessTokenFromKeycloakPrivateKeyJwt();
        } else {
            return getAccessTokenFromKeycloakBasicAuth();
        }
    }

    /**
     * Get an access token from the BankID OIDC server using basic authentication
     */
    private OAuth2TokenApi.TokenResponse getAccessTokenFromKeycloakBasicAuth() {
        return VARS.oAuth2TokenApi
                .withBasicAndDPoP(
                        bankIDOidcServer.clientId(),
                        bankIDOidcServer.clientSecret(),
                        URI.create(VARS.openIDConfig.token_endpoint))
                .getTokenBasic(
                        "client_credentials",
                        "esign/b2b"
                );
    }

    /**
     * Get an access token from the BankID OIDC server using private key authentication
     */
    private OAuth2TokenApi.TokenResponse getAccessTokenFromKeycloakPrivateKeyJwt() {
        // We must build a signed JWT to use as client_assertion
        // The signing is using the PrivateKeyAssertionBuilder built on the private key of the clientId
        // The public key is registered in the OIDC server

        return VARS.oAuth2TokenApi
                .withDPoP(URI.create(VARS.openIDConfig.token_endpoint))
                .getTokenPrivateKeyJwt(
                        "urn:ietf:params:oauth:client-assertion-type:jwt-bearer",
                        bankIDOidcServer.privateKeyAssertionBuilder()
                                .buildAssertion(bankIDOidcServer.clientId(), URI.create(VARS.openIDConfig.token_endpoint)),
                        "client_credentials",
                        "esign/b2b"
                );
    }

    /**
     * Shows the public key to register in BankID OIDC when client is using private_key_jwt authentication
     */
    public void showPublicKeyJwk() {
        if (bankIDOidcServer.authType() != PRIVATE_KEY_JWT) {
            System.out.println("Client " + bankIDOidcServer.clientId() + " does not use private_key_jwt authentication");
            return;
        }
        System.out.println("\nFor client " + bankIDOidcServer.clientId() +
                " using BankID OIDC at " + bankIDOidcServer.oidcRoot() +
                " register this key in BankID OIDC:\n" +
                PrettyPrint.prettyPrintJson(bankIDOidcServer.privateKeyAssertionBuilder().getPublicKeyAsJsonMap()) + "\n");
    }

}
