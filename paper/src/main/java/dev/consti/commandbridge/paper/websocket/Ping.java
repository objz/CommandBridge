package dev.consti.commandbridge.paper.websocket;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.Timer;
import java.util.TimerTask;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import dev.consti.commandbridge.paper.core.Runtime;
import dev.consti.foundationlib.logging.Logger;
import dev.consti.foundationlib.utils.ConfigManager;

public class Ping {
    private static void startPing(Logger logger, Runnable onPong, String url, int port, int maxAttempts) {
        final int millis = 4000;

        Timer timer = new Timer();

        disableCertificateValidation();

        timer.scheduleAtFixedRate(new TimerTask() {
            int attempts = 0;

            @Override
            public void run() {
                attempts++;
                if (attempts > maxAttempts) {
                    logger.warn("Max reconnect attempts reached. Reconnect manual by running '/cbc reconnect'");
                    timer.cancel();
                    return;
                }

                try {
                    URL targetUrl = new URL("https://" + url + ":" + port + "/ping");
                    HttpsURLConnection conn = (HttpsURLConnection) targetUrl.openConnection();
                    conn.setRequestMethod("GET");
                    conn.setConnectTimeout(1500);
                    conn.setReadTimeout(1500);

                    logger.debug("Pinging WebsocketServer");

                    int responseCode = conn.getResponseCode();
                    if (responseCode == 200) {
                        BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                        String response = in.readLine();
                        in.close();

                        if ("pong".equalsIgnoreCase(response)) {
                            logger.info("Attempting to reconnect to the server!");
                            onPong.run();
                            timer.cancel();
                        }
                    }
                } catch (Exception e) {
                    // logger.error("Ping error: {}", e);
                }
            }
        }, 0, millis);
    }

    public static void reconnect(Logger logger) {
        ConfigManager config = Runtime.getInstance().getConfig();
        int attempts;
        try {
            attempts = Integer.parseInt(config.getKey("config.yml", "timeout")) / 4;
        } catch (RuntimeException e) {
            attempts = 120 / 4;
        }

        startPing(logger, () -> {
            Runtime.getInstance().getClient().disconnect();

            String host = config.getKey("config.yml", "remote");
            int port = Integer.parseInt(config.getKey("config.yml", "port"));

            try {
                Runtime.getInstance().getClient().connect(host, port);
                logger.info("Client reconnected successfully to {}:{}", host, port);
            } catch (Exception e) {
                logger.error("Client reconnection failed (but ping succeeded):", e);
            }

        }, config.getKey("config.yml", "remote"),
                Integer.parseInt(config.getKey("config.yml", "port")),
                attempts);
    }

    private static void disableCertificateValidation() {
        try {
            TrustManager[] trustAllCerts = new TrustManager[] {
                    new X509TrustManager() {
                        public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                            return new java.security.cert.X509Certificate[0];
                        }

                        public void checkClientTrusted(java.security.cert.X509Certificate[] certs, String authType) {
                        }

                        public void checkServerTrusted(java.security.cert.X509Certificate[] certs, String authType) {
                        }
                    }
            };

            SSLContext sc = SSLContext.getInstance("TLS");
            sc.init(null, trustAllCerts, new java.security.SecureRandom());
            HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
            HttpsURLConnection.setDefaultHostnameVerifier((hostname, session) -> true);
        } catch (Exception e) {
            throw new RuntimeException("Failed to disable certificate validation", e);
        }
    }
}
