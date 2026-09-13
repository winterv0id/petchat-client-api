package com.petchat.api.signalrclient.synchronization;

import com.microsoft.signalr.HubConnection;
import com.microsoft.signalr.HubConnectionState;
import com.petchat.api.objects.events.UserEvent;
import com.petchat.api.objects.response.sync.SyncResult;
import com.petchat.api.objects.response.sync.UserSnapshot;
import com.petchat.api.signalrclient.BackoffRetry;
import com.petchat.api.signalrclient.external.EventStore;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.SubmissionPublisher;

public class Synchronizator {
    public Synchronizator(HubConnection hubConnection, EventStore eventStore,
                          SubmissionPublisher<UserEvent> eventPublisher,
                          CompositeDisposable disposables) {
        this.hubConnection = hubConnection;
        this.eventStore = eventStore;
        this.userEventPublisher = eventPublisher;
        this.disposables = disposables;
    }

    private static final Logger LOGGER = LoggerFactory.getLogger(Synchronizator.class);
    private final BackoffRetry syncBackoff = new BackoffRetry();
    private final SubmissionPublisher<UserEvent> userEventPublisher;
    private final CompositeDisposable disposables;

    private volatile HubConnection hubConnection;
    private final EventStore eventStore;

    private SyncState syncState;
    public SyncState getSyncState() { return syncState; }

    private volatile long lastAckedSequence = 0;
    private volatile boolean isDisconnected = false;

    public void init() throws ExecutionException, InterruptedException {
        syncState = new SyncState(eventStore.getLastAppliedSequence().get());
    }

    public void startSync() {
        syncState.resyncing = true;
        var disposable = hubConnection
                .invoke(SyncResult.class, "Sync", syncState.appliedSequence)
                .subscribe(this::handleSyncResult, this::handleSyncError);
        disposables.add(disposable);
    }

    public void rebindConnection(HubConnection newConnection) {
        this.hubConnection = newConnection;
    }

    public void shutdown() {
        isDisconnected = true;
        syncBackoff.shutdown();
    }

    public void handleEvent(UserEvent evt) {
        if (syncState.resyncing) {
            syncState.pendingBuffer.add(evt);
        } else {
            applyAndPersist(evt);
        }
    }

    private void handleSyncResult(SyncResult result) {
        if (isDisconnected) return;

        if (result.isFullSnapshot) {
            applyFullSnapshot(result.snapshot, result.sequence);
            finishResync();
        } else {
            for (UserEvent evt : result.events) applyAndPersist(evt);

            if (result.hasMore) {
                var disposable = hubConnection
                        .invoke(SyncResult.class, "Sync", syncState.appliedSequence)
                        .subscribe(this::handleSyncResult, this::handleSyncError);
                disposables.add(disposable);
            } else {
                finishResync();
            }
        }
    }

    private void finishResync() {
        UserEvent buffered;
        while ((buffered = syncState.pendingBuffer.poll()) != null) applyAndPersist(buffered);

        syncState.resyncing = false;
        syncBackoff.reset();
        sendAckIfChanged();
    }

    private void handleSyncError(Throwable error) {
        // syncState.resyncing остаётся true
        LOGGER.error("sync error: {}", error.getMessage());
        syncBackoff.schedule(() -> {
            if (hubConnection.getConnectionState() == HubConnectionState.CONNECTED) {
                startSync();
            } else {
                LOGGER.warn("scheduled sync impossible - no hub server connection.");
            }
        });
    }

    private void applyAndPersist(UserEvent evt) {
        if (evt.sequence != -1) {
            if (evt.sequence <= syncState.appliedSequence) return;

            eventStore.saveAppliedEvent(evt).thenRun(() -> {
                syncState.appliedSequence = evt.sequence;
            }).exceptionally(ex -> {
                LOGGER.error("Error when saving applied event - sequence not saved!", ex);
                return null;
            });
        }

        // разослать наблюдателям
        userEventPublisher.submit(evt);
    }

    private void applyFullSnapshot(UserSnapshot snapshot, long sequence) {
        eventStore.applySnapshot(snapshot, sequence).thenRun(() ->
                syncState.appliedSequence = sequence);
    }

    public void sendAckIfChanged() {
       eventStore.getLastAppliedSequence().thenAccept(sequence -> {
           if (sequence > lastAckedSequence) {
               hubConnection.send("Ack", sequence);
               lastAckedSequence = sequence;
           }
       });
    }
}