package com.petchat.api.objects.serverdto;

import java.time.Instant;
import java.util.ArrayList;

public class ChatDto {
    public String id;
    public Integer messageIndex;
    public ArrayList<Integer> members;
    public MessageDto lastMessage;
    public ChatPropertiesDto chatProperties;
    public String imageUrl;
    public String chatName;

    public String lastActivityAt;
    public Instant getLastActivityAtInstant() {
        return Instant.parse(lastActivityAt);
    }
}
