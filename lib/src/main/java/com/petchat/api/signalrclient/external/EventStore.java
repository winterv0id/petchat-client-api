package com.petchat.api.signalrclient.external;

import com.petchat.api.objects.events.UserEvent;
import com.petchat.api.objects.response.sync.UserSnapshot;

import java.util.concurrent.CompletableFuture;

public interface EventStore {
    CompletableFuture<Void> saveAppliedEvent(UserEvent evt);
    CompletableFuture<Long> getLastAppliedSequence();
    CompletableFuture<Void> applySnapshot(UserSnapshot snapshot, long sequence);
}