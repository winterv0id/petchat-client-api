package com.petchat.api.exceptions;

public class ApiClientException extends ApiException {
    public ApiClientException(String message) {
        super(message);
    }
    public ApiClientException(int statusCode, String content){
        super(statusCode, content);
    }
}
