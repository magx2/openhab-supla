/**
 * Copyright (c) 2010-2024 Contributors to the openHAB project
 *
 * <p>See the NOTICE file(s) distributed with this work for additional information.
 *
 * <p>This program and the accompanying materials are made available under the terms of the Eclipse Public License 2.0
 * which is available at http:www.eclipse.org/legal/epl-2.0
 *
 * <p>SPDX-License-Identifier: EPL-2.0
 */
package pl.grzeslowski.supla.openhab.internal.server.handler;

import static java.util.Objects.requireNonNull;

import java.math.BigDecimal;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import pl.grzeslowski.supla.openhab.internal.SuplaBindingConstants;

/** @author Grzeslowski - Initial contribution */
@NonNullByDefault
public class SuplaCloudBridgeHandlerConfig {
    @Nullable
    private BigDecimal serverAccessId;

    @Nullable
    private String serverAccessIdPassword;

    @Nullable
    private String email;

    @Nullable
    private String authKey;

    private BigDecimal port = new BigDecimal(SuplaBindingConstants.DEFAULT_PORT);
    private boolean ssl = true;

    @Nullable
    public Integer getServerAccessId() {
        return serverAccessId != null ? serverAccessId.intValue() : null;
    }

    public void setServerAccessId(BigDecimal serverAccessId) {
        this.serverAccessId = serverAccessId;
    }

    @Nullable
    public String getServerAccessIdPassword() {
        return serverAccessIdPassword;
    }

    public void setServerAccessIdPassword(String serverAccessIdPassword) {
        this.serverAccessIdPassword = serverAccessIdPassword;
    }

    @Nullable
    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    @Nullable
    public String getAuthKey() {
        return authKey;
    }

    public void setAuthKey(String authKey) {
        this.authKey = authKey;
    }

    public BigDecimal getPort() {
        return requireNonNull(port);
    }

    public void setPort(BigDecimal port) {
        this.port = port;
    }

    public boolean isServerAuth() {
        return serverAccessId != null && serverAccessIdPassword != null;
    }

    public boolean isEmailAuth() {
        return email != null && authKey != null;
    }

    public boolean isSsl() {
        return ssl;
    }

    public void setSsl(boolean ssl) {
        this.ssl = ssl;
    }

    @Override
    public String toString() {
        return "SuplaCloudBridgeHandlerConfig{" + "serverAccessId="
                + serverAccessId + ", serverAccessIdPassword='<SECRET>'"
                + ", email='"
                + email + '\'' + ", authKey='<SECRET>'"
                + ", port="
                + port + '}';
    }
}
