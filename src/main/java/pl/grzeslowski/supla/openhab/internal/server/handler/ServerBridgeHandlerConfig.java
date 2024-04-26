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

import static pl.grzeslowski.supla.openhab.internal.SuplaBindingConstants.DEFAULT_PORT;

import java.math.BigDecimal;
import lombok.Data;
import lombok.ToString;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;

/** @author Grzeslowski - Initial contribution */
@NonNullByDefault
@Data
public class ServerBridgeHandlerConfig {
    @Nullable
    private BigDecimal serverAccessId;

    @Nullable
    @ToString.Exclude
    private String serverAccessIdPassword;

    @Nullable
    private String email;

    private BigDecimal port = new BigDecimal(DEFAULT_PORT);
    private boolean ssl = true;
    private String protocols = "TLSv1.3, TLSv1.2, TLSv1, TLSv1.1";
    private BigDecimal timeout = new BigDecimal(8);
    private BigDecimal timeoutMin = new BigDecimal(10);
    private BigDecimal timeoutMax = new BigDecimal(12);

    public boolean isServerAuth() {
        return serverAccessId != null && serverAccessIdPassword != null;
    }

    public boolean isEmailAuth() {
        return email != null;
    }
}
