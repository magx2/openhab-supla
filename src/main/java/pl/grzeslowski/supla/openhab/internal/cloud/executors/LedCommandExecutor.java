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
package pl.grzeslowski.supla.openhab.internal.cloud.executors;

import io.swagger.client.ApiException;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.core.library.types.HSBType;
import org.openhab.core.library.types.PercentType;

/** @author Grzeslowski - Initial contribution */
@NonNullByDefault
public interface LedCommandExecutor {
    void setLedState(final int channelId, final PercentType brightness);

    void setLedState(final int channelId, final HSBType hsb);

    void changeColor(final int channelId, final HSBType command) throws ApiException;

    void changeColorBrightness(final int channelId, final PercentType command) throws ApiException;

    void changeBrightness(final int channelId, final PercentType command) throws ApiException;
}
