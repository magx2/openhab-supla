package pl.grzeslowski.openhab.supla.internal.server.handler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

import java.time.Duration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pl.grzeslowski.openhab.supla.internal.extension.random.Guid;
import pl.grzeslowski.openhab.supla.internal.extension.random.RandomExtension;
import pl.grzeslowski.openhab.supla.internal.server.oh_config.TimeoutConfiguration;

@ExtendWith({MockitoExtension.class, RandomExtension.class})
class PingIT {
    @InjectMocks
    Ping ping;

    @Mock
    Ping.DeviceStatusUpdater updater;

    final TimeoutConfiguration timeout =
            new TimeoutConfiguration(Duration.ofMillis(300), Duration.ofMillis(100), Duration.ofMillis(500));

    @BeforeEach
    void setUp(@Guid String guid) {
        given(updater.getGuid()).willReturn(guid);
    }

    @Test
    @DisplayName("should ping normal device")
    void normalDevice() throws InterruptedException {
        // given
        ping.start(timeout);

        // when
        Thread.sleep(timeout.max().minusMillis(100));
        ping.ping();
        Thread.sleep(timeout.max().minusMillis(100));
        ping.ping();

        // then
        // pings where coming by so updater should not be turned off
        verify(updater, never()).unresponsive(any(), any());
        assertThat(ping.isRunning()).isTrue();
        // wait for ping to expire
        Thread.sleep(timeout.max().multipliedBy(2));
        verify(updater).unresponsive(any(), any());
        assertThat(ping.isRunning()).isFalse();
    }

    @Test
    @DisplayName("should close ping even if `unresponsive` throws exception")
    void unresponsiveThrowsException() throws InterruptedException {
        // given
        doThrow(new RuntimeException("BUM!")).when(updater).unresponsive(any(), any());
        ping.start(timeout);

        // when
        Thread.sleep(timeout.max().minusMillis(100));
        ping.ping();

        // then
        await().timeout(timeout.max().multipliedBy(2))
                .untilAsserted(() -> assertThat(ping.isRunning()).isFalse());
    }

    @Test
    @DisplayName("should close ping even if `unresponsive` throws exception")
    void channelDisconnectedThrowsException() throws InterruptedException {
        // given
        given(updater.isSleepModeEnabled()).willReturn(true);
        given(updater.isSleeping()).willReturn(true);
        doThrow(new RuntimeException("BUM!")).when(updater).channelDisconnected();
        ping.start(timeout);

        // when
        Thread.sleep(timeout.max().minusMillis(100));
        ping.ping();

        // then
        await().timeout(timeout.max().multipliedBy(2))
                .untilAsserted(() -> assertThat(ping.isRunning()).isFalse());
    }

    @Test
    @DisplayName("should ping sleep device (that is not sleeping)")
    void sleepDevice() throws InterruptedException {
        // given
        given(updater.isSleepModeEnabled()).willReturn(true);
        ping.start(timeout);

        // when
        Thread.sleep(timeout.max().minusMillis(100));
        ping.ping();
        Thread.sleep(timeout.max().minusMillis(100));
        ping.ping();

        // then
        // pings where coming by so updater should not be turned off
        verify(updater, never()).unresponsive(any(), any());
        assertThat(ping.isRunning()).isTrue();
        // wait for ping to expire
        Thread.sleep(timeout.max().multipliedBy(2));
        verify(updater).unresponsive(any(), any());
        assertThat(ping.isRunning()).isFalse();
    }

    @Test
    @DisplayName("should ping sleep device (that is not sleeping)")
    void sleepDeviceIsSleeping() throws InterruptedException {
        // given
        given(updater.isSleepModeEnabled()).willReturn(true);
        given(updater.isSleeping()).willReturn(true);
        ping.start(timeout);

        // when
        Thread.sleep(timeout.max().minusMillis(100));
        ping.ping();
        Thread.sleep(timeout.max().minusMillis(100));
        ping.ping();

        // then
        // pings where coming by so updater should not be turned off
        verify(updater, never()).channelDisconnected();
        verify(updater, never()).unresponsive(any(), any());
        assertThat(ping.isRunning()).isTrue();
        // wait for ping to expire
        Thread.sleep(timeout.max().multipliedBy(2));
        verify(updater).channelDisconnected();
        verify(updater, never()).unresponsive(any(), any());
        assertThat(ping.isRunning()).isFalse();
    }
}
