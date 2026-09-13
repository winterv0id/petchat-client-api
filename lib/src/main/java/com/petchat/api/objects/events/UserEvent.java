package com.petchat.api.objects.events;

import com.petchat.api.objects.events.payloads.*;
import com.petchat.api.objects.serverdto.ChatDto;
import com.petchat.api.objects.serverdto.MessageDto;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

public class UserEvent {
    public long sequence;
    public String userId;
    public String chatId;
    public String eventType;
    public String payloadJson;
    public String createdAt;

    private static final ObjectMapper MAPPER = new ObjectMapper();

    public MessageDto asMessage() {
        return parse(MessageDto.class);
    }

    public ChatDto asChat() {
        return parse(ChatDto.class);
    }

    public MessageEditedPayload asMessageEdited() {
        return parse(MessageEditedPayload.class);
    }

    public MessageDeletedPayload asMessageDeleted() {
        return parse(MessageDeletedPayload.class);
    }

    public MessageReadedPayload asMessageReaded() {
        return parse(MessageReadedPayload.class);
    }

    public UserTypingPayload asUserTypingPayload() {
        return parse(UserTypingPayload.class);
    }

    public UserOnlineStatusPayload asUserOnlineStatusPayload() {
        return parse(UserOnlineStatusPayload.class);
    }

    private <T> T parse(Class<T> type) {
        try {
            return MAPPER.readValue(payloadJson, type);
        } catch (JacksonException e) {
            throw new RuntimeException("Не удалось распарсить payload как " + type.getSimpleName(), e);
        }
    }
}
