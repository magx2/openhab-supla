package pl.grzeslowski.openhab.supla.internal.server.oh_config;

import java.math.BigDecimal;
import java.time.Duration;
import lombok.Data;
import lombok.ToString;
import org.eclipse.jdt.annotation.Nullable;

@Data
public class ServerDeviceHandlerConfiguration {
    private static final Duration DEFAULT_ACTION_TIMEOUT = Duration.ofSeconds(30);

    @Nullable
    private String guid;

    @Nullable
    private BigDecimal serverAccessId;

    @Nullable
    @ToString.Exclude
    private String serverAccessIdPassword;

    @Nullable
    private String email;

    @Nullable
    private String authKey;

    @Nullable
    private String timeout;

    @Nullable
    private String timeoutMin;

    @Nullable
    private String timeoutMax;

    @Nullable
    private String actionTimeout = DEFAULT_ACTION_TIMEOUT.toString();

    @Nullable
    private String setDeviceConfigActionTimeout;

    @Nullable
    private String resetElectricMeterCountersActionTimeout;

    @Nullable
    private String enterConfigModeActionTimeout;

    @Nullable
    private String checkFirmwareUpdateActionTimeout;

    @Nullable
    private String startFirmwareUpdateActionTimeout;

    @Nullable
    private String startSecurityUpdateActionTimeout;

    public Duration getTimeout() {
        return TimeoutConfiguration.tryParseDuration(timeout).orElse(null);
    }

    public Duration getTimeoutMin() {
        return TimeoutConfiguration.tryParseDuration(timeoutMin).orElse(null);
    }

    public Duration getTimeoutMax() {
        return TimeoutConfiguration.tryParseDuration(timeoutMax).orElse(null);
    }

    public Duration getActionTimeout() {
        return TimeoutConfiguration.tryParseDuration(actionTimeout).orElse(DEFAULT_ACTION_TIMEOUT);
    }

    public Duration getSetDeviceConfigActionTimeout() {
        return TimeoutConfiguration.tryParseDuration(setDeviceConfigActionTimeout)
                .orElseGet(this::getActionTimeout);
    }

    public Duration getResetElectricMeterCountersActionTimeout() {
        return TimeoutConfiguration.tryParseDuration(resetElectricMeterCountersActionTimeout)
                .orElseGet(this::getActionTimeout);
    }

    public Duration getEnterConfigModeActionTimeout() {
        return TimeoutConfiguration.tryParseDuration(enterConfigModeActionTimeout)
                .orElseGet(this::getActionTimeout);
    }

    public Duration getCheckFirmwareUpdateActionTimeout() {
        return TimeoutConfiguration.tryParseDuration(checkFirmwareUpdateActionTimeout)
                .orElseGet(this::getActionTimeout);
    }

    public Duration getStartFirmwareUpdateActionTimeout() {
        return TimeoutConfiguration.tryParseDuration(startFirmwareUpdateActionTimeout)
                .orElseGet(this::getActionTimeout);
    }

    public Duration getStartSecurityUpdateActionTimeout() {
        return TimeoutConfiguration.tryParseDuration(startSecurityUpdateActionTimeout)
                .orElseGet(this::getActionTimeout);
    }
}
