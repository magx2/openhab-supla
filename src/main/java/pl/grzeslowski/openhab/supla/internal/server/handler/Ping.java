package pl.grzeslowski.openhab.supla.internal.server.handler;

import static java.time.Instant.now;
import static java.util.Objects.requireNonNull;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.openhab.core.thing.ThingStatus.OFFLINE;
import static org.openhab.core.thing.ThingStatusDetail.COMMUNICATION_ERROR;
import static org.openhab.core.types.UnDefType.UNDEF;
import static pl.grzeslowski.openhab.supla.internal.GuidLogger.attachGuid;
import static pl.grzeslowski.openhab.supla.internal.Localization.text;
import static pl.grzeslowski.openhab.supla.internal.SuplaBindingConstants.BINDING_ID;

import java.io.Closeable;
import java.text.SimpleDateFormat;
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
    private final DeviceStatusUpdater updater;
    private volatile long lastMessageFromDevice = 0L;

    @Nullable
    private ScheduledFuture<?> pingSchedule;

    @Nullable
    private TimeoutConfiguration timeout;

    public boolean isRunning() {
        return pingSchedule != null && !pingSchedule.isCancelled() && timeout != null && lastMessageFromDevice > 0L;
    }

    public synchronized void start(TimeoutConfiguration timeout) {
        if (isRunning()) {
            attachGuid(updater.getGuid(), () -> log.warn("Ping is already running for device {}", updater.getGuid()));
            return;
        }
        lastMessageFromDevice = now().getEpochSecond();
        this.timeout = timeout;
        if (updater.isSleepModeEnabled() && timeout.timeout() < 30) {
            attachGuid(
                    updater.getGuid(),
                    () -> log.warn(
                            "This is a sleep device and timeout is low ({} s). Consider increasing the timeout to prevent ONLINE->OFFLINE toggeling.",
                            timeout.timeout()));
        }
        pingSchedule = ThreadPoolManager.getScheduledPool(BINDING_ID)
                .scheduleWithFixedDelay(this::checkIfDeviceIsUp, timeout.timeout() * 2L, timeout.timeout(), SECONDS);
    }

    private synchronized void checkIfDeviceIsUp() {
        if (!isRunning()) {
            return;
        }
        var now = now().getEpochSecond();
        var delta = now - lastMessageFromDevice;
        if (delta > requireNonNull(timeout).max()) {
            var lastPingDate = new Date(SECONDS.toMillis(lastMessageFromDevice));
            var formatter = new SimpleDateFormat("HH:mm:ss z");
            updater.updateStatus(
                    OFFLINE, COMMUNICATION_ERROR, text("supla.offline.no-ping", delta, formatter.format(lastPingDate)));
            close();
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
        lastMessageFromDevice = 0;

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
        lastMessageFromDevice = now().getEpochSecond();
    }

    public State toState() {
        return isRunning() ? new DateTimeType(Instant.ofEpochSecond(lastMessageFromDevice)) : UNDEF;
    }

    public interface DeviceStatusUpdater {
        @Nullable
        String getGuid();

        boolean isSleepModeEnabled();

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
