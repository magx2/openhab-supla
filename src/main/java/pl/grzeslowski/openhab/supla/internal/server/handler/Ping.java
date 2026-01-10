package pl.grzeslowski.openhab.supla.internal.server.handler;

import static java.time.Instant.now;
import static java.util.Objects.requireNonNull;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.openhab.core.thing.ThingStatus.OFFLINE;
import static org.openhab.core.thing.ThingStatusDetail.COMMUNICATION_ERROR;
import static org.openhab.core.types.UnDefType.UNDEF;
import static pl.grzeslowski.openhab.supla.internal.GuidLogger.attachGuid;
import static pl.grzeslowski.openhab.supla.internal.Localization.text;
import static pl.grzeslowski.openhab.supla.internal.SuplaBindingConstants.BINDING_ID;

import java.io.Closeable;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
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
    private static final Duration MINIMAL_TIMEOUT_FOR_SLEEP_DEVICE = Duration.ofSeconds(30);
    private final DeviceStatusUpdater updater;
    private volatile Instant lastMessageFromDevice;

    @Nullable
    private ScheduledFuture<?> pingSchedule;

    @Nullable
    private TimeoutConfiguration timeout;

    public boolean isRunning() {
        return pingSchedule != null && !pingSchedule.isCancelled() && timeout != null && lastMessageFromDevice != null;
    }

    public synchronized void start(TimeoutConfiguration timeout) {
        if (isRunning()) {
            attachGuid(updater.getGuid(), () -> log.warn("Ping is already running for device {}", updater.getGuid()));
            return;
        }
        lastMessageFromDevice = now();
        this.timeout = timeout;
        if (updater.isSleepModeEnabled() && timeout.timeout().compareTo(MINIMAL_TIMEOUT_FOR_SLEEP_DEVICE) < 0) {
            attachGuid(
                    updater.getGuid(),
                    () -> log.warn(
                            "This is a sleep device and timeout is low ({}). Consider increasing the timeout to {}.",
                            timeout.timeout(),
                            MINIMAL_TIMEOUT_FOR_SLEEP_DEVICE));
        }
        pingSchedule = ThreadPoolManager.getScheduledPool(BINDING_ID)
                .scheduleWithFixedDelay(
                        this::checkIfDeviceIsUp,
                        timeout.timeout().multipliedBy(2).toMillis(),
                        timeout.timeout().toMillis(),
                        MILLISECONDS);
    }

    private synchronized void checkIfDeviceIsUp() {
        if (!isRunning()) {
            return;
        }
        var now = now();
        if (lastMessageFromDevice.plus(requireNonNull(timeout).max()).compareTo(now) < 0) {
            var formatter = new SimpleDateFormat("HH:mm:ss z");
            var delta = Duration.between(lastMessageFromDevice, now);
            if (updater.isSleepModeEnabled() && updater.isSleeping()) {
                log.debug("Updater is sleeping and there was no ping in {} so disconneting channel", delta);
                updater.channelDisconnected();
                // closing ping will be done in `channelDisconnected`
            } else {
                updater.updateStatus(
                        OFFLINE,
                        COMMUNICATION_ERROR,
                        text(
                                "supla.offline.no-ping",
                                delta.getSeconds(),
                                formatter.format(Date.from(lastMessageFromDevice))));
                close();
            }
        }
    }

    @Override
    public synchronized void close() {
        if (!isRunning()) {
            attachGuid(updater.getGuid(), () -> log.warn("Ping is not running for device {}", updater.getGuid()));
            return;
        }
        var localPingSchedule = pingSchedule;
        pingSchedule = null;
        timeout = null;
        lastMessageFromDevice = null;

        var cancelled = requireNonNull(localPingSchedule).cancel(true);
        if (!cancelled) {
            attachGuid(
                    updater.getGuid(),
                    () -> log.warn("Failed to cancel ping schedule for device {}", updater.getGuid()));
        }
    }

    public synchronized void ping() {
        if (!isRunning()) {
            attachGuid(updater.getGuid(), () -> log.warn("Ping is not running for device {}", updater.getGuid()));
            return;
        }
        attachGuid(updater.getGuid(), () -> log.trace("Ping {}", updater.getGuid()));
        lastMessageFromDevice = now();
    }

    public State toState() {
        return isRunning() ? new DateTimeType(lastMessageFromDevice) : UNDEF;
    }

    public interface DeviceStatusUpdater {
        @Nullable
        String getGuid();

        boolean isSleepModeEnabled();

        void channelDisconnected();

        boolean isSleeping();

        void updateStatus(
                org.openhab.core.thing.ThingStatus status,
                org.openhab.core.thing.ThingStatusDetail statusDetail,
                String description);
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
