package no.bankid.esign.merchant.b2b.environment;

public class BankIDOIDCServers {

    public final static OIDCServerSpec bankIDOidcServer = new OIDCServerSpec(
            "https://auth.current.bankid.no/auth/realms/current",
            "clientId",
            "clientSecret",
            "https://api.preprod.esign-stoetest.cloud"
    );

    public record OIDCServerSpec(
            String oidcRoot,
            String clientId,
            String clientSecret,
            String b2bSignerRootUrl) {

    }
}
