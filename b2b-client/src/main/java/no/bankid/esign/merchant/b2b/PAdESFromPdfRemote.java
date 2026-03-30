package no.bankid.esign.merchant.b2b;

import eu.europa.esig.dss.enumerations.SignatureAlgorithm;
import no.bankid.esign.feign.api.b2b.v0.model.PadesSignRequest;
import no.bankid.esign.feign.api.b2b.v0.model.PadesSignResponse;
import no.bankid.esign.merchant.b2b.feignclients.OAuth2TokenApi;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Base64;

import static no.bankid.esign.merchant.b2b.environment.Globals.VARS;

/**
 * Example showing how to create PAdES (PDF Advanced Electronic Signatures) using the B2B AdES API.
 * <p>
 * Reads PDF files from the current directory, signs them with BankID, and writes
 * the PAdES-signed PDFs back to disk.
 * <p>
 * To run:
 * <pre>
 * mvn exec:java -Dexec.mainClass="no.bankid.esign.merchant.b2b.PAdESFromPdfRemote"
 * </pre>
 */
public class PAdESFromPdfRemote {

    public static void main(String[] args) throws Exception {
        var example = new PAdESFromPdfRemote();
        // Place PDF files (e.g., small-ex.pdf) in the working directory and list them here:
        example.signPdf("small-ex.pdf");
    }

    /**
     * Signs a PDF file using PAdES-LT (Long Term) via the B2B AdES API.
     * Uses RSA-SHA512 signature algorithm.
     *
     * @param pdfFileName the name of the PDF file to sign (must exist in the working directory)
     */
    private void signPdf(String pdfFileName) throws IOException {
        Path pdfPath = Paths.get(pdfFileName);
        if (!Files.exists(pdfPath)) {
            System.out.println("PDF file not found: " + pdfPath.toAbsolutePath() + " — skipping.");
            return;
        }
        byte[] pdfBytes = Files.readAllBytes(pdfPath);
        String pdfBytesB64 = Base64.getEncoder().encodeToString(pdfBytes);

        SignatureAlgorithm signatureAlgorithm = SignatureAlgorithm.RSA_SHA512;
        var request = new PadesSignRequest()
                .pdfBytesB64(pdfBytesB64)
                .signAlgo(signatureAlgorithm.getOid())
                .hashAlgorithmOID(signatureAlgorithm.getDigestAlgorithm().getOid())
                .reason("Testing PAdES signing")
                .padesType(PadesSignRequest.PadesTypeEnum.LT);

        OAuth2TokenApi.TokenResponse tokenResponse = VARS.getAccessTokenFromKeycloak();
        PadesSignResponse result = VARS.b2bSigner.b2bAdesApi()
                .withATAndDPoP(tokenResponse.access_token, VARS.b2bSigner.padesSignUri())
                .padesSign(request);

        String outputFileName = "remote-pades-" + pdfFileName;
        Files.write(Path.of(outputFileName), Base64.getDecoder().decode(result.getPadesB64()));
        System.out.println("PAdES saved: " + outputFileName);
        System.out.println("result.getAdesVSummaries() = " + result.getAdesVSummaries());
    }
}
