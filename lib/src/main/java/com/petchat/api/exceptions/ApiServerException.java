package com.petchat.api.exceptions;

public class ApiServerException extends ApiException {
    public ApiServerException(String message) {
        super(message);
    }
    public ApiServerException(int statusCode, String content){
        super(statusCode, content);
    }
}
