package pl.grzeslowski.supla.openhab.internal.server.handler;

import java.math.BigDecimal;
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
    private BigDecimal timeout;

    @Nullable
    private BigDecimal timeoutMin;

    @Nullable
    private BigDecimal timeoutMax;

    public Integer getTimeout() {
        if (timeout == null) {
            return null;
        }
        return timeout.intValue();
    }

    public Integer getTimeoutMin() {
        if (timeoutMin == null) {
            return null;
        }
        return timeoutMin.intValue();
    }

    public Integer getTimeoutMax() {
        if (timeoutMax == null) {
            return null;
        }
        return timeoutMax.intValue();
    }
}
