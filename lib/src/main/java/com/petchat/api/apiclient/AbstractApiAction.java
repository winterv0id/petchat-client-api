package com.petchat.api.apiclient;

public abstract class AbstractApiAction {

    private final PetChatApiClient vkApiClient;

    public AbstractApiAction(PetChatApiClient vkApiClient) {
        this.vkApiClient = vkApiClient;
    }

    protected PetChatApiClient getClient() {
        return vkApiClient;
    }
}
