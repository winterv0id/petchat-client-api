package com.petchat.api.exceptions;

public class ApiException extends Exception {
    public int statusCode;
    public String content;

    public ApiException(String message) {
        super(message);
    }
    public ApiException(int statusCode, String content){
        super(String.format("API error: (%d) %s", statusCode, content));
        this.statusCode = statusCode;
        this.content = content;
    }
}
