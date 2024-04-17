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

import static java.lang.Integer.parseInt;
import static java.lang.Long.parseLong;
import static java.lang.System.currentTimeMillis;
import static java.time.temporal.ChronoUnit.MILLIS;
import static java.util.Collections.emptyMap;
import static java.util.concurrent.TimeUnit.SECONDS;

import io.swagger.client.ApiClient;
import io.swagger.client.ApiException;
import io.swagger.client.ApiResponse;
import io.swagger.client.api.ChannelsApi;
import io.swagger.client.api.IoDevicesApi;
import io.swagger.client.api.ServerApi;
import io.swagger.client.model.Channel;
import io.swagger.client.model.ChannelExecuteActionRequest;
import io.swagger.client.model.Device;
import io.swagger.client.model.ServerInfo;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** @author Grzeslowski - Initial contribution */
@NonNullByDefault
class SwaggerApi implements ChannelsCloudApi, IoDevicesCloudApi, ServerCloudApi {
    private static final long ONE_HOUR_MS = TimeUnit.HOURS.toMillis(1);

    private final Logger logger = LoggerFactory.getLogger(SwaggerApi.class);
    private final ChannelsApi channelsApi;
    private final IoDevicesApi ioDevicesApi;
    private final ServerApi serverApi;
    private final long startTimeMs = currentTimeMillis();
    private final AtomicLong numberOfRequests = new AtomicLong();
    private final AtomicInteger rateLimitLimit = new AtomicInteger();
    private final AtomicInteger rateLimitRemaining = new AtomicInteger();
    private final AtomicLong rateLimitResetTimestamp = new AtomicLong();

    SwaggerApi(final ApiClient apiClient) {
        channelsApi = new ChannelsApi(apiClient);
        ioDevicesApi = new IoDevicesApi(apiClient);
        serverApi = new ServerApi(apiClient);
    }

    @Override
    public void executeAction(final ChannelExecuteActionRequest body, final Integer id) throws ApiException {
        apiCall().apply(() -> {
            channelsApi.executeAction(body, id);
            return new ApiResponse<>(200, emptyMap());
        });
    }

    @Override
    public List<Channel> getChannels(List<String> include) throws ApiException {
        return this.<List<Channel>>apiCall()
                .apply(() -> channelsApi.getChannelsWithHttpInfo(include, null, null, null));
    }

    @Override
    public Channel getChannel(final int id, final List<String> include) throws ApiException {
        return this.<Channel>apiCall().apply(() -> channelsApi.getChannelWithHttpInfo(id, include));
    }

    @Override
    public Device getIoDevice(final int id, final List<String> include) throws Exception {
        return this.<Device>apiCall().apply(() -> ioDevicesApi.getIoDeviceWithHttpInfo(id, include));
    }

    @Override
    public List<Device> getIoDevices(final List<String> include) throws Exception {
        return this.<List<Device>>apiCall().apply(() -> ioDevicesApi.getIoDevicesWithHttpInfo(include));
    }

    @Override
    public ServerInfo getServerInfo() throws ApiException {
        // FYI: server info does not count in API rate limit
        return serverApi.getServerInfo();
    }

    @Override
    public ApiCalls getApiCalls() {
        var timestamp = rateLimitResetTimestamp.get();
        var resetDateTime = Instant.ofEpochSecond(timestamp).atZone(ZoneId.systemDefault());
        var remaining = rateLimitRemaining.get();
        var limit = rateLimitLimit.get();
        var calls = limit - remaining;

        var elapsedTimeMs = currentTimeMillis() - startTimeMs;
        var elapsedTimeSec = elapsedTimeMs / 1_000.0;
        var elapsedTimeMin = elapsedTimeMs / 1_000.0 / 60.0;
        var elapsedTimeHour = elapsedTimeMs / 1_000.0 / 60.0 / 60.0;
        var nr = (double) numberOfRequests.get();
        var reqPerSec = elapsedTimeSec > 0 ? nr / elapsedTimeSec : 0.0;
        var reqPerMin = elapsedTimeMin > 0 ? nr / elapsedTimeMin : 0.0;
        var reqPerHour = elapsedTimeHour > 0 ? nr / elapsedTimeHour : 0.0;

        return new ApiCalls(resetDateTime, limit, calls, remaining, reqPerSec, reqPerMin, reqPerHour);
    }

    private <OutT> ApiFunction<ApiSupplier<OutT>, OutT> apiCall() {
        return (f) -> {
            var now = currentTimeMillis();
            var resetTime = rateLimitResetTimestamp.get();
            var resetTimeMs = SECONDS.toMillis(resetTime);
            if (rateLimitRemaining.get() <= 0 && now < resetTimeMs) {
                var dateTime = Instant.ofEpochSecond(resetTime).atZone(ZoneId.systemDefault());
                var duration = Duration.of(resetTimeMs - now, MILLIS);
                var formatted = String.format(
                        "%02d:%02d:%02d.%03d", //
                        duration.toHours(), //
                        duration.toMinutesPart(), //
                        duration.toSecondsPart(), //
                        duration.toMillisPart());
                throw new ApiException(429, "Rate limit reached! Waiting till " + dateTime + " / " + formatted);
            }
            try {
                var apiResponse = f.get();
                saveRateLimits(apiResponse.getHeaders());
                var data = apiResponse.getData();
                numberOfRequests.incrementAndGet();
                return data;
            } catch (ApiException e) {
                saveRateLimits(e.getResponseHeaders());
                throw e;
            }
        };
    }

    private void saveRateLimits(@Nullable Map<String, List<String>> headers) {
        if (headers == null || headers.isEmpty()) {
            return;
        }
        parseIntFromHeader(headers, "X-RateLimit-Limit", rateLimitLimit);
        parseIntFromHeader(headers, "X-RateLimit-Remaining", rateLimitRemaining);
        parseLongFromHeader(headers, "X-RateLimit-Reset", rateLimitResetTimestamp);
    }

    private void parseIntFromHeader(Map<String, List<String>> headers, String header, AtomicInteger integer) {
        var limit = headers.get(header);
        if (limit == null || limit.isEmpty()) {
            return;
        }
        var value = limit.get(0);
        try {
            integer.set(parseInt(value));
        } catch (NumberFormatException e) {
            logger.warn("Cannot parse {} header to int: {}", header, value);
        }
    }

    @SuppressWarnings("SameParameterValue")
    private void parseLongFromHeader(Map<String, List<String>> headers, String header, AtomicLong along) {
        var limit = headers.get(header);
        if (limit.isEmpty()) {
            return;
        }
        var value = limit.get(0);
        try {
            along.set(parseLong(value));
        } catch (NumberFormatException e) {
            logger.warn("Cannot parse {} header to long: {}", header, value);
        }
    }

    private interface ApiFunction<T, R> {
        R apply(T t) throws ApiException;
    }

    private interface ApiSupplier<T> {
        ApiResponse<T> get() throws ApiException;
    }
}
