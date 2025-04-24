# BankID B2B Signing example application

This is an example application that demonstrates how to use the BankID B2B Signing API.

**This project is not actively maintained.** It is provided as a reference implementation and may not be up to date with the latest changes in the BankID API.

## Documentation
The documentation for the BankID B2B Signing API can be found at [BankID B2B Signing API](https://developer.bankid.no/bankid-esign-provider/apis/b2b/).

## Configure
Update `src/main/java/no/bankid/esign/merchant/b2b/environment/BankIDOIDCServers.java` with your OIDC client credentials

```java
    public final static OIDCServerSpec bankIDOidcServer = new OIDCServerSpec(
        "https://auth.current.bankid.no/auth/realms/current",
        "clientId",
        "clientSecret",
        "https://api.preprod.esign-stoetest.cloud"
    );
```
- The program will use the `clientId` and `clientSecret` to authenticate with the BankID OIDC server.
- The client must have the `esign/b2b` scope and also be configured to use DPoP authentication.

## Build
To build the application, run the following command:

```bash
mvn clean install
```

This will build the whole application including generating code from the OpenAPI specification.

To prevent code generation everytime you build, `cd` to the `b2b-client` directory and work from there.

## Run

To run the application, `cd` to the `b2b-client` directory:

```bash
cd b2b-client
```

Then run the application with:

```bash
mvn exec:java
```

If running from an IDE, make sure to set the working directory to the `b2b-client` when running the `B2BMiniExample`
application.

