package org.openhab.binding.foscamipcamera.tls;

import javax.net.ssl.X509TrustManager;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;


/**
 * Created by ricardo.lorenzo on 02/04/2018.
 */
public class LenientTrustManager implements X509TrustManager {
    private X509Certificate[] chain;

    public LenientTrustManager(X509TrustManager trustManager) {
    }

    public X509Certificate[] getAcceptedIssuers() {
        return new X509Certificate[] {};
    }

    @Override
    public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {
        this.chain = chain;
        // throw new UnsupportedOperationException();
    }

    @Override
    public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
        this.chain = chain;
        // this.tm.checkServerTrusted(chain, authType);
    }

    public X509Certificate[] getServerCertificate() {
        return this.chain;
    }
}
