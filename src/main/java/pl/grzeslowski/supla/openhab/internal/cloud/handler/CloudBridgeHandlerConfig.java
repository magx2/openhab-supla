package pl.grzeslowski.supla.openhab.internal.cloud.handler;

import static java.lang.Math.max;
import static java.util.concurrent.TimeUnit.MINUTES;

import java.math.BigDecimal;
import org.eclipse.jdt.annotation.NonNullByDefault;

/** @author Martin Grześlowski - Initial contribution */
@NonNullByDefault
public class CloudBridgeHandlerConfig {
    private String oAuthToken = "";
    private BigDecimal refreshInterval = BigDecimal.valueOf(30);
    private BigDecimal refreshHandlerInterval = BigDecimal.valueOf(MINUTES.toSeconds(10));
    private int cacheEvict = 30;

    public String getOAuthToken() {
        return oAuthToken;
    }

    public void setOAuthToken(String oAuthToken) {
        this.oAuthToken = oAuthToken;
    }

    public long getRefreshInterval() {
        return max(refreshInterval.longValue(), 1);
    }

    public void setRefreshInterval(BigDecimal refreshInterval) {
        this.refreshInterval = refreshInterval;
    }

    public int getCacheEvict() {
        return cacheEvict;
    }

    public void setCacheEvict(int cacheEvict) {
        this.cacheEvict = cacheEvict;
    }

    public long getRefreshHandlerInterval() {
        return max(refreshHandlerInterval.longValue(), 1);
    }

    public void setRefreshHandlerInterval(BigDecimal refreshHandlerInterval) {
        this.refreshHandlerInterval = refreshHandlerInterval;
    }
}
