package com.petchat.api.signalrclient.synchronization;

import com.microsoft.signalr.HubConnection;
import com.microsoft.signalr.HubConnectionState;
import com.petchat.api.objects.events.EventType;
import com.petchat.api.objects.events.UserEvent;
import com.petchat.api.objects.response.sync.SyncResult;
import com.petchat.api.objects.response.sync.UserSnapshot;
import com.petchat.api.signalrclient.external.EventStore;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.core.SingleEmitter;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * {@link Synchronizator} - ядро клиентской синхронизации, отвечает за:
 * запрос синхронизации, обработку ответа, сбор live-events во время главной синхронизации
 * и применения их после окончания, отправку ACK после локального сохранения ({@link EventStore}),
 * рассылку live-events всем наблюдателям (SubmissionPublisher<(UserEvent)> userEventPublisher).
 *
 * <p>{@link HubConnection} - мокается через Mockito.{@link EventStore} подменяется
 * {@link FakeEventStore} - простой in-memory реализацией; завершенные {@link CompletableFuture}
 * выполняют {@code thenRun}/{@code thenAccept} синхронно - избавляет от ожидания.
 */
@ExtendWith(MockitoExtension.class)
class SynchronizatorTest {

    @Mock
    private HubConnection hubConnection;

    private CompositeDisposable disposables;
    private FakeEventStore eventStore;
    private SubmissionPublisher<UserEvent> publisher;
    private CollectingSubscriber subscriber;
    private Synchronizator sut;

    @BeforeEach
    void setup() {
        disposables = new CompositeDisposable();
        eventStore = new FakeEventStore(0);
        publisher = new SubmissionPublisher<>();
        subscriber = new CollectingSubscriber();
        publisher.subscribe(subscriber);
        sut = new Synchronizator(hubConnection, eventStore, publisher, disposables);
    }

    // region init
    @Test
    void init_ReadsInitialAppliedSequence_FromEventStore() throws Exception {
        eventStore.lastAppliedSequence = 42;

        sut.init();

        assertEquals(42, sut.getSyncState().appliedSequence);
    }
    // endregion

    // region startSync
    @Test
    void startSync_SetsResyncingTrue_AndInvokesSyncWithCurrentAppliedSequence() throws Exception {
        eventStore.lastAppliedSequence = 100;
        sut.init();
        when(hubConnection.invoke(eq(SyncResult.class), eq("Sync"), any()))
                .thenReturn(Single.never());

        sut.startSync();

        assertTrue(sut.getSyncState().resyncing);
        verify(hubConnection).invoke(eq(SyncResult.class), eq("Sync"), eq(100L));
    }

    @Test
    void startSync_FullSnapshotResult_AppliesSnapshot_AndFinishResync() throws Exception {
        sut.init();
        var snapshot = new UserSnapshot();
        var result = fullSnapshotResult(snapshot, 500);

        when(hubConnection.invoke(eq(SyncResult.class), eq("Sync"), any()))
                .thenReturn(Single.just(result));

        sut.startSync();

        assertSame(snapshot, eventStore.appliedSnapshot);
        assertEquals(500, eventStore.appliedSnapshotSequence);
        assertEquals(500, sut.getSyncState().appliedSequence);
        assertFalse(
                sut.getSyncState().resyncing,
                "после применения снимка syncState.resyncing должен стать false"
        );
    }

    @Test
    void startSync_IncrementalResult_NoMore_AppliesEvents_AndFinishResync() throws Exception {
        sut.init();

        var eventA = makeEvent(101, EventType.NEW_MESSAGE);
        var eventB = makeEvent(102, EventType.NEW_MESSAGE);

        var result = incrementalResult(List.of(eventA, eventB), false, 102);
        when(hubConnection.invoke(eq(SyncResult.class), eq("Sync"), any()))
                .thenReturn(Single.just(result));

        sut.startSync();

        assertEquals(List.of(eventA, eventB), eventStore.savedEvents);
        assertEquals(102, sut.getSyncState().appliedSequence);
        assertFalse(sut.getSyncState().resyncing);
    }

    @Test
    void startSync_IncrementalResult_HasMore_RequestsNextPage_UntilNoMore() throws Exception {
        sut.init();

        var page1Event = makeEvent(101, EventType.NEW_MESSAGE);
        var page1 = incrementalResult(List.of(page1Event), true, 101);
        var page2Event = makeEvent(202, EventType.NEW_MESSAGE);
        var page2 = incrementalResult(List.of(page2Event), false, 202);

        when(hubConnection.invoke(eq(SyncResult.class), eq("Sync"), any()))
                .thenReturn(Single.just(page1), Single.just(page2));

        sut.startSync();

        verify(hubConnection, times(2)).invoke(eq(SyncResult.class), eq("Sync"), any());
        assertEquals(List.of(page1Event, page2Event), eventStore.savedEvents);
        assertEquals(202, sut.getSyncState().appliedSequence);
        assertFalse(sut.getSyncState().resyncing);
    }

    @Test
    void finishResync_ApplyAllBufferedEvents_InOriginalOrder() throws Exception {
        sut.init();
        sut.getSyncState().resyncing = true;

        Queue<UserEvent> buffer = sut.getSyncState().pendingBuffer;
        var bufferedA = makeEvent(301, EventType.NEW_MESSAGE);
        var bufferedB = makeEvent(302, EventType.NEW_MESSAGE);
        buffer.add(bufferedA);
        buffer.add(bufferedB);

        var emptyResult = incrementalResult(List.of(), false, 300);
        when(hubConnection.invoke(eq(SyncResult.class), eq("Sync"), any()))
                .thenReturn(Single.just(emptyResult));

        sut.startSync();

        assertEquals(List.of(bufferedA, bufferedB), eventStore.savedEvents);
        assertEquals(302, sut.getSyncState().appliedSequence);
        assertFalse(sut.getSyncState().resyncing);
        assertTrue(sut.getSyncState().pendingBuffer.isEmpty());
    }
    // endregion

    // region handleEvent
    @Test
    void handleEvent_WhileResyncing_BuffersInsteadOfApplyingImmediately() throws Exception {
        sut.init();

        sut.getSyncState().resyncing = true;
        var evt = makeEvent(999, EventType.NEW_MESSAGE);

        sut.handleEvent(evt);

        assertTrue(
                eventStore.savedEvents.isEmpty(),
                "во время синхронизации событие не должно применяться сразу (конфликт)"
        );
        assertEquals(evt, sut.getSyncState().pendingBuffer.peek());
    }

    @Test
    void handleEvent_NotResyncing_AppliesImmediately() throws Exception {
        sut.init();
        var evt = makeEvent(1, EventType.NEW_MESSAGE);

        sut.handleEvent(evt);

        assertEquals(List.of(evt), eventStore.savedEvents);
    }

    @Test
    void handleEvent_AlreadyAppliedSequence_NotSavedOrPublished() throws Exception {
        eventStore.lastAppliedSequence = 500;
        sut.init();

        // sequence == appliedSequence
        var oldEvent = makeEvent(500, EventType.NEW_MESSAGE);

        sut.handleEvent(oldEvent);

        assertTrue(
                eventStore.savedEvents.isEmpty(),
                "уже примененное событие не должно сохраняться повторно"
        );
        assertNull(
                subscriber.received.poll(300, TimeUnit.MILLISECONDS),
                "уже примененное событие не должно доходить до наблюдателей UI"
        );
    }

    @Test
    void handleEvent_SequenceGreaterThanApplied_IsApplied() throws Exception {
        eventStore.lastAppliedSequence = 500;
        sut.init();

        var newEvent = makeEvent(501, EventType.NEW_MESSAGE);
        sut.handleEvent(newEvent);

        assertEquals(List.of(newEvent), eventStore.savedEvents);
    }

    @Test
    void handleEvent_Event_SequenceMinusOne_NotSave_NotRegressAppliedSequence_ButIsPublished() throws Exception {
        // Эфемерное событие должно дойти до наблюдателей, но не должно
        // попасть в eventStore и не должно откатить appliedSequence в значение -1.
        eventStore.lastAppliedSequence = 42;
        sut.init();

        var typingEvent = makeEvent(-1, EventType.USER_TYPING);
        sut.handleEvent(typingEvent);

        assertTrue(
                eventStore.savedEvents.isEmpty(),
                "эфемерное событие не должно попадать в локальное хранилище"
        );
        assertEquals(
                42,
                sut.getSyncState().appliedSequence,
                "эфемерное событие не должно откатывать позицию синхронизации"
        );

        var delivered = subscriber.received.poll(2, TimeUnit.SECONDS);
        assertNotNull(delivered, "typingEvent не был отправлен наблюдателям");
        assertEquals(-1, delivered.sequence);
    }
    // endregion

    // region sendAckIfChanged
    @Test
    void sendAckIfChanged_NewSequenceGreaterThanLastAcked_SendsAck() throws Exception {
        eventStore.lastAppliedSequence = 555;
        sut.init();

        //lastAckedSequence = 0
        sut.sendAckIfChanged();

        verify(hubConnection).send(eq("Ack"), eq(555L));
    }

    @Test
    void sendAckIfChanged_CalledTwiceWithSameValue_SendsAckOnlyOnce() throws Exception {
        eventStore.lastAppliedSequence = 555;
        sut.init();

        sut.sendAckIfChanged();
        sut.sendAckIfChanged();

        verify(hubConnection, times(1)).send(eq("Ack"), eq(555L));
    }
    // endregion

    // region ошибки/реконнект
    @Test
    void handleSyncError_HubConnected_RetriesSync() throws Exception {
        sut.init();

        when(hubConnection.getConnectionState())
                .thenReturn(HubConnectionState.CONNECTED);

        when(hubConnection.invoke(eq(SyncResult.class), eq("Sync"), any()))
                .thenReturn(Single.error(new RuntimeException("неизвестная ошибка")));

        sut.startSync();

        // BackoffRetry ждет 1с перед первой повторной попыткой
        verify(hubConnection, timeout(2000)
                .times(2))
                .invoke(eq(SyncResult.class), eq("Sync"), any());
    }

    @Test
    void handleSyncError_ConnectionNotConnected_NotRetrySync() throws Exception {
        sut.init();

        when(hubConnection.getConnectionState())
                .thenReturn(HubConnectionState.DISCONNECTED);

        when(hubConnection.invoke(eq(SyncResult.class), eq("Sync"), any()))
                .thenReturn(Single.error(new RuntimeException("неизвестная ошибка")));

        sut.startSync();

        verify(hubConnection, after(1500)
                .times(1))
                .invoke(eq(SyncResult.class), eq("Sync"), any());
    }
    // endregion

    // region shutdown
    @Test
    void shutdown_ResultAfterShutdown_IsIgnored() throws Exception {
        sut.init();

        SingleEmitter<SyncResult>[] captured = new SingleEmitter[1];
        Single<SyncResult> controllable = Single.create(emitter -> captured[0] = emitter);
        when(hubConnection.invoke(eq(SyncResult.class), eq("Sync"), any()))
                .thenReturn(controllable);

        sut.startSync();
        sut.shutdown();  //клиент завершает работу до прихода ответа

        // здесь ответ пришел
        captured[0].onSuccess(fullSnapshotResult(new UserSnapshot(), 999));

        assertNull(
                eventStore.appliedSnapshot,
                "результат, пришедший после shutdown, не должен применяться"
        );
    }
    // endregion

    // rebind connection
    @Test
    void rebindConnection_NextStartSync_UsesNewConnection() throws Exception {
        // HubClient пересобирает HubConnection при каждом реконнекте и вызывает
        // rebindConnection — Synchronizator после этого должен работать через новое соединение.
        sut.init();
        HubConnection newConnection = mock(HubConnection.class);
        when(newConnection.invoke(eq(SyncResult.class), eq("Sync"), any()))
                .thenReturn(Single.never());

        sut.rebindConnection(newConnection);
        sut.startSync();

        verify(newConnection).invoke(eq(SyncResult.class), eq("Sync"), any());
        verify(hubConnection, never()).invoke(eq(SyncResult.class), eq("Sync"), any());
    }

    @Test
    void rebindConnection_SendAckIfChanged_UsesNewConnection() throws Exception {
        eventStore.lastAppliedSequence = 888;
        sut.init();
        HubConnection newConnection = mock(HubConnection.class);

        sut.rebindConnection(newConnection);
        sut.sendAckIfChanged();

        verify(newConnection).send(eq("Ack"), eq(888L));
        verify(hubConnection, never()).send(eq("Ack"), any());
    }
    // endregion

    // region helpers
    private static UserEvent makeEvent(long sequence, String eventType) {
        var evt = new UserEvent();
        evt.sequence = sequence;
        evt.userId = "1";
        evt.eventType = eventType;
        evt.payloadJson = "{}";
        return evt;
    }

    private static SyncResult incrementalResult(List<UserEvent> events, boolean hasMore, long sequence) {
        var syncResult = new SyncResult();
        syncResult.isFullSnapshot = false;
        syncResult.events = new ArrayList<>(events);
        syncResult.hasMore = hasMore;
        syncResult.sequence = sequence;
        return syncResult;
    }

    private static SyncResult fullSnapshotResult(UserSnapshot snapshot, long sequence) {
        var syncResult = new SyncResult();
        syncResult.isFullSnapshot = true;
        syncResult.snapshot = snapshot;
        syncResult.sequence = sequence;
        return syncResult;
    }

    /** in-memory impl {@link EventStore} */
    private static class FakeEventStore implements EventStore {
        final List<UserEvent> savedEvents = new ArrayList<>();
        volatile long lastAppliedSequence;
        volatile UserSnapshot appliedSnapshot;
        volatile long appliedSnapshotSequence = -1;

        FakeEventStore(long initialSequence) {
            this.lastAppliedSequence = initialSequence;
        }

        @Override
        public CompletableFuture<Void> saveAppliedEvent(UserEvent evt) {
            savedEvents.add(evt);
            return CompletableFuture.completedFuture(null);
        }

        @Override
        public CompletableFuture<Long> getLastAppliedSequence() {
            return CompletableFuture.completedFuture(lastAppliedSequence);
        }

        @Override
        public CompletableFuture<Void> applySnapshot(UserSnapshot snapshot, long sequence) {
            this.appliedSnapshot = snapshot;
            this.appliedSnapshotSequence = sequence;
            return CompletableFuture.completedFuture(null);
        }
    }

    /** Собирает всё, что было отправлено наблюдателям {@code SubmissionPublisher} */
    private static class CollectingSubscriber implements Flow.Subscriber<UserEvent> {
        final BlockingQueue<UserEvent> received = new LinkedBlockingQueue<>();

        @Override
        public void onSubscribe(Flow.Subscription subscription) {
            subscription.request(Long.MAX_VALUE);
        }
        @Override
        public void onNext(UserEvent item) {
            received.add(item);
        }
        @Override
        public void onError(Throwable throwable) { }
        @Override
        public void onComplete() { }
    }
    // endregion
}