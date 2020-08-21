package com.github.cornerstonews.websocket.ssl;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

public class PEMFile {

    // PKCS#8 format
    final private String PEM_PRIVATE_BEGIN = "-----BEGIN PRIVATE KEY-----";
    final private String PEM_PRIVATE_END = "-----END PRIVATE KEY-----";

    // PKCS#1 format
    final String PEM_RSA_PRIVATE_BEGIN = "-----BEGIN RSA PRIVATE KEY-----";
    final String PEM_RSA_PRIVATE_END = "-----END RSA PRIVATE KEY-----";

    // RFC5915 format
    final String PEM_EC_PRIVATE_BEGIN = "-----BEGIN EC PRIVATE KEY-----";
    final String PEM_EC_PRIVATE_END = "-----END EC PRIVATE KEY-----";

    // Certificate format
    final String PEM_CERTIFICATE_BEGIN = "-----BEGIN CERTIFICATE-----";
    final String PEM_CERTIFICATE_END = "-----END CERTIFICATE-----";

    // X509 format
    final String PEM_X509_CERTIFICATE_BEGIN = "-----BEGIN X509 CERTIFICATE-----";
    final String PEM_X509_CERTIFICATE_END = "-----END X509 CERTIFICATE-----";

    final private String filename;
    final private byte[] pemContent;
    final private List<byte[]> pemCertContents;
    private PemType type;
    private String keyAlgorithm;

    private enum PemType {
        PRIVATE_KEY, // PKCS#8 format
        RSA_PRIVATE_KEY, // PKCS#1 format
        RSA_EC_KEY, // RFC5915 format
        CERTIFICATE
    }

    public PEMFile(String filename) throws IOException, GeneralSecurityException {
        this(filename, null);
    }

    public PEMFile(String filename, String keyAlgorithm) throws IOException, GeneralSecurityException {
        this.filename = filename;
        this.keyAlgorithm = keyAlgorithm;

        List<String> pemStringList = new ArrayList<>();

        StringBuilder pemBuilder = new StringBuilder();
        try (InputStream inputStream = new FileInputStream(filename)) {
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.US_ASCII));
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.contains(PEM_PRIVATE_BEGIN)) {
                    type = PemType.PRIVATE_KEY;
                    pemBuilder.append(line.replace(PEM_PRIVATE_BEGIN, "").trim());
                } else if (line.contains(PEM_PRIVATE_END)) {
                    pemBuilder.append(line.replace(PEM_PRIVATE_END, "").trim());
                    pemStringList.add(pemBuilder.toString().trim());
                    pemBuilder = new StringBuilder();
                } else if (line.contains(PEM_RSA_PRIVATE_BEGIN)) {
                    type = PemType.RSA_PRIVATE_KEY;
                    pemBuilder.append(line.replace(PEM_RSA_PRIVATE_BEGIN, "").trim());
                } else if (line.contains(PEM_RSA_PRIVATE_END)) {
                    pemBuilder.append(line.replace(PEM_RSA_PRIVATE_END, "").trim());
                    pemStringList.add(pemBuilder.toString().trim());
                    pemBuilder = new StringBuilder();
                } else if (line.contains(PEM_EC_PRIVATE_BEGIN)) {
                    type = PemType.RSA_EC_KEY;
                    pemBuilder.append(line.replace(PEM_EC_PRIVATE_BEGIN, "").trim());
                } else if (line.contains(PEM_EC_PRIVATE_END)) {
                    pemBuilder.append(line.replace(PEM_EC_PRIVATE_END, "").trim());
                    pemStringList.add(pemBuilder.toString().trim());
                    pemBuilder = new StringBuilder();
                } else if (line.contains(PEM_CERTIFICATE_BEGIN) || line.contains(PEM_X509_CERTIFICATE_BEGIN)) {
                    type = PemType.CERTIFICATE;
                    line = line.replace(PEM_CERTIFICATE_BEGIN, "").trim();
                    line = line.replace(PEM_X509_CERTIFICATE_BEGIN, "").trim();
                    pemBuilder.append(line);
                } else if (line.contains(PEM_CERTIFICATE_END) || line.contains(PEM_X509_CERTIFICATE_END)) {
                    line = line.replace(PEM_CERTIFICATE_END, "").trim();
                    line = line.replace(PEM_X509_CERTIFICATE_END, "").trim();
                    pemStringList.add(pemBuilder.toString().trim());
                    pemBuilder = new StringBuilder();
                    pemBuilder.append(line);
                } else if (!line.contains(":") && !line.startsWith(" ")) {
                    pemBuilder.append(line.trim());
                }
            }
        }

        if (type == PemType.CERTIFICATE) {
            this.pemContent = null;
            this.pemCertContents = new ArrayList<byte[]>();
            for (String pemString : pemStringList) {
                pemCertContents.add(Base64.getDecoder().decode(pemString));
            }
        } else {
            this.pemCertContents = null;
            this.pemContent = Base64.getDecoder().decode(pemStringList.get(0));
        }
    }

    public List<Certificate> toCertificates() throws CertificateException {
        CertificateFactory factory = CertificateFactory.getInstance("X.509");
        List<Certificate> certs = new ArrayList<Certificate>();
        for(byte[] pemCertContent : pemCertContents) { 
            certs.add(factory.generateCertificate(new ByteArrayInputStream(pemCertContent)));
        }
        return certs;
    }

    public PrivateKey toPrivateKey() throws GeneralSecurityException, IOException {
        KeySpec keySpec = null;

        switch (type) {
        case RSA_PRIVATE_KEY: { // PKCS#8 format
            keySpec = parsePKCS1(this.pemContent);
            break;
        }
        case PRIVATE_KEY: { // PKCS#1 format
            keySpec = new PKCS8EncodedKeySpec(this.pemContent);
            break;
        }
        }

        InvalidKeyException exception = new InvalidKeyException("Could not parse private key file: " + filename);
        if (keyAlgorithm == null) {
            for (String algorithm : new String[] { "RSA", "DSA", "EC" }) {
                try {
                    return KeyFactory.getInstance(algorithm).generatePrivate(keySpec);
                } catch (InvalidKeySpecException e) {
                    exception.addSuppressed(e);
                }
            }
        } else {
            try {
                return KeyFactory.getInstance(keyAlgorithm).generatePrivate(keySpec);
            } catch (InvalidKeySpecException e) {
                exception.addSuppressed(e);
            }
        }

        throw exception;
    }

    // https://github.com/Mastercard/client-encryption-java/blob/master/src/main/java/com/mastercard/developer/utils/EncryptionUtils.java
    private static KeySpec parsePKCS1(byte[] pemContent) throws GeneralSecurityException {
        // We can't use Java internal APIs to parse ASN.1 structures, so we build a PKCS#8 key Java can understand
        int pkcs1Length = pemContent.length;
        int totalLength = pkcs1Length + 22;
        byte[] pkcs8Header = new byte[] { 0x30, (byte) 0x82, (byte) ((totalLength >> 8) & 0xff), (byte) (totalLength & 0xff), // Sequence + total length
                0x2, 0x1, 0x0, // Integer (0)
                0x30, 0xD, 0x6, 0x9, 0x2A, (byte) 0x86, 0x48, (byte) 0x86, (byte) 0xF7, 0xD, 0x1, 0x1, 0x1, 0x5, 0x0, // Sequence: 1.2.840.113549.1.1.1, NULL
                0x4, (byte) 0x82, (byte) ((pkcs1Length >> 8) & 0xff), (byte) (pkcs1Length & 0xff) // Octet string + length
        };
        byte[] pkcs8bytes = new byte[pkcs8Header.length + pemContent.length];
        System.arraycopy(pkcs8Header, 0, pkcs8bytes, 0, pkcs8Header.length);
        System.arraycopy(pemContent, 0, pkcs8bytes, pkcs8Header.length, pemContent.length);

        return new PKCS8EncodedKeySpec(pkcs8bytes);
    }

}
