package pl.grzeslowski.openhab.supla.internal.server.oh_config;

import java.math.BigDecimal;
import java.time.Duration;
import lombok.Data;
import lombok.ToString;
import org.eclipse.jdt.annotation.Nullable;

@Data
public class ServerDeviceHandlerConfiguration {
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

    public Duration getTimeout() {
        return TimeoutConfiguration.tryParseDuration(timeout).orElse(null);
    }

    public Duration getTimeoutMin() {
        return TimeoutConfiguration.tryParseDuration(timeoutMin).orElse(null);
    }

    public Duration getTimeoutMax() {
        return TimeoutConfiguration.tryParseDuration(timeoutMax).orElse(null);
    }
}
