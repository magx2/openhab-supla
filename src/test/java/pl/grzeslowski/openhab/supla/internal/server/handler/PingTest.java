package pl.grzeslowski.openhab.supla.internal.server.handler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.atomic.AtomicReference;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.openhab.core.common.ThreadPoolManager;
import pl.grzeslowski.openhab.supla.internal.server.oh_config.TimeoutConfiguration;

@NonNullByDefault
@ExtendWith(MockitoExtension.class)
class PingTest {
    @Mock
    private Ping.DeviceStatusUpdater updater;

    @Test
    void shouldKeepSleepingDeviceOnlineUntilReportedValuesExpire() {
        var currentTime = new AtomicReference<>(Instant.parse("2026-10-18T12:00:00Z"));
        var executor = mock(ScheduledExecutorService.class);
        var future = mock(ScheduledFuture.class);
        var check = new AtomicReference<Runnable>();
        when(executor.scheduleWithFixedDelay(any(), anyLong(), anyLong(), any()))
                .thenAnswer(invocation -> {
                    check.set(invocation.getArgument(0));
                    return future;
                });
        when(future.cancel(true)).thenReturn(true);
        when(updater.isSleepModeEnabled()).thenReturn(true);
        when(updater.isSleeping()).thenReturn(true);
        try (var clock = mockStatic(Instant.class, CALLS_REAL_METHODS);
                var pools = mockStatic(ThreadPoolManager.class)) {
            clock.when(Instant::now).thenAnswer(__ -> currentTime.get());
            pools.when(() -> ThreadPoolManager.getScheduledPool("supla")).thenReturn(executor);
            var ping = new Ping(updater);
            ping.start(new TimeoutConfiguration(10, 8, 12));
            ping.keepAliveFor(Duration.ofMinutes(1));

            currentTime.set(currentTime.get().plusSeconds(30));
            check.get().run();
            verify(updater, never()).channelDisconnected();
            verify(updater, never()).unresponsive(any(), any());
            assertThat(ping.isRunning()).isTrue();

            currentTime.set(currentTime.get().plusSeconds(31));
            check.get().run();
            verify(updater).channelDisconnected();
            assertThat(ping.isRunning()).isFalse();
            verify(future).cancel(true);
        }
    }

    @Test
    void shouldAllowReentrantAndRepeatedCleanup() {
        var currentTime = new AtomicReference<>(Instant.parse("2026-10-18T12:00:00Z"));
        var executor = mock(ScheduledExecutorService.class);
        var future = mock(ScheduledFuture.class);
        var check = new AtomicReference<Runnable>();
        when(executor.scheduleWithFixedDelay(any(), anyLong(), anyLong(), any()))
                .thenAnswer(invocation -> {
                    check.set(invocation.getArgument(0));
                    return future;
                });
        when(future.cancel(true)).thenReturn(true);
        try (var clock = mockStatic(Instant.class, CALLS_REAL_METHODS);
                var pools = mockStatic(ThreadPoolManager.class)) {
            clock.when(Instant::now).thenAnswer(__ -> currentTime.get());
            pools.when(() -> ThreadPoolManager.getScheduledPool("supla")).thenReturn(executor);
            var ping = new Ping(updater);
            doAnswer(__ -> {
                        ping.close();
                        return null;
                    })
                    .when(updater)
                    .unresponsive(any(), any());
            ping.start(new TimeoutConfiguration(10, 8, 12));

            currentTime.set(currentTime.get().plusSeconds(13));
            check.get().run();
            ping.close();

            assertThat(ping.isRunning()).isFalse();
            verify(future).cancel(true);
        }
    }
}
