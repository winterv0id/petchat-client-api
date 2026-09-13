package com.petchat.api.apiclient;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.stream.JsonReader;
import com.petchat.api.exceptions.ApiClientException;
import com.petchat.api.exceptions.ApiServerException;
import com.petchat.api.exceptions.NotAuthorizedException;
import com.petchat.api.httpclient.HttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.StringReader;
import java.lang.reflect.Type;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

public abstract class ApiRequest<T> {
    public enum HttpMethod { GET, POST }

    private static final Logger LOGGER = LoggerFactory.getLogger(ApiRequest.class);

    private final HttpClient client;
    protected HttpClient getClient() {
        return client;
    }

    private final Gson gson;
    protected Gson getGson() { return gson; }

    private final String url;
    public String getUrl() {
        return url;
    }

    private final Type responseClass;
    public Type getResponseClass() {
        return responseClass;
    }

    private final HttpMethod httpMethod;
    public HttpMethod getHttpMethod() { return httpMethod; }

    public ApiRequest(String url, HttpClient client, Type responseClass, HttpMethod httpMethod) {
        this.client = client;
        this.url = url;
        this.responseClass = responseClass;
        this.gson = new GsonBuilder().disableHtmlEscaping().create();
        this.httpMethod = httpMethod;
    }

    public CompletableFuture<T> execute() {
        return executeAsClientResponse().thenApplyAsync(this::parseClientResponse);
    }

    private T parseClientResponse(Response clientResponse) {
        String textResponse = clientResponse.getContent();
        JsonReader jsonReader = new JsonReader(new StringReader(textResponse));
        JsonObject json = JsonParser.parseReader(jsonReader).getAsJsonObject();

        //TODO error handle
//        if (json.has("error")) {
//            JsonElement errorElement = json.get("error");
//            Error error;
//            try {
//                error = gson.fromJson(errorElement, Error.class);
//            } catch (JsonSyntaxException e) {
//                LOG.error("Invalid JSON: {}", textResponse, e);
//                throw new ParserException("Can't parse json response");
//            }
//            ApiException exc = ExceptionMapper.parseException(error);
//            ApiExtendedException extendedException = new ApiExtendedException(
//                    error.setErrorText(exc.getDescription()),
//                    clientResponse.getStatusCode(),
//                    clientResponse.getHeaders()
//            );
//
//            LOG.error("API error", extendedException);
//            throw extendedException;
//        }

        return gson.fromJson(json, responseClass);
    }

    public CompletableFuture<Response> executeAsClientResponse() {
        return CompletableFuture.supplyAsync(() -> {
            Response response;
            try {
                switch (httpMethod) {
                    case GET -> response = client.get(url, getQueryString());
                    case POST ->  response = client.post(url, getJsonObject());
                    default -> response = null;
                }
            } catch (IOException e) {
                LOGGER.error("Problems with request: {}", url, e);
                throw new CompletionException(e);
            }

            int statusCode = response.getStatusCode();
            if (statusCode >= 400) {
                Exception e;
                if (statusCode == 401)
                    e = new NotAuthorizedException(response.getContent());
                else if (statusCode < 500)
                    e = new ApiClientException(response.getStatusCode(), response.getContent());
                else
                    e = new ApiServerException(response.getStatusCode(), response.getContent());

                LOGGER.error("Problems with request: {}", url, e);
                throw new CompletionException(e);
            }

            return response;
        });
    }

    public CompletableFuture<String> executeAsString() throws ApiServerException {
        return executeAsClientResponse().thenApply(ResponseTypeable::getContent);
    }

    public CompletableFuture<Response> executeAsRaw() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return client.post(url, getQueryString());
            } catch (IOException e) {
                LOGGER.error("Problems with request: {}", url, e);
                throw new CompletionException(e);
            }
        });
    }

    protected abstract String getQueryString();
    protected abstract JsonObject getJsonObject();
}
