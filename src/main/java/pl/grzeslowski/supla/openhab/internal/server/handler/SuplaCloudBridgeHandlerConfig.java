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

import java.math.BigDecimal;
import lombok.Data;
import lombok.ToString;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import pl.grzeslowski.supla.openhab.internal.SuplaBindingConstants;

/** @author Grzeslowski - Initial contribution */
@NonNullByDefault
@Data
public class SuplaCloudBridgeHandlerConfig {
    @Nullable
    private BigDecimal serverAccessId;

    @Nullable
    @ToString.Exclude
    private String serverAccessIdPassword;

    @Nullable
    private String email;

    @Nullable
    @ToString.Exclude
    private String authKey;

    private BigDecimal port = new BigDecimal(SuplaBindingConstants.DEFAULT_PORT);
    private boolean ssl = true;

    public boolean isServerAuth() {
        return serverAccessId != null && serverAccessIdPassword != null;
    }

    public boolean isEmailAuth() {
        return email != null && authKey != null;
    }
}
