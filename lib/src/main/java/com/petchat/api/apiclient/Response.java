package com.petchat.api.apiclient;

import java.util.Map;

public class Response extends ResponseTypeable<String> {
    public Response(int statusCode, String content, Map<String, String> headers) {
        super(statusCode, content, headers);
    }
}
