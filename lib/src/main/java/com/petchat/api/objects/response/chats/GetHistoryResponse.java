package com.petchat.api.objects.response.chats;

import com.petchat.api.objects.serverdto.MessageDto;

import java.util.List;

public class GetHistoryResponse {
    public List<MessageDto> messages;
    public int length;
    public boolean hasMore;
}
