package no.bankid.esign.merchant.b2b.environment;

public class BankIDOIDCServers {

    public final static OIDCServerSpec bankIDOidcAtCurrent = new OIDCServerSpec(
        "https://auth.current.bankid.no/auth/realms/current",
        "clientId",
        "clientSecret"
    );

    public record OIDCServerSpec(String oidcRoot, String clientId, String clientSecret) {

    }
}
