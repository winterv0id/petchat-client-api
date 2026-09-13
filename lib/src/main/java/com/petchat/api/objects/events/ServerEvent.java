package com.petchat.api.objects.events;

import io.reactivex.rxjava3.annotations.Nullable;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

public class ServerEvent {
    public ServerEvent(String eventType, String message) {
        this.eventType = eventType;
        this.message = message;
    }
    public String eventType;
    public String message;
    public @Nullable JsonNode additionalPayload = null;

    private static final ObjectMapper MAPPER = new ObjectMapper();
}
