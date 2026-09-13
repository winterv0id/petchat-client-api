package com.petchat.api.objects.events.payloads;

import java.time.Instant;

public class MessageEditedPayload {
    public int messageId;
    public String newText;

    public String editDate;
    public Instant getEditDateInstant() {
        return Instant.parse(editDate);
    }
}
