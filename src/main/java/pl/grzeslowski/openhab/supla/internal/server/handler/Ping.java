package pl.grzeslowski.openhab.supla.internal.server.handler;

import static java.time.Instant.now;
import static java.util.Objects.requireNonNull;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.openhab.core.types.UnDefType.UNDEF;
import static pl.grzeslowski.openhab.supla.internal.GuidLogger.attachGuid;
import static pl.grzeslowski.openhab.supla.internal.SuplaBindingConstants.BINDING_ID;

import java.io.Closeable;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ScheduledFuture;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.common.ThreadPoolManager;
import org.openhab.core.library.types.DateTimeType;
import org.openhab.core.types.State;
import pl.grzeslowski.openhab.supla.internal.server.oh_config.TimeoutConfiguration;

@Slf4j
@RequiredArgsConstructor
class Ping implements Closeable {
    private final DeviceStatusUpdater updater;
    private volatile Instant lastMessageFromDevice;

    @Nullable
    private ScheduledFuture<?> pingSchedule;

    @Nullable
    private TimeoutConfiguration timeout;

    @Nullable
    private Instant valuesValidUntil;

    public synchronized boolean isRunning() {
        return pingSchedule != null && !pingSchedule.isCancelled() && timeout != null && lastMessageFromDevice != null;
    }

    public synchronized void start(TimeoutConfiguration timeout) {
        if (isRunning()) {
            attachGuid(updater.getGuid(), () -> log.warn("Ping is already running for device {}", updater.getGuid()));
            return;
        }
        lastMessageFromDevice = now();
        this.timeout = timeout;
        pingSchedule = ThreadPoolManager.getScheduledPool(BINDING_ID)
                .scheduleWithFixedDelay(
                        this::checkIfDeviceIsUp,
                        timeout.timeout().multipliedBy(2).toMillis(),
                        timeout.timeout().toMillis(),
                        MILLISECONDS);
    }

    public synchronized void keepAliveFor(Duration validityTime) {
        var validUntil = now().plus(validityTime);
        if (valuesValidUntil == null || validUntil.isAfter(valuesValidUntil)) {
            valuesValidUntil = validUntil;
        }
    }

    private synchronized void checkIfDeviceIsUp() {
        if (!isRunning()) {
            return;
        }
        var now = now();
        if (valuesValidUntil != null && !now.isAfter(valuesValidUntil)) {
            return;
        }
        if (lastMessageFromDevice.plus(requireNonNull(timeout).max()).compareTo(now) < 0) {
            var delta = Duration.between(lastMessageFromDevice, now);
            try {
                if (updater.isSleepModeEnabled() && updater.isSleeping()) {
                    attachGuid(
                            updater.getGuid(),
                            () -> log.debug(
                                    "Updater is sleeping and there was no ping in {} so disconnecting channel", delta));
                    updater.channelDisconnected();
                } else {
                    attachGuid(updater.getGuid(), () -> log.debug("Ping expired, delta={}", delta));
                    updater.unresponsive(delta, lastMessageFromDevice);
                }
            } catch (Exception e) {
                attachGuid(updater.getGuid(), () -> log.warn("Error occurred", e));
            } finally {
                close();
            }
        }
    }

    @Override
    public synchronized void close() {
        var localPingSchedule = pingSchedule;
        if (localPingSchedule == null) {
            return;
        }
        pingSchedule = null;
        timeout = null;
        lastMessageFromDevice = null;
        valuesValidUntil = null;

        var cancelled = requireNonNull(localPingSchedule).cancel(true);
        if (!cancelled) {
            attachGuid(
                    updater.getGuid(),
                    () -> log.warn("Failed to cancel ping schedule for device {}", updater.getGuid()));
        }
    }

    public synchronized void ping(TimeoutConfiguration timeout) {
        if (!isRunning()) {
            start(timeout);
        }
        ping();
    }

    public synchronized void ping() {
        if (!isRunning()) {
            attachGuid(updater.getGuid(), () -> log.warn("Ping is not running for device {}", updater.getGuid()));
            return;
        }
        attachGuid(updater.getGuid(), () -> log.trace("Ping {}", updater.getGuid()));
        lastMessageFromDevice = now();
    }

    public synchronized State toState() {
        return isRunning() ? new DateTimeType(lastMessageFromDevice) : UNDEF;
    }

    public interface DeviceStatusUpdater {
        @Nullable
        String getGuid();

        boolean isSleepModeEnabled();

        void channelDisconnected();

        boolean isSleeping();

        void unresponsive(Duration delta, Instant lastMessageFromDevice);
    }

    @Override
    public String toString() {
        return "Ping{" //
                + "guid=" + updater.getGuid() //
                + ", timeout=" + timeout //
                + ", lastMessageFromDevice=" + lastMessageFromDevice //
                + '}';
    }
}
