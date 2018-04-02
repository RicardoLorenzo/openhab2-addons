package org.openhab.binding.foscamipcamera.tls;

import javax.net.SocketFactory;
import javax.net.ssl.*;
import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;


/**
 * Created by ricardo.lorenzo on 02/04/2018.
 */
public class LenientSSLSocketFactory extends SSLSocketFactory {
    private SSLSocketFactory sf = null;

    public LenientSSLSocketFactory() {
        try {
            KeyStore keyStore = KeyStore.getInstance("JKS");
            TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            trustManagerFactory.init(keyStore);
            X509TrustManager x509Manager = (X509TrustManager) trustManagerFactory.getTrustManagers()[0];
            TrustManager[] trustAllCerts = new TrustManager[] { new LenientTrustManager(x509Manager) };
            SSLContext context = SSLContext.getInstance("TLS");
            context.init(null, trustAllCerts, new java.security.SecureRandom());
            this.sf = context.getSocketFactory();
        } catch (KeyManagementException e) {
            // nothing
        } catch (NoSuchAlgorithmException e) {
            // nothing
        } catch (KeyStoreException e) {
            // nothing
        }
    }

    public static SocketFactory getDefault() {
        return new LenientSSLSocketFactory();
    }

    @Override
    public String[] getDefaultCipherSuites() {
        return new String[0];
    }

    @Override
    public String[] getSupportedCipherSuites() {
        return new String[0];
    }

    @Override
    public Socket createSocket(Socket socket, String s, int i, boolean b) throws IOException {
        if(this.sf == null) {
            throw new IOException("socket factory was not created");
        }
        return this.sf.createSocket(socket, s, i, b);
    }

    @Override
    public Socket createSocket(String s, int i) throws IOException, UnknownHostException {
        if(this.sf == null) {
            throw new IOException("socket factory was not created");
        }
        return this.sf.createSocket(s, i);
    }

    @Override
    public Socket createSocket(InetAddress inetAddress, int i) throws IOException {
        if(this.sf == null) {
            throw new IOException("socket factory was not created");
        }
        return this.sf.createSocket(inetAddress, i);
    }

    @Override
    public Socket createSocket(String s, int i, InetAddress inetAddress, int i1) throws IOException,
        UnknownHostException {
        if(this.sf == null) {
            throw new IOException("socket factory was not created");
        }
        return this.sf.createSocket(s, i, inetAddress, i1);
    }

    @Override
    public Socket createSocket(InetAddress inetAddress, int i, InetAddress inetAddress1, int i1) throws IOException {
        if(this.sf == null) {
            throw new IOException("socket factory was not created");
        }
        return this.sf.createSocket(inetAddress, i, inetAddress1, i1);
    }
}
