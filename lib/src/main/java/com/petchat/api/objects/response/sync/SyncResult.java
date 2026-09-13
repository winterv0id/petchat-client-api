package com.petchat.api.objects.response.sync;

import com.petchat.api.objects.events.UserEvent;

import java.util.List;

public class SyncResult {
    public boolean isFullSnapshot;
    public List<UserEvent> events; // not null if isFullSnapshot == false
    public boolean hasMore;
    public UserSnapshot snapshot; // not null if isFullSnapshot == true
    public long sequence;
}
