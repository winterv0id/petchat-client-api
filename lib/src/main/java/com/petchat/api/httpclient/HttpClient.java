package com.petchat.api.httpclient;

import com.google.gson.JsonObject;
import com.petchat.api.apiclient.Response;

import java.io.File;
import java.io.IOException;
import java.util.Map;

public interface HttpClient {

    Response post(String url, JsonObject jsonObject) throws IOException;
    Response post(String url, String body) throws IOException;
    Response post(String url, Map<String, File> files) throws IOException;
    Response post(String url, String body, String contentType) throws IOException;

    Response get(String url) throws IOException;
    Response get(String url, String parameters) throws IOException;
    Response get(String url, String parameters, String contentType) throws IOException;

    Response delete(String url) throws IOException;
    Response delete(String url, String body) throws IOException;
    Response delete(String url, String body, String contentType) throws IOException;

    void downloadFile(String url, File file) throws IOException;
    void downloadFile(String url, File outputFile, ProgressListener listener) throws IOException;
}
