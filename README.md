# BankID B2B Signing example application

This is example applications that demonstrates how to use the BankID B2B Signing API and the BankID B2B CSC signing API.

**This project is not actively maintained.** It is provided as a reference implementation and may not be up to date with the latest changes in the BankID API.

## Documentation

The documentation for the BankID B2B Signing API can be found
at [BankID B2B Signing API](https://developer.bankid.no/bankid-esign-provider/apis/b2b/v0/B2B-Bankid-Signer/).
The documentation for the BankID B2B CSC signing API can be found
at [BankID B2B CSC Signing API](https://developer.bankid.no/bankid-esign-provider/apis/b2b/csc/b2b-csc-overview/).

## Prerequisite 

- To use the example applications, you need to have an OIDC client registered with BankID OIDC.
- You need to have the `esign/b2b` scope enabled for your client.
- You need to use `private_key_jwt` authentication method, not `basic`.
- You also need to use DPoP authentication when calling BankID OIDC and the B2B signing API.
- You need to have a keystore with a private key.
- You need to register the corresponding public key in JWK format with BankID OIDC.

## Keystore
- The example code uses a Java keystore to store the private key.
- Keystore and passwords in this example are for demonstration purposes only, and should be handled properly in a real application.

There are several ways to create a keystore, below there is an example using the `keytool` command line tool that comes with the JDK.
```bash
keytool -genkeypair -alias private_key_jwk -keyalg EC -groupname secp256r1 -keystore
private_key_jwk_keystore.jks -storepass changeit -keypass changeit -dname "cn=Esign B2B Key,o=Your org,c=NO"
```

- This will create a keystore file named `private_key_jwk_keystore.jks` in the current directory.
- The certificate in the keystore will get an exiration date 90 days from now, but the keys will not expire.
- The password, alias, and path to this file must be set in the code below. 
- Many algorithms are supported, see https://auth.current.bankid.no/auth/realms/current/.well-known/openid-configuration

Register the keystore in the code and run. It will print the public key that must be registered in BankID OIDC
Register the public key in BankID OIDC, and complete the configuration. You are ready to run the rest of the example code.

## Configuration

Update `src/main/java/no/bankid/esign/merchant/b2b/environment/BankIDOIDCServers.java` with your OIDC client credentials.
```java
    private final static OIDCServerSpec bankIDOidcCurrentKeyAuthentication = serverWithPrivateKeyJwtSpec(
        "https://auth.current.bankid.no/auth/realms/current",
        "your-client-id",
        "path-to-your-keystore.jks",
        "your-keystore-password",
        "your-private-key-alias-in-keystore",
        "https://api.preprod.esign-stoetest.cloud"
);

public final static OIDCServerSpec bankIDOidcServer = bankIDOidcCurrentKeyAuthentication;
```

- In test environments `basic` authentication may work, but not in production, where `private_key_jwt` is required
- The example code supports both, but `private_key_jwt` is required in production.


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

Then run the applications with: (I could not make this run in a power shell window, only in command prompt window)

### B2B v0 — SDO-based signing
```bash
mvn exec:java -Dexec.mainClass="no.bankid.esign.merchant.b2b.B2BMiniExample"
```

### CSC v2 — Raw hash signing
```bash
mvn exec:java -Dexec.mainClass="no.bankid.esign.merchant.b2b.CSCMiniExample"
```

### XAdES — Server-side XML signing
Creates XAdES Baseline B or LT signatures from XML documents. The server handles certificate management, OCSP, and optional timestamping.
```bash
mvn exec:java -Dexec.mainClass="no.bankid.esign.merchant.b2b.XAdESFromXmlRemote"
```
This example demonstrates two scenarios:
- Signing a simple XML with a targeted element (`#dataToSign`) using XAdES-LT (with timestamp)
- Signing a Brønnøysund Registry XML with a designated signature placement (`#placeSignatureHere`) using XAdES-B

### PAdES — Server-side PDF signing
Creates PAdES Baseline LT signatures from PDF documents. Place PDF files (e.g. `small-ex.pdf`) in the `b2b-client` working directory before running.
```bash
mvn exec:java -Dexec.mainClass="no.bankid.esign.merchant.b2b.PAdESFromPdfRemote"
```

If running from an IDE, make sure to set the working directory to the `b2b-client` when running any of the example applications.

Good luck!
