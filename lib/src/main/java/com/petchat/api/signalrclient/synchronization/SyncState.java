package com.petchat.api.signalrclient.synchronization;

import com.petchat.api.objects.events.UserEvent;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

public class SyncState {
    public SyncState(long initialSequence) {
        appliedSequence = initialSequence;
    }
    // true - live-события не применяются сразу, а копятся в pendingBuffer, пока идёт синхронизация
    volatile boolean resyncing = false;

    final Queue<UserEvent> pendingBuffer = new ConcurrentLinkedQueue<>();

    // копия LocalEventStore.getLastAppliedSequence() в памяти
    volatile long appliedSequence;
}
