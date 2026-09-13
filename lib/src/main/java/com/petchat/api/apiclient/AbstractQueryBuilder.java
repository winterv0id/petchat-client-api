package com.petchat.api.apiclient;

import com.google.gson.JsonArray;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.petchat.api.objects.base.EnumParam;
import org.apache.hc.core5.http.Header;

import java.lang.reflect.Type;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static java.util.Arrays.asList;

public abstract class AbstractQueryBuilder<T, R> extends ApiRequest<R> {
    private final Map<String, Object> params = new HashMap<>();
    private final List<Header> headers = new ArrayList<>();
    private String method;

    public AbstractQueryBuilder(PetChatApiClient client, String method, Type type, HttpMethod httpMethod) {
        super(client.getApiEndpoint() + method, client.getTransportClient(), type, httpMethod);
        this.method = method;
    }

    public AbstractQueryBuilder(PetChatApiClient client, String endpoint, String method, Type type, HttpMethod httpMethod) {
        super(endpoint + method, client.getTransportClient(), type, httpMethod);
    }

    private static String mapToGetString(Map<String, Object> params) {
        return params.entrySet().stream()
                .map(entry -> entry.getKey() + "=" + (entry.getValue() != null ? escape(paramToString(entry.getValue())) : ""))
                .collect(Collectors.joining("&"));
    }

    private static String paramToString(Object value) {
        if (value instanceof Collection<?> coll) {
            return coll.stream().map(Objects::toString).collect(Collectors.joining(","));
        }
        if (value instanceof int[] arr) {
            return IntStream.of(arr).mapToObj(Integer::toString).collect(Collectors.joining(","));
        }
        return value.toString();
    }

    private static String escape(String data) {
        return URLEncoder.encode(data, StandardCharsets.UTF_8);
    }

    public T queryParam(String key, Object value) {
        if (value != null)
            params.put(key, value);

        return getThis();
    }

    @SafeVarargs
    public final <U> T queryParam(String key, U... value) {
        return queryParam(key, asList(value));
    }
    
    public T queryParam(String key, EnumParam<T> value) {
        return queryParam(key, value.getValue());
    }
    @SafeVarargs
    public final T queryParam(String key, EnumParam<T>... fields) {
        return queryParam(key, Arrays.stream(fields).map(EnumParam::toString).collect(Collectors.joining(",")));
    }
    public T queryParam(String key, List<? extends EnumParam<T>> fields) {
        return queryParam(key, fields.stream().map(EnumParam::toString).collect(Collectors.joining(",")));
    }

    @Override
    protected JsonObject getJsonObject() {
        JsonObject json = new JsonObject();
        for (Map.Entry<String, Object> entry : build().entrySet()) {
            addToJson(json, entry.getKey(), entry.getValue());
        }
        return json;
    }

    private static void addToJson(JsonObject json, String key, Object value) {
        switch (value) {
            case null -> json.add(key, JsonNull.INSTANCE);
            case String s -> json.addProperty(key, s);
            case Boolean b -> json.addProperty(key, b);
            case Number n -> json.addProperty(key, n);
            case int[] arr -> {
                JsonArray jsonArray = new JsonArray();
                for (int i : arr) jsonArray.add(i);
                json.add(key, jsonArray);
            }
            case Collection<?> coll -> {
                JsonArray jsonArray = new JsonArray();
                for (Object item : coll) {
                    if (item instanceof Number n) jsonArray.add(n);
                    else if (item instanceof Boolean b) jsonArray.add(b);
                    else jsonArray.add(Objects.toString(item));
                }
                json.add(key, jsonArray);
            }
            default -> json.addProperty(key, value.toString());
        }
    }

    @Override
    protected String getQueryString() {
        return mapToGetString(build());
    }

    protected abstract T getThis();
    protected abstract Collection<String> essentialKeys();

    public Map<String, Object> build() {
        if (!params.keySet().containsAll(essentialKeys())) {
            throw new IllegalArgumentException("Not all the keys are passed: essential keys are " + essentialKeys());
        }
        return Collections.unmodifiableMap(params);
    }

    public String getMethod() { return method; }
    public Map<String, Object> getParams() { return params; }
    public String toString() { return this.getMethod(); }
}
