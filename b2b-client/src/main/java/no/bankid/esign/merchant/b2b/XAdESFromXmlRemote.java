package no.bankid.esign.merchant.b2b;

import eu.europa.esig.dss.enumerations.SignatureAlgorithm;
import no.bankid.esign.feign.api.b2b.v0.model.XadesSignRequest;
import no.bankid.esign.feign.api.b2b.v0.model.XadesSignResponse;
import no.bankid.esign.merchant.b2b.feignclients.OAuth2TokenApi;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;

import static no.bankid.esign.merchant.b2b.environment.Globals.VARS;

/**
 * Example showing how to create XAdES (XML Advanced Electronic Signatures) using the B2B AdES API.
 * <p>
 * Demonstrates two signing scenarios:
 * <ul>
 *   <li>Signing a simple XML document with a specific element targeted for signature</li>
 *   <li>Signing a more complex XML document (BrReg slette-melding) with a designated signature placement element</li>
 * </ul>
 */
public class XAdESFromXmlRemote {

    public static void main(String[] args) throws Exception {
        var example = new XAdESFromXmlRemote();
        example.signSimpleXml();
        example.signSletteBrev();
    }

    /**
     * Signs a simple XML document, targeting a specific element (#dataToSign) using RSA-SHA384.
     * Uses XAdES-LT level for long-term validation.
     */
    private void signSimpleXml() throws Exception {
        String xmlToSignB64 = Base64.getEncoder().encodeToString(
                """
                <document>
                    <data id="dataToSign">
                        <infoset>
                            <info>
                                <name>John Doe</name>
                                <email>john.doe@gmail.com</email>
                            </info>
                            <info>
                                <name>Jane Doe</name>
                                <email>jane.doe@hotmail.com</email>
                            </info>
                        </infoset>
                    </data>
                </document>
                """.getBytes(StandardCharsets.UTF_8));

        SignatureAlgorithm signatureAlgorithm = SignatureAlgorithm.RSA_SHA384;
        var request = new XadesSignRequest()
                .xmlBytesB64(xmlToSignB64)
                .signAlgo(signatureAlgorithm.getOid())
                .whatToSignId("#dataToSign")
                .hashAlgorithmOID(signatureAlgorithm.getDigestAlgorithm().getOid())
                .xadesType(XadesSignRequest.XadesTypeEnum.LT);

        XadesSignResponse result = callXadesSign(request);

        Files.write(Path.of("remote-xades-simple.xml"), Base64.getDecoder().decode(result.getXadesB64()));
        System.out.println("XAdES saved: remote-xades-simple.xml");
        System.out.println("result.getAdesVSummaries() = " + result.getAdesVSummaries());
    }

    /**
     * Signs a BrReg slette-melding XML, placing the signature at a designated element (#placeSignatureHere)
     * using RSA-SHA512. Uses XAdES-B level (basic).
     */
    private void signSletteBrev() throws Exception {
        String xmlToSignB64 = Base64.getEncoder().encodeToString(
                """
                <Melding xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xmlns="http://schema.brreg.no/postmottak/henvendelse"
                         versjon="4.0" id="f2fad110-6773-4e89-9415-75082bc7aa40" tjeneste="eting" tjenestehandling="sletting">
                    <Mottaker id="974760673" idType="Organisasjonsnummer"/>
                    <Avsender id="314266676" idType="Organisasjonsnummer"/>
                    <Referanse referanse="f2fad110-6773-4e89-9415-75082bc7aa40" referanseType="MeldingsReferanse"/>
                    <Innhold>
                        <Data format="xml">
                            <melding xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                                     xmlns="http://seres.no/xsd/Losoreregisteret/LrSletting/30012">
                                <skjemainnhold>
                                    <innsender>
                                        <organisasjonsnummer>314266676</organisasjonsnummer>
                                        <navn>AKSEPTABEL SLAPP TIGER AS</navn>
                                        <innsendersReferanse>f2fad110-6773-4e89-9415-75082bc7aa40</innsendersReferanse>
                                        <folgeseddel xsi:nil="true"/>
                                    </innsender>
                                    <hoveddagbok>
                                        <dagboknummer>1000216811</dagboknummer>
                                        <embedsnummer xsi:nil="true"/>
                                        <bekreftetIkkeTransportert>J</bekreftetIkkeTransportert>
                                    </hoveddagbok>
                                    <pantsetterPerson>
                                        <fodselsnummer>30843549825</fodselsnummer>
                                    </pantsetterPerson>
                                    <panthaverEnhet>
                                        <organisasjonsnummer>314266676</organisasjonsnummer>
                                    </panthaverEnhet>
                                    <sletting>
                                        <slettingskode>H</slettingskode>
                                    </sletting>
                                </skjemainnhold>
                            </melding>
                        </Data>
                    </Innhold>
                    <Signatur id="placeSignatureHere"></Signatur>
                </Melding>
                """.getBytes(StandardCharsets.UTF_8));

        SignatureAlgorithm signatureAlgorithm = SignatureAlgorithm.RSA_SHA512;
        var request = new XadesSignRequest()
                .xmlBytesB64(xmlToSignB64)
                .signAlgo(signatureAlgorithm.getOid())
                .whereToPlaceSignatureId("#placeSignatureHere")
                .hashAlgorithmOID(signatureAlgorithm.getDigestAlgorithm().getOid())
                .xadesType(XadesSignRequest.XadesTypeEnum.B);

        XadesSignResponse result = callXadesSign(request);

        Files.write(Path.of("remote-xades-slettebrev.xml"), Base64.getDecoder().decode(result.getXadesB64()));
        System.out.println("XAdES saved: remote-xades-slettebrev.xml");
        System.out.println("result.getAdesVSummaries() = " + result.getAdesVSummaries());
    }

    private static XadesSignResponse callXadesSign(XadesSignRequest request) {
        OAuth2TokenApi.TokenResponse tokenResponse = VARS.getAccessTokenFromKeycloak();
        return VARS.b2bSigner.b2bAdesApi()
                .withATAndDPoP(tokenResponse.access_token, VARS.b2bSigner.xadesSignUri())
                .xadesSign(request);
    }
}
