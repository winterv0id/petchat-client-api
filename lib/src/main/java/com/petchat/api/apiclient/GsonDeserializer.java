package com.petchat.api.apiclient;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.lang.reflect.Type;

public class GsonDeserializer {
    private static final Gson mapper = new GsonBuilder()
            .setPrettyPrinting()
            .create();

    public static <T> T deserialize(String json, Type modelType) {
        return mapper.fromJson(json, modelType);
    }
}
