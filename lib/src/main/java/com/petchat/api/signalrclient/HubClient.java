package com.petchat.api.signalrclient;

import com.microsoft.signalr.HubConnection;
import com.microsoft.signalr.HubConnectionBuilder;
import com.petchat.api.objects.events.ServerEvent;
import com.petchat.api.objects.events.UserEvent;
import com.petchat.api.objects.serverdto.SendMessageResultDto;
import com.petchat.api.signalrclient.external.EventStore;
import com.petchat.api.signalrclient.synchronization.Synchronizator;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.*;
import java.util.function.Supplier;

public class HubClient {
    private static final String HUB_ENDPOINT = "/hubs/chat";
    private static final Logger LOGGER = LoggerFactory.getLogger(HubClient.class);

    public static final SubmissionPublisher<ServerEvent> ServerEventPublisher = new SubmissionPublisher<>();
    public static final SubmissionPublisher<UserEvent> UserEventPublisher = new SubmissionPublisher<>();

    private final BackoffRetry connectionBackoff = new BackoffRetry();
    private final CompositeDisposable disposables = new CompositeDisposable();

    private final String serverAddr;
    private final Supplier<String> authTokenSupplier;

    protected final Synchronizator synchronizator;
    protected volatile HubConnection hubConnection;
    protected final ScheduledExecutorService ackSheduler = Executors.newSingleThreadScheduledExecutor();

    public HubClient(String serverAddr, Supplier<String> authTokenSupplier, EventStore eventStore) {
        this.serverAddr = serverAddr;
        this.authTokenSupplier = authTokenSupplier;

        this.hubConnection = buildConnection();

        hubConnection.on("Event", this::onLiveEvent, UserEvent.class);
        hubConnection.onClosed(this::scheduleReconnect);

        this.synchronizator = new Synchronizator(hubConnection, eventStore, UserEventPublisher, disposables);

        ackSheduler.scheduleAtFixedRate(synchronizator::sendAckIfChanged, 5, 5, TimeUnit.SECONDS);
    }

    private HubConnection buildConnection() {
        return HubConnectionBuilder
                .create(serverAddr + HUB_ENDPOINT)
                .withHeader("Authorization", "Bearer " + authTokenSupplier.get())
                .build();
    }

    private void registerHandlers(HubConnection connection) {
        connection.on("Event", this::onLiveEvent, UserEvent.class);
        connection.onClosed(this::scheduleReconnect);
    }

    public void connect() throws ExecutionException, InterruptedException {
        synchronizator.init();

        var disposable = this.hubConnection.start().subscribe(
                this.synchronizator::startSync,
                error -> {
                    LOGGER.error("Hub connection error: {}", error.getMessage());
                });
        disposables.add(disposable);
    }

    public void disconnect() {
        if (hubConnection != null) {
            hubConnection.stop();
        }
        connectionBackoff.shutdown();
        synchronizator.shutdown();
        disposables.dispose();
    }

    private void onLiveEvent(UserEvent evt) {
        synchronizator.handleEvent(evt);
    }

    //region Actions
    public void typing(Integer peerId) {
        hubConnection.send("UserTyping", peerId);
    }
    public void stopTyping(Integer peerId) {
        hubConnection.send("UserStoppedTyping", peerId);
    }
    //endregion

    //region Requests
    public CompletableFuture<SendMessageResultDto> sendMessage(int peerId, String text) {
        return toFuture(hubConnection.invoke(SendMessageResultDto.class, "SendMessage", peerId, text));
    }

    public CompletableFuture<Boolean> markMessagesReadUpTo(int upToMessageId) {
        return toFuture(hubConnection.invoke(Boolean.class, "MarkMessagesReadUpTo", upToMessageId));
    }

    public CompletableFuture<Boolean> editMessage(int messageId, String newText) {
        return toFuture(hubConnection.invoke(Boolean.class, "EditMessage", messageId, newText));
    }

    public CompletableFuture<Boolean> deleteMessage(int messageId) {
        return toFuture(hubConnection.invoke(Boolean.class, "DeleteMessage", messageId));
    }
    //endregion

    private void scheduleReconnect(Exception ex) {
        if (ex == null) return; // hubConnection.stop();
        LOGGER.error("SignalR HubClient connection aborted by reason: {}", ex.getMessage());

        connectionBackoff.schedule(() -> {
            HubConnection fresh = buildConnection();
            registerHandlers(fresh);
            this.hubConnection = fresh;
            synchronizator.rebindConnection(fresh);

            try {
                fresh.start().blockingAwait();
                connectionBackoff.reset();
                synchronizator.startSync();
            } catch (Exception e) {
                scheduleReconnect(e);
            }
        });
    }

    private <T> CompletableFuture<T> toFuture(Single<T> single) {
        CompletableFuture<T> future = new CompletableFuture<>();
        var disposable = single.subscribe(future::complete, future::completeExceptionally);
        disposables.add(disposable);
        return future;
    }
}
