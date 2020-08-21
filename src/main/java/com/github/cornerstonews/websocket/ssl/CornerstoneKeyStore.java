package com.github.cornerstonews.websocket.ssl;

public class CornerstoneKeyStore {

    private String keystore;
    private String keystorePassword;

    public CornerstoneKeyStore(String keystore, String keystorePassword) {
        this.keystore = keystore;
        this.keystorePassword = keystorePassword;
    }

    public String getKeystore() {
        return keystore;
    }

    public void setKeystore(String keystore) {
        this.keystore = keystore;
    }

    public String getKeystorePassword() {
        return keystorePassword;
    }

    public void setKeystorePassword(String keystorePassword) {
        this.keystorePassword = keystorePassword;
    }

}
