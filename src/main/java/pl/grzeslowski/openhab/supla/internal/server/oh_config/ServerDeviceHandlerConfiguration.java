package pl.grzeslowski.openhab.supla.internal.server.oh_config;

import java.math.BigDecimal;
import lombok.ToString;
import org.eclipse.jdt.annotation.Nullable;

public record ServerDeviceHandlerConfiguration(
        @Nullable String guid,
        @Nullable BigDecimal serverAccessId,
        @Nullable @ToString.Exclude String serverAccessIdPassword,
        @Nullable String email,
        @Nullable String authKey,
        @Nullable BigDecimal timeout,
        @Nullable BigDecimal timeoutMin,
        @Nullable BigDecimal timeoutMax) {

    public Integer getTimeout() {
        if (this.timeout == null) {
            return null;
        }
        return this.timeout.intValue();
    }

    public Integer getTimeoutMin() {
        if (this.timeoutMin == null) {
            return null;
        }
        return this.timeoutMin.intValue();
    }

    public Integer getTimeoutMax() {
        if (this.timeoutMax == null) {
            return null;
        }
        return this.timeoutMax.intValue();
    }
}
