/**
 * Copyright (c) 2010-2024 Contributors to the openHAB project
 *
 * <p>See the NOTICE file(s) distributed with this work for additional information.
 *
 * <p>This program and the accompanying materials are made available under the terms of the Eclipse Public License 2.0
 * which is available at http://www.eclipse.org/legal/epl-2.0
 *
 * <p>SPDX-License-Identifier: EPL-2.0
 */
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
