# BankID B2B Signing example application

This is an example application that demonstrates how to use the BankID B2B Signing API.

## Configure
Update `src/main/java/no/bankid/esign/merchant/b2b/environment/BankIDOIDCServers.java` with your OIDC client credentials

```java
    public final static OIDCServerSpec bankIDOidcAtCurrent = new OIDCServerSpec(
        "https://auth.current.bankid.no/auth/realms/current",
        "clientId",
        "clientSecret"
    );
```


## Build
To build the application, run the following command:

```bash
mvn clean install
```

## Run
To run the application, use the following command:
