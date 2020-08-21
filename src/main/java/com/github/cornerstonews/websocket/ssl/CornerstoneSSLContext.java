package com.github.cornerstonews.websocket.ssl;

import java.io.File;
import java.io.FileOutputStream;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.util.List;
import java.util.Random;

public class CornerstoneSSLContext {

    private String certificateChainFilePath;
    private String certificateFilePath;
    private String certificateKeyFilePath;

    int leftLimit = 48; // numeral '0'
    int rightLimit = 122; // letter 'z'
    int passwordLength = 20;

    public CornerstoneSSLContext(String certificateFilePath, String certificateKeyFilePath, String certificateChainFilePath) {
        this.certificateFilePath = certificateFilePath;
        this.certificateKeyFilePath = certificateKeyFilePath;
        this.certificateChainFilePath = certificateChainFilePath;
    }

    public CornerstoneKeyStore getKeyStore() {
        try {
            PrivateKey certificateKey = new PEMFile(this.certificateKeyFilePath).toPrivateKey();
            Certificate certificate = new PEMFile(this.certificateFilePath).toCertificates().get(0);
            List<Certificate> caCertificates = new PEMFile(this.certificateChainFilePath).toCertificates();

            KeyStore keyStore = KeyStore.getInstance("JKS");
            keyStore.load(null, null);
            int i = 0;
            for (Certificate caCertificate : caCertificates) {
                keyStore.setCertificateEntry("chain-cert-" + ++i, caCertificate);
            }
            keyStore.setCertificateEntry("cert", certificate);

            String randomPassword = new Random().ints(leftLimit, rightLimit + 1) // .filter(i -> (i <= 57 || i >= 65) && (i <= 90 || i >= 97))
                    .limit(passwordLength).collect(StringBuilder::new, StringBuilder::appendCodePoint, StringBuilder::append).toString();

            File temp = File.createTempFile(".CornerstoneKeyStore", ".jks");
            keyStore.setKeyEntry("client-key", certificateKey, randomPassword.toCharArray(), new Certificate[] { certificate });
            keyStore.store(new FileOutputStream(temp), randomPassword.toCharArray());

            return new CornerstoneKeyStore(temp.getAbsolutePath(), randomPassword);
        } catch (Exception e) {
            throw new RuntimeException("Cannot build keystore", e);
        }
    }

}
