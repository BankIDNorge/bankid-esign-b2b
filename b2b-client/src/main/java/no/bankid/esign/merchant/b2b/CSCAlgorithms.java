package no.bankid.esign.merchant.b2b;

import eu.europa.esig.dss.enumerations.DigestAlgorithm;
import eu.europa.esig.dss.enumerations.EncryptionAlgorithm;
import eu.europa.esig.dss.enumerations.SignatureAlgorithm;

import java.util.EnumSet;
import java.util.Set;

import static eu.europa.esig.dss.enumerations.SignatureAlgorithm.RSA_SSA_PSS_SHA1_MGF1;

/**
 * Defines the sets of algorithms used in B2B CSC signing and provides utility methods for handling OIDs
 * Supported algorithms when using B2B CSC for signature generation, we know these are supported by B2B CSC
 * The info endpoint may be used to query supported algorithms from the CSC, and we could use that to
 * build these sets dynamically. For now these sets are hardcoded.
 * <p>
 * These sets are used in example code to demonstrate signing with various algorithms.
 * <p>
 * Remark that the SignatureAlgorithm enum does not provide OIDs for RSASSA-PSS algorithms, so to get OIDs for
 * both signature and hash algorithms, use the getSafeOid methods in this class.
 * <p>
 * Note that the CSC may support other algorithms than these, the info endpoint could be used to verify.
 * These are just a set of algorithms we know are supported and want to use in our examples.
 */
public class CSCAlgorithms {
    public final static Set<DigestAlgorithm> CSC_HASH_ALGS = EnumSet.of(
            DigestAlgorithm.SHA256,
            DigestAlgorithm.SHA384,
            DigestAlgorithm.SHA512
    );
    public final static Set<EncryptionAlgorithm> CSC_ENCRYPTION_ALGS = EnumSet.of(
            EncryptionAlgorithm.RSA,
            EncryptionAlgorithm.RSASSA_PSS
    );
    public final static Set<SignatureAlgorithm> CSC_SIGNATURE_ALGS;

    static {
        Set<SignatureAlgorithm> sigAlgs = EnumSet.noneOf(SignatureAlgorithm.class);
        for (DigestAlgorithm hashAlg : CSC_HASH_ALGS) {
            for (EncryptionAlgorithm encAlg : CSC_ENCRYPTION_ALGS) {
                sigAlgs.add(SignatureAlgorithm.getAlgorithm(encAlg, hashAlg));
            }
        }
        CSC_SIGNATURE_ALGS = sigAlgs;
    }

    public static String getSafeOid(SignatureAlgorithm signatureAlgorithm) {
        String oid = signatureAlgorithm.getOid();
        if (oid == null && signatureAlgorithm.name().contains("PSS")) {
            return RSA_SSA_PSS_SHA1_MGF1.getOid(); // "1.2.840.113549.1.1.10"
        }
        if (oid == null) {
            throw new IllegalStateException("No OID found for signature algorithm: " + signatureAlgorithm);
        }
        return oid;
    }

    public static String getSafeOid(DigestAlgorithm hashAlgorithm) {
        String oid = hashAlgorithm.getOid();
        if (oid == null) {
            throw new IllegalStateException("No OID found for hash algorithm: " + hashAlgorithm);
        }
        return oid;
    }
}
