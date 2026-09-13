package com.petchat.api.objects.serverdto;

public class ChatPropertiesDto {
    public int id;
    public String chatId;
    public int userId;
    public int peerId;
    public UserDto peer;
    public boolean notificationsEnabled;
    public boolean archived;
    public boolean pinned;
}
