package pl.grzeslowski.openhab.supla.internal.server.oh_config;

import java.math.BigDecimal;
import java.time.Duration;
import lombok.Data;
import lombok.ToString;
import org.eclipse.jdt.annotation.Nullable;
import pl.grzeslowski.openhab.supla.internal.server.handler.trait.ServerBridge;

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
        return ServerBridge.tryParseDuration(timeout).orElse(null);
    }

    public Duration getTimeoutMin() {
        return ServerBridge.tryParseDuration(timeoutMin).orElse(null);
    }

    public Duration getTimeoutMax() {
        return ServerBridge.tryParseDuration(timeoutMax).orElse(null);
    }
}
