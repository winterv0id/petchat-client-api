package com.petchat.api.httpclient;

import com.google.gson.JsonObject;
import com.petchat.api.apiclient.Response;
import org.apache.hc.client5.http.classic.methods.HttpDelete;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.entity.mime.FileBody;
import org.apache.hc.client5.http.entity.mime.MultipartEntityBuilder;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.client5.http.ssl.DefaultClientTlsStrategy;
import org.apache.hc.core5.http.*;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.apache.hc.core5.http.message.BasicHeader;
import org.apache.hc.core5.ssl.SSLContexts;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.SocketException;
import java.util.*;

public class ApacheHttpClient implements HttpClient {

    protected static final Logger LOG = LoggerFactory.getLogger(ApacheHttpClient.class);

    protected static final String ENCODING = "UTF-8";
    protected static final String FORM_CONTENT_TYPE = "application/x-www-form-urlencoded";
    protected static final String CONTENT_TYPE_HEADER = "Content-Type";

    protected static final int DEFAULT_RETRY_ATTEMPTS_NETWORK_ERROR_COUNT = 3;
    protected static final int DEFAULT_RETRY_INVALID_STATUS_COUNT = 3;

    protected CloseableHttpClient httpClient;
    private final String userAgent;
    private final Hashtable<String, BasicHeader> defaultHeaders = new Hashtable<>();

    public ApacheHttpClient(String userAgent, String clientKey, String deviceId)  {
        this.userAgent = userAgent;
        defaultHeaders.put("X-CLIENT-KEY", new BasicHeader("X-CLIENT-KEY", clientKey));
        defaultHeaders.put("X-DEVICE-ID", new BasicHeader("X-DEVICE-ID", deviceId));

        createHttpClientInstance();
    }

    private void createHttpClientInstance() {
        try {
            httpClient = HttpClients.custom()
                    .setUserAgent(userAgent)
                    .setDefaultHeaders(defaultHeaders.values())
                    .setConnectionManager(
                            //ONLY LOCAL!
                            PoolingHttpClientConnectionManagerBuilder.create()
                                    .setTlsSocketStrategy(
                                            new DefaultClientTlsStrategy(
                                                    SSLContexts.custom()
                                                            .loadTrustMaterial(
                                                                    null,
                                                                    // create TrustStrategy, trust any cert
                                                                    (chain, authType) -> true
                                                            )
                                                            .build(),
                                                    // create HostnameVerifier, trust any host
                                                    (host, session) -> true
                                            )
                                    )
                                    .build()
                    )
                    .build();
        } catch (Exception ignore) { }
    }

    public void addDefaultHeader(String name, String value) {
        defaultHeaders.put(name, new BasicHeader(name, value));
        createHttpClientInstance();
    }

    public void removeDefaultHeader(String name) {
        defaultHeaders.remove(name);
        createHttpClientInstance();
    }

    protected static Map<String, String> getHeaders(Header[] headers) {
        Map<String, String> result = new HashMap<>();
        for (Header header : headers) {
            result.put(header.getName(), header.getValue());
        }

        return result;
    }

    protected Response callWithStatusCheck(ClassicHttpRequest request) throws IOException {
        Response response;
        int attempts = 0;

        do {
            response = call(request);
            attempts++;
        } while (attempts < DEFAULT_RETRY_INVALID_STATUS_COUNT && isInvalidGatewayStatus(response.getStatusCode()));

        return response;
    }

    protected boolean isInvalidGatewayStatus(int status) {
        return status == HttpStatus.SC_BAD_GATEWAY || status == HttpStatus.SC_GATEWAY_TIMEOUT;
    }

    protected Response call(ClassicHttpRequest request) throws IOException {
        SocketException exception = null;
        for (int i = 0; i < DEFAULT_RETRY_ATTEMPTS_NETWORK_ERROR_COUNT; i++) {
            try {
                CloseableHttpResponse response = httpClient.execute(request);
                try {
                    String result = EntityUtils.toString(response.getEntity(), ENCODING);
                    Map<String, String> responseHeaders = getHeaders(response.getHeaders());
                    return new Response(response.getCode(), result, responseHeaders);
                } catch (ParseException e) {
                    throw new RuntimeException(e);
                }
            } catch (SocketException e) {
                LOG.warn("Network troubles", e);
                exception = e;
            }
        }

        throw exception;
    }

    @Override
    public Response get(String url) throws IOException {
        return get(url,null, FORM_CONTENT_TYPE);
    }

    @Override
    public Response get(String url, String parameters) throws IOException {
        return get(url, parameters, FORM_CONTENT_TYPE);
    }

    @Override
    public Response get(String url, String parameters, String contentType) throws IOException {
        HttpGet request = new HttpGet(parameters != null ? url + "?" + parameters : url);
        request.setHeader(CONTENT_TYPE_HEADER, contentType);
        return callWithStatusCheck(request);
    }

    @Override
    public Response post(String url, String body) throws IOException {
        return post(url, body, FORM_CONTENT_TYPE);
    }

    @Override
    public Response post(String url, JsonObject jsonObject) throws IOException {
        HttpPost request = new HttpPost(url);
        request.setHeader(CONTENT_TYPE_HEADER, "application/json");
        request.setEntity(new StringEntity(jsonObject.toString(), ContentType.APPLICATION_JSON));

        return callWithStatusCheck(request);
    }

    @Override
    public Response post(String url, String body, String contentType) throws IOException {
        HttpPost request = new HttpPost(url);
        request.setHeader(CONTENT_TYPE_HEADER, contentType);
        if (body != null) {
            request.setEntity(new StringEntity(body, ContentType.parse("UTF-8")));
        }

        return callWithStatusCheck(request);
    }

    @Override
    public Response post(String url, Map<String, File> files) throws IOException {
        HttpPost request = new HttpPost(url);
        MultipartEntityBuilder builder = MultipartEntityBuilder.create();
        files.forEach((key, file) -> builder.addPart(key, new FileBody(file)));
        request.setEntity(builder.build());

        return callWithStatusCheck(request);
    }

    @Override
    public Response delete(String url) throws IOException {
        return delete(url, null, FORM_CONTENT_TYPE);
    }

    @Override
    public Response delete(String url, String body) throws IOException {
        return delete(url, body, FORM_CONTENT_TYPE);
    }

    @Override
    public Response delete(String url, String body, String contentType) throws IOException {
        HttpDelete request = new HttpDelete(url);
        request.setHeader(CONTENT_TYPE_HEADER, contentType);
        if (body != null) {
            request.setEntity(new StringEntity(body, ContentType.parse("UTF-8")));
        }

        return callWithStatusCheck(request);
    }


    @Override
    public void downloadFile(String url, File outputFile, ProgressListener listener) {
        HttpGet request = new HttpGet(url);

        try (CloseableHttpResponse response = httpClient.execute(request)) {
            HttpEntity entity = response.getEntity();
            if (entity != null) {
                long totalSize = entity.getContentLength();
                try (InputStream inputStream = entity.getContent();
                     FileOutputStream outputStream = new FileOutputStream(outputFile)) {

                    byte[] buffer = new byte[4096];
                    int bytesRead;
                    long downloaded = 0;

                    while ((bytesRead = inputStream.read(buffer)) != -1) {
                        outputStream.write(buffer, 0, bytesRead);
                        downloaded += bytesRead;
                        listener.onProgress(downloaded, totalSize);
                    }

                    listener.onComplete(outputFile);
                }
            }
        } catch (Exception e) {
            listener.onError(e);
        }
    }

    @Override
    public void downloadFile(String url, File file) throws IOException {
        try (CloseableHttpResponse response = httpClient.execute(new HttpGet(url))) {
            HttpEntity entity = response.getEntity();
            if (entity != null) {
                try (FileOutputStream outstream = new FileOutputStream(file)) {
                    entity.writeTo(outstream);
                }
            }
        }
    }
}