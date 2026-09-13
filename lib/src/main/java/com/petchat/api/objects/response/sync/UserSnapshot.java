package com.petchat.api.objects.response.sync;

import com.petchat.api.objects.serverdto.ChatDto;
import com.petchat.api.objects.serverdto.MessageDto;

public class UserSnapshot {
    public ChatDto[] chats;
    public MessageDto[] messages;
    //....
}
