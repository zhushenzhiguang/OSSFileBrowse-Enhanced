package net.jdr2021.utils;

import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;

import java.io.*;
import java.security.SecureRandom;

import java.net.HttpURLConnection;
import java.net.URL;
import java.security.cert.X509Certificate;
import java.util.Optional;
import javax.net.ssl.*;

/**
 * @version 1.0
 * @Author jdr
 * @Date 2024-5-24 21:27
 * @注释
 */


public class HttpUtils {

    static {
        // 忽略主机名验证
        TrustManager[] trustAllCertificates = new TrustManager[]{
                new X509TrustManager() {
                    @Override
                    public X509Certificate[] getAcceptedIssuers() {
                        return null;
                    }
                    @Override
                    public void checkClientTrusted(X509Certificate[] certs, String authType) {
                    }

                    @Override
                    public void checkServerTrusted(X509Certificate[] certs, String authType) {
                    }
                }
        };

        try {
            SSLContext sslContext = SSLContext.getInstance("SSL");
            sslContext.init(null, trustAllCertificates, new java.security.SecureRandom());
            HttpsURLConnection.setDefaultSSLSocketFactory(sslContext.getSocketFactory());
            HttpsURLConnection.setDefaultHostnameVerifier((hostname, session) -> true);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // 创建一个 "trust-all" SSLContext（仅用于测试）
    private static SSLContext createTrustAllSslContext() throws Exception {
        TrustManager[] trustAll = new TrustManager[]{
                new X509TrustManager() {
                    public void checkClientTrusted(X509Certificate[] certs, String authType) { }
                    public void checkServerTrusted(X509Certificate[] certs, String authType) { }
                    public X509Certificate[] getAcceptedIssuers() { return new X509Certificate[0]; }
                }
        };
        SSLContext sc = SSLContext.getInstance("TLS");
        sc.init(null, trustAll, new SecureRandom());
        return sc;
    }

    // 忽略主机名校验（仅用于测试）
    private static HostnameVerifier trustAllHostnameVerifier = new HostnameVerifier() {
        public boolean verify(String hostname, SSLSession session) {
            return true;
        }
    };

    public static void httpGetBinary(String url, boolean ignoreHttpsCerts, String saveFilePath) throws Exception {
        HttpURLConnection connection = null;
        InputStream inputStream = null;
        FileOutputStream fileOutputStream = null;

        try {
            URL getUrl = new URL(url);
            connection = (HttpURLConnection) getUrl.openConnection();
            connection.setRequestMethod("GET");
            connection.setRequestProperty("Accept", "*/*");

            // 如果是 HTTPS，并且用户要求忽略证书
            if (connection instanceof HttpsURLConnection) {
                HttpsURLConnection https = (HttpsURLConnection) connection;
                if (ignoreHttpsCerts) {
                    SSLContext trustAllContext = createTrustAllSslContext();
                    if (trustAllContext != null) {
                        https.setSSLSocketFactory(trustAllContext.getSocketFactory());
                        https.setHostnameVerifier(trustAllHostnameVerifier);
                    }
                }
            }

            connection.connect();

            // 获取输入流
            inputStream = connection.getInputStream();
            fileOutputStream = new FileOutputStream(saveFilePath);

            byte[] buffer = new byte[8192]; // 8KB缓冲
            int bytesRead;

            while ((bytesRead = inputStream.read(buffer)) != -1) {
                fileOutputStream.write(buffer, 0, bytesRead);
            }

            fileOutputStream.flush();
            System.out.println("文件保存成功: " + saveFilePath);



        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            if (inputStream != null) inputStream.close();
            if (fileOutputStream != null) fileOutputStream.close();
            if (connection != null) connection.disconnect();
        }
    }

    public static String httpGet(String url, boolean ignoreHttpsCerts) throws Exception {
        HttpURLConnection connection = null;
        BufferedReader reader = null;
        StringBuilder response = new StringBuilder();

        try {
            URL getUrl = new URL(url);
            connection = (HttpURLConnection) getUrl.openConnection();
            connection.setRequestMethod("GET");
            connection.setRequestProperty("Accept", "*/*");

            // 如果是 HTTPS，并且用户要求忽略证书，则在此连接上应用 SSLContext + HostnameVerifier
            if (connection instanceof HttpsURLConnection) {
                HttpsURLConnection https = (HttpsURLConnection) connection;
                if (ignoreHttpsCerts) {
                    SSLContext trustAllContext = null;
                    if (ignoreHttpsCerts) {
                        trustAllContext = createTrustAllSslContext();
                    }
                    if (trustAllContext != null) {
                        https.setSSLSocketFactory(trustAllContext.getSocketFactory());
                        https.setHostnameVerifier(trustAllHostnameVerifier);
                    }
                }
            }

            reader = new BufferedReader(new InputStreamReader(connection.getInputStream(),"UTF-8"));

            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
        } finally {
            if (reader != null) {
                reader.close();
            }
            if (connection != null) {
                connection.disconnect();
            }
        }

        return response.toString();
    }

    public static String httpPost(String url, String body) throws IOException {
        HttpURLConnection connection = null;
        BufferedReader reader = null;
        StringBuilder response = new StringBuilder();

        try {
            URL postUrl = new URL(url);
            connection = (HttpURLConnection) postUrl.openConnection();
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "*/*");
            connection.setDoOutput(true);

            OutputStream os = connection.getOutputStream();
            os.write(body.getBytes());
            os.flush();
            os.close();

            reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));

            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
        } finally {
            if (reader != null) {
                reader.close();
            }
            if (connection != null) {
                connection.disconnect();
            }
        }

        return response.toString();
    }
}

