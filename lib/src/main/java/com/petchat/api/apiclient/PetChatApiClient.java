package com.petchat.api.apiclient;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.petchat.api.actions.*;
import com.petchat.api.exceptions.ApiException;
import com.petchat.api.exceptions.NotAuthorizedException;
import com.petchat.api.httpclient.ApacheHttpClient;
import com.petchat.api.httpclient.HttpClient;
import com.petchat.api.objects.response.auth.AuthResponse;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

public class PetChatApiClient {
    private final String apiAddress;
    private final ApacheHttpClient transportClient;
    private final Gson gson;

    public PetChatApiClient(String apiAddress, String userAgent, String clientKey, String deviceId) {
        this.apiAddress = apiAddress + "/";
        this.transportClient = new ApacheHttpClient(userAgent, clientKey, deviceId);
        this.gson = new Gson();
    }

    public String getApiEndpoint() { return apiAddress; }
    public Gson getGson() { return this.gson;}
    public HttpClient getTransportClient() { return this.transportClient; }

    public CompletableFuture<AuthResponse> authorize(String login, String password, Boolean refresh) {
        return creditionals().auth()
                .login(login)
                .password(password)
                .refresh(refresh)
                .executeAsClientResponse().thenApplyAsync(response -> {
                    JsonObject jsonObject = JsonParser.parseString(response.getContent()).getAsJsonObject();
                    var authResponse = gson.fromJson(jsonObject, AuthResponse.class);

                    transportClient.addDefaultHeader("Authorization", "Bearer " + authResponse.accessToken);
                    return authResponse;
                });
    }

    public CompletableFuture<Void> authorize(String accessToken) {
        return CompletableFuture.runAsync(() -> {
            transportClient.addDefaultHeader("Authorization", "Bearer " + accessToken);

            Response response = null;
            try {
                response = transportClient.get(apiAddress + "auth/check");
            } catch (IOException e) {
                throw new CompletionException(e);
            }

            switch (response.getStatusCode()) {
                case 200: break;
                case 401:  {
                    transportClient.removeDefaultHeader("Authorization");
                    throw new CompletionException(new NotAuthorizedException(response.getContent()));
                }
                default: throw new CompletionException(new ApiException(response.getStatusCode(), response.getContent()));
            }
        });
    }

    public CompletableFuture<AuthResponse> register(String login, String password, String nickname) {
        return creditionals().register()
                .login(login)
                .password(password)
                .nickname(nickname)
                .executeAsClientResponse().thenApplyAsync(response -> {
                    JsonObject jsonObject = JsonParser.parseString(response.getContent()).getAsJsonObject();
                    var authResponse = gson.fromJson(jsonObject, AuthResponse.class);
                    transportClient.addDefaultHeader("Authorization", "Bearer " + authResponse.accessToken);
                    return authResponse;
                });
    }

    //region actions
    public Sync sync() { return new Sync(this); }
    public Fcm fcm() { return new Fcm(this); }
    public Users users() { return new Users(this); }
    public Chats chats() { return new Chats(this); }
    private Creditionals creditionals() { return new Creditionals(this); }
    //endregion
}
