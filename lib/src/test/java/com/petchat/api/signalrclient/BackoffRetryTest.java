package com.petchat.api.signalrclient;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**{@link BackoffRetry} - задержка между попытками переподключения (1с, 2с, 5с, 10с, 30с)*/
class BackoffRetryTest {

    private final BackoffRetry sut = new BackoffRetry();

    @AfterEach
    void shutdown() {
        sut.shutdown();
    }

    @Test
    void schedule_RunsActionAfterDelay() throws InterruptedException {
        var latch = new CountDownLatch(1);

        sut.schedule(latch::countDown);

        assertTrue(
                latch.await(2, TimeUnit.SECONDS),
                "действие должно выполниться после первой задержки (1с)"
        );
    }

    @Test
    void schedule_DoesNotRunActionImmediately() throws InterruptedException {
        // проверка на то, что задержка (1с) действительно работает
        var counter = new AtomicInteger(0);

        sut.schedule(counter::incrementAndGet);

        Thread.sleep(200);
        assertFalse(counter.get() > 0, "действие не должно выполниться раньше первой задержки");
    }

    @Test
    void reset_ResetsDelayForNextSchedule() throws InterruptedException {
        sut.schedule(() -> { });
        sut.schedule(() -> { });

        // счетчик попыток сейчас равен 2, на 3 попытке delay будет 5с,
        // reset должен сбросить счетчик в 0
        sut.reset();

        var latch = new CountDownLatch(1);
        sut.schedule(latch::countDown);

        assertTrue(
                latch.await(2, TimeUnit.SECONDS),
                "после reset() задержка должна вернуться к минимальной (1с), а не расти дальше"
        );
    }

    @Test
    void shutdown_CancelRunningAlreadyScheduledAction() throws InterruptedException {
        var counter = new AtomicInteger(0);
        sut.schedule(counter::incrementAndGet); // сработает не раньше чем через 1с

        sut.shutdown();

        Thread.sleep(1200); // действие уже сработало бы без отмены
        assertFalse(
                counter.get() > 0,
                "shutdown() должен предотвратить выполнение уже запланированной задачи"
        );
    }
}