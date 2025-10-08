package no.bankid.esign.merchant.b2b.environment;

import static no.bankid.esign.merchant.b2b.environment.BankIDOIDCServers.AuthType.BASIC;
import static no.bankid.esign.merchant.b2b.environment.BankIDOIDCServers.AuthType.PRIVATE_KEY_JWT;

public class BankIDOIDCServers {
    public enum AuthType {BASIC, PRIVATE_KEY_JWT}

    private final static OIDCServerSpec bankIDOidcCurrentKeyAuthentication = serverWithPrivateKeyJwtSpec(
            "https://auth.current.bankid.no/auth/realms/current",
            "your-client-id",
            "path-to-your-keystore.jks",
            "your-keystore-password",
            "your-private-key-alias-in-keystore",
            "https://api.esign-stoedev.cloud"
    );
    private final static OIDCServerSpec bankIDOidcCurrentPwdAuthentication = serverWithBasicSpec(
            "https://auth.current.bankid.no/auth/realms/current",
            "your-client-id",
            "your-client-secret",
            "https://api.esign-stoedev.cloud"
    );


    public final static OIDCServerSpec bankIDOidcServer = bankIDOidcCurrentKeyAuthentication;

    /**
     * Specification of an OIDC server to use for authentication and token acquisition
     *
     * @param oidcRoot                   The root URL of the BankID OIDC server, e.g. https://auth.current.bankid.no/auth/realms/current
     * @param clientId                   The BankID OIDC client id to use when acquiring tokens
     * @param clientSecret               The client secret to use when acquiring tokens, null if using private_key_jwt
     * @param b2bSignerRootUrl           The root URL of the B2B signer service, e.g. https://api.esign-stoedev.cloud
     * @param authType                   The authentication type to use, either BASIC or PRIVATE_KEY_JWT
     * @param privateKeyAssertionBuilder The builder that creates the private_key_jwt token, null if using BASIC
     */
    public record OIDCServerSpec(
            String oidcRoot,
            String clientId,
            String clientSecret,
            String b2bSignerRootUrl,
            AuthType authType,
            PrivateKeyAssertionBuilder privateKeyAssertionBuilder) {
        public OIDCServerSpec {
        }
    }

    /**
     * Create an OIDC server spec using basic authentication
     */
    public static OIDCServerSpec serverWithBasicSpec(
            String oidcRoot,
            String clientId,
            String clientSecret,
            String b2bSignerRootUrl
    ) {
        return new OIDCServerSpec(oidcRoot, clientId, clientSecret, b2bSignerRootUrl, BASIC, null);
    }

    /**
     * Create an OIDC server spec using private_key_jwt authentication, same passphrase for keystore and key
     */
    public static OIDCServerSpec serverWithPrivateKeyJwtSpec(
            String oidcRoot,
            String clientId,
            String keystorePath,
            String keystorePassword,
            String keystoreAlias,
            String b2bSignerRootUrl
    ) {
        return new OIDCServerSpec(oidcRoot, clientId, null, b2bSignerRootUrl, PRIVATE_KEY_JWT,
                new PrivateKeyAssertionBuilder(keystorePath, keystorePassword, keystoreAlias));
    }


}
