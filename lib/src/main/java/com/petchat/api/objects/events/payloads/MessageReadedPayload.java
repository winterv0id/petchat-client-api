package com.petchat.api.objects.events.payloads;

import java.time.Instant;

public class MessageReadedPayload {
    public String chatId;
    public int upToMessageId;

    public String readDate;
    public Instant getReadDateInstant() {
        return Instant.parse(readDate);
    }
}
