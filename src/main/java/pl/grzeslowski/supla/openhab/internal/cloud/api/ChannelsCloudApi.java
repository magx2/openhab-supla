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
package pl.grzeslowski.supla.openhab.internal.cloud.api;

import io.swagger.client.ApiException;
import io.swagger.client.model.ChannelExecuteActionRequest;
import java.util.List;
import org.eclipse.jdt.annotation.NonNullByDefault;

/** @author Grzeslowski - Initial contribution */
@NonNullByDefault
public interface ChannelsCloudApi {
    void executeAction(ChannelExecuteActionRequest body, Integer id) throws ApiException;

    io.swagger.client.model.Channel getChannel(int id, List<String> include) throws Exception;

    List<io.swagger.client.model.Channel> getChannels(List<String> include) throws Exception;
}
