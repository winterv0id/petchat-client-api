package com.petchat.api.objects.events.payloads;

import java.time.Instant;

public class UserOnlineStatusPayload {
    public int userId;
    public String status;

    public String at;
    public Instant getAtInstant() {
        return Instant.parse(at);
    }
}
