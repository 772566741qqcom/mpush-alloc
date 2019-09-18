/* Copyright rememberjava.com. Licensed under GPL 3. See http://rememberjava.com/license */
package com.shinemo.mpush.alloc;

import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpsConfigurator;
import com.sun.net.httpserver.HttpsServer;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;
import java.io.File;
import java.io.FileInputStream;
import java.lang.ProcessBuilder.Redirect;
import java.net.InetSocketAddress;
import java.security.KeyStore;

/**
 * A HTTPS server using a self-signed TLS 1.2 key and certificate generated by
 * the Java keytool command.
 * <p>
 */
@SuppressWarnings("restriction")
public class HttpServerCreator {

  private static final File KEYSTORE_FILE = new File(System.getProperty("java.io.tmpdir"), "mpush.jks");

  private static final String KEYSTORE_PASSWORD = "mpush_2017";

  private static final String KEY_PASSWORD = "mpush_2017";

  /**
   * Generates a new self-signed certificate in /tmp/test.jks, if it does not
   * already exist.
   */
  private static void generateCertificate() throws Exception {
    if (KEYSTORE_FILE.exists()) {
      return;
    } ;

    System.setProperty("javax.net.debug", "all");

    File keytool = new File(System.getProperty("java.home"), "bin/keytool");

    String[] genkeyCmd =
      new String[] {keytool.toString(), "-genkey", "-keyalg", "RSA", "-alias", "mpush.com", "-validity", "365",
        "-keysize", "2048", "-dname", "cn=mpush.com,ou=mpush,o=OHUN .Inc,c=CN", "-keystore",
        KEYSTORE_FILE.getAbsolutePath(), "-storepass", KEYSTORE_PASSWORD, "-keypass", KEY_PASSWORD};

    System.out.println(String.join(" ", genkeyCmd));

    ProcessBuilder processBuilder = new ProcessBuilder(genkeyCmd);
    processBuilder.redirectErrorStream(true);
    processBuilder.redirectOutput(Redirect.INHERIT);
    processBuilder.redirectError(Redirect.INHERIT);
    Process exec = processBuilder.start();
    exec.waitFor();

    System.out.println("Exit value: " + exec.exitValue());

  }

  private static HttpsServer createHttpsServer(int port) throws Exception {
    generateCertificate();
    HttpsServer httpsServer = HttpsServer.create(new InetSocketAddress(port), 0);
    SSLContext sslContext = getSslContext();
    httpsServer.setHttpsConfigurator(new HttpsConfigurator(sslContext));
    return httpsServer;
  }

  private static HttpServer createHttpServer(int port) throws Exception {
    HttpServer httpServer = HttpServer.create(new InetSocketAddress(port), 0);
    return httpServer;
  }

  private static SSLContext getSslContext() throws Exception {
    KeyStore ks = KeyStore.getInstance("JKS");
    ks.load(new FileInputStream(KEYSTORE_FILE), KEYSTORE_PASSWORD.toCharArray());

    KeyManagerFactory kmf = KeyManagerFactory.getInstance("SunX509");
    kmf.init(ks, KEY_PASSWORD.toCharArray());

    TrustManagerFactory tmf = TrustManagerFactory.getInstance("SunX509");
    tmf.init(ks);

    SSLContext sslContext = SSLContext.getInstance("TLS");
    sslContext.init(kmf.getKeyManagers(), tmf.getTrustManagers(), null);

    return sslContext;
  }

  public static HttpServer createServer(int port, boolean https) {
    try {
      if (https) {
        return HttpServerCreator.createHttpsServer(port);
      } else {
        return HttpServerCreator.createHttpServer(port);
      }
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }
}