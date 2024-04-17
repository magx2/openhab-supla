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

import static java.util.Objects.requireNonNull;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** @author Grzeslowski - Initial contribution */
@NonNullByDefault
public final class SwaggerApiFactory
        implements ChannelsCloudApiFactory, IoDevicesCloudApiFactory, ServerCloudApiFactory {
    private static final Logger logger = LoggerFactory.getLogger(SwaggerApiFactory.class);
    private final ApiClientFactory apiClientFactory;
    private final Map<String, SwaggerApi> tokenToApi = Collections.synchronizedMap(new HashMap<>());

    public SwaggerApiFactory(final ApiClientFactory apiClientFactory) {
        this.apiClientFactory = apiClientFactory;
    }

    @Override
    public ChannelsCloudApi newChannelsCloudApi(String token) {
        return requireNonNull(
                tokenToApi.computeIfAbsent(token, t -> new SwaggerApi(apiClientFactory.newApiClient(token, logger))));
    }

    @Override
    public IoDevicesCloudApi newIoDevicesCloudApi(String token) {
        return requireNonNull(
                tokenToApi.computeIfAbsent(token, t -> new SwaggerApi(apiClientFactory.newApiClient(token, logger))));
    }

    @Override
    public ServerCloudApi newServerCloudApi(String token) {
        return requireNonNull(
                tokenToApi.computeIfAbsent(token, t -> new SwaggerApi(apiClientFactory.newApiClient(token, logger))));
    }
}
