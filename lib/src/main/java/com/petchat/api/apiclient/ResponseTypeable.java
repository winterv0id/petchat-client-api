package com.petchat.api.apiclient;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

public class ResponseTypeable<T> {
    private final int statusCode;
    private final T content;
    private final Map<String, String> headers;

    public ResponseTypeable(int statusCode, T content, Map<String, String> headers) {
        this.statusCode = statusCode;
        this.content = content;
        this.headers = headers;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public T getContent() {
        return content;
    }

    public JsonElement getJson() {
        return GsonDeserializer.deserialize(new Gson().toJson(content), JsonElement.class);
    }
    public JsonObject getJsonObject() {
        return GsonDeserializer.deserialize(new Gson().toJson(content), JsonObject.class);
    }
    public JsonArray getJsonArray() {
        return GsonDeserializer.deserialize(new Gson().toJson(content), JsonArray.class);
    }

    public Map<String, String> getHeaders() {
        return headers;
    }

    public String getCookie(String header, String cookie) {
        return Arrays.stream(this.getHeaders().get(header).split(";"))
                .filter(entry -> entry.contains(cookie))
                .map(entry -> entry.split("=")[1])
                .findFirst().orElse(null);
    }

    public Map<String, String> getCookies(String header) {
        return Arrays.stream(this.getHeaders().get(header).split(";"))
                .map(entry -> entry.split("="))
                .collect(Collectors.toMap(cookieArr -> cookieArr[0], cookieArr -> cookieArr[1]));
    }

    public Map<String, String> getCookies() {
        return getCookies("Set-Cookie");
    }
}
