package pl.grzeslowski.openhab.supla.internal.server.handler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.openhab.core.common.ThreadPoolManager;
import pl.grzeslowski.openhab.supla.internal.server.oh_config.TimeoutConfiguration;

@NonNullByDefault
@ExtendWith(MockitoExtension.class)
class PingTest {
    @Mock
    private Ping.DeviceStatusUpdater updater;

    @Mock
    private ScheduledExecutorService executor;

    @Mock
    private ScheduledFuture<?> future;

    private final AtomicReference<Instant> currentTime = new AtomicReference<>(Instant.parse("2026-10-18T12:00:00Z"));
    private final AtomicReference<Runnable> check = new AtomicReference<>();
    private MockedStatic<Instant> clock;
    private MockedStatic<ThreadPoolManager> pools;
    private Ping ping;

    @BeforeEach
    void setUp() {
        when(executor.scheduleWithFixedDelay(any(), anyLong(), anyLong(), any()))
                .thenAnswer(invocation -> {
                    check.set(invocation.getArgument(0));
                    return future;
                });
        when(future.cancel(true)).thenReturn(true);
        clock = mockStatic(Instant.class, CALLS_REAL_METHODS);
        pools = mockStatic(ThreadPoolManager.class);
        clock.when(Instant::now).thenAnswer(__ -> currentTime.get());
        pools.when(() -> ThreadPoolManager.getScheduledPool("supla")).thenReturn(executor);
        ping = new Ping(updater);
        ping.start(new TimeoutConfiguration(10, 8, 12));
    }

    @AfterEach
    void tearDown() {
        ping.close();
        pools.close();
        clock.close();
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void shouldKeepSleepingDeviceOnlineUntilLatestValuesExpire(boolean shorterReading) {
        when(updater.isSleepModeEnabled()).thenReturn(true);
        when(updater.isSleeping()).thenReturn(true);
        ping.updateChannelValidity(0, Duration.ofMinutes(1));

        currentTime.set(currentTime.get().plusSeconds(30));
        check.get().run();
        verify(updater, never()).channelDisconnected();
        verify(updater, never()).unresponsive(any(), any());
        assertThat(ping.isRunning()).isTrue();

        if (shorterReading) {
            ping.ping();
            ping.updateChannelValidity(0, Duration.ofSeconds(5));
        }
        currentTime.set(currentTime.get().plusSeconds(shorterReading ? 13 : 31));
        check.get().run();

        verify(updater).channelDisconnected();
        assertThat(ping.isRunning()).isFalse();
        verify(future).cancel(true);
    }

    @Test
    void shouldRetainOtherChannelsValidityWhenOneChannelShortensItsValidity() {
        when(updater.isSleepModeEnabled()).thenReturn(true);
        when(updater.isSleeping()).thenReturn(true);
        ping.updateChannelValidity(0, Duration.ofSeconds(120));
        ping.updateChannelValidity(1, Duration.ofSeconds(90));

        currentTime.set(currentTime.get().plusSeconds(30));
        ping.ping();
        ping.updateChannelValidity(0, Duration.ofSeconds(5));
        currentTime.set(currentTime.get().plusSeconds(13));
        check.get().run();

        verify(updater, never()).channelDisconnected();
        verify(updater, never()).unresponsive(any(), any());
        assertThat(ping.isRunning()).isTrue();

        currentTime.set(currentTime.get().plusSeconds(48));
        check.get().run();

        verify(updater).channelDisconnected();
        assertThat(ping.isRunning()).isFalse();
    }

    @ParameterizedTest
    @MethodSource("nonExpiringValues")
    void shouldDiscardDeadlineWhenNewValueHasNoValidity(@Nullable Duration validity) {
        when(updater.isSleepModeEnabled()).thenReturn(true);
        when(updater.isSleeping()).thenReturn(true);
        ping.updateChannelValidity(0, Duration.ofMinutes(1));

        currentTime.set(currentTime.get().plusSeconds(30));
        ping.ping();
        ping.updateChannelValidity(0, validity);
        currentTime.set(currentTime.get().plusSeconds(13));
        check.get().run();

        verify(updater).channelDisconnected();
        assertThat(ping.isRunning()).isFalse();
    }

    private static Stream<@Nullable Duration> nonExpiringValues() {
        return Stream.of(null, Duration.ZERO);
    }

    @Test
    void shouldAllowReentrantAndRepeatedCleanup() {
        doAnswer(__ -> {
                    ping.close();
                    return null;
                })
                .when(updater)
                .unresponsive(any(), any());

        currentTime.set(currentTime.get().plusSeconds(13));
        check.get().run();
        ping.close();

        assertThat(ping.isRunning()).isFalse();
        verify(future).cancel(true);
    }
}
