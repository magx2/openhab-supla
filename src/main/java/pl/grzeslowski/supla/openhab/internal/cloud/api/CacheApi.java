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

import static java.lang.String.join;
import static java.util.Collections.unmodifiableList;
import static java.util.concurrent.TimeUnit.SECONDS;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import io.swagger.client.ApiException;
import io.swagger.client.model.Channel;
import io.swagger.client.model.ChannelExecuteActionRequest;
import io.swagger.client.model.Device;
import io.swagger.client.model.ServerInfo;
import java.util.*;
import java.util.concurrent.TimeUnit;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** @author Grzeslowski - Initial contribution */
@NonNullByDefault
final class CacheApi implements ChannelsCloudApi, IoDevicesCloudApi, ServerCloudApi {
    private static final TimeUnit cacheEvictUnit = SECONDS;
    private final Logger logger = LoggerFactory.getLogger(CacheApi.class);
    private final ChannelsCloudApi channelsCloudApi;
    private final LoadingCache<List<String>, List<Channel>> getChannelsCache;
    private final LoadingCache<GetChannelKey, Channel> getChannelCache;
    private final LoadingCache<GetIoDeviceKey, Device> getIoDeviceCache;
    private final LoadingCache<List<String>, List<Device>> getIoDevicesCache;
    private final LoadingCache<String, ServerInfo> getServerInfoCache;
    private final ServerCloudApi serverCloudApi;
    private final Set<Set<String>> channlesIncludePermutations;

    CacheApi(
            int cacheEvictTime,
            ChannelsCloudApi channelsCloudApi,
            IoDevicesCloudApi ioDevicesCloudApi,
            ServerCloudApi serverCloudApi) {
        this.channelsCloudApi = channelsCloudApi;
        getChannelsCache = Caffeine.newBuilder()
                .expireAfterWrite(cacheEvictTime, cacheEvictUnit)
                .build(key -> {
                    logger.debug("Missed cache for `getChannels({})`", join(", ", key));
                    return channelsCloudApi.getChannels(key);
                });
        getChannelCache = Caffeine.newBuilder()
                .expireAfterWrite(cacheEvictTime, cacheEvictUnit)
                .build(key -> {
                    logger.debug("Missed cache for `getChannel({})`", key);
                    return channelsCloudApi.getChannel(key.id, key.include);
                });
        getIoDeviceCache = Caffeine.newBuilder()
                .expireAfterWrite(cacheEvictTime, cacheEvictUnit)
                .build(key -> {
                    logger.debug("Missed cache for `getIoDevice({})`", key);
                    return ioDevicesCloudApi.getIoDevice(key.id, key.include);
                });
        getIoDevicesCache = Caffeine.newBuilder()
                .expireAfterWrite(cacheEvictTime, cacheEvictUnit)
                .build(key -> {
                    logger.debug("Missed cache for `getIoDevices({})`", join(", ", key));
                    return ioDevicesCloudApi.getIoDevices(key);
                });
        getServerInfoCache = Caffeine.newBuilder()
                .expireAfterWrite(cacheEvictTime, cacheEvictUnit)
                .build(__ -> {
                    logger.debug("Missed cache for `getServerInfo`");
                    return serverCloudApi.getServerInfo();
                });
        this.serverCloudApi = serverCloudApi;
        channlesIncludePermutations = generateCombinations(List.of(
                "iodevice",
                "location",
                "connected",
                "state",
                "supportedFunctions",
                "measurementLogsCount",
                "relationsCount"));
    }

    @Override
    public void executeAction(final ChannelExecuteActionRequest body, final Integer id) throws ApiException {
        channelsCloudApi.executeAction(body, id);
        channlesIncludePermutations.stream() //
                .filter(include -> include.contains("state")) //
                .map(ArrayList::new) //
                .forEach(include -> {
                    getChannelCache.invalidate(new GetChannelKey(id, include));
                    getChannelsCache.invalidate(include);
                });
    }

    @Override
    public List<Channel> getChannels(List<String> include) {
        return getChannelsCache.get(include);
    }

    @Override
    public Channel getChannel(final int id, final List<String> include) {
        var channel = getChannels(include) //
                .stream() //
                .filter(c -> c.getId() == id) //
                .findFirst();
        return channel.orElseGet(() -> getChannelCache.get(new GetChannelKey(id, include)));
    }

    private static final class GetChannelKey {
        final int id;
        final List<String> include;

        private GetChannelKey(final int id, final List<String> include) {
            this.id = id;
            this.include = unmodifiableList(include);
        }

        @Override
        public boolean equals(@Nullable final Object o) {
            if (this == o) return true;
            if (!(o instanceof GetChannelKey that)) return false;
            return id == that.id && Objects.equals(include, that.include);
        }

        @Override
        public int hashCode() {
            return Objects.hash(id);
        }

        @Override
        public String toString() {
            return id + ", [" + join(", ", include) + "]";
        }
    }

    @Override
    public Device getIoDevice(final int id, final List<String> include) {
        var device = getIoDevices(include) //
                .stream() //
                .filter(d -> d.getId() == id) //
                .findAny();
        return device.orElseGet(() -> getIoDeviceCache.get(new GetIoDeviceKey(id, include)));
    }

    @Override
    public List<Device> getIoDevices(final List<String> include) {
        return getIoDevicesCache.get(include);
    }

    private static final class GetIoDeviceKey {
        final int id;
        final List<String> include;

        private GetIoDeviceKey(final int id, final List<String> include) {
            this.id = id;
            this.include = unmodifiableList(include);
        }

        @Override
        public boolean equals(@Nullable final Object o) {
            if (this == o) return true;
            if (!(o instanceof GetIoDeviceKey that)) return false;
            return id == that.id && Objects.equals(include, that.include);
        }

        @Override
        public int hashCode() {
            return Objects.hash(id);
        }

        @Override
        public String toString() {
            return id + ", [" + join(", ", include) + "]";
        }
    }

    @Override
    public ServerInfo getServerInfo() {
        return getServerInfoCache.get("getServerInfo()");
    }

    @Override
    public ApiCalls getApiCalls() {
        return serverCloudApi.getApiCalls();
    }

    private static Set<Set<String>> generateCombinations(List<String> values) {
        Set<Set<String>> result = new HashSet<>();
        generateCombinations(values, 0, new LinkedHashSet<>(), result);
        return result;
    }

    private static void generateCombinations(
            List<String> values, int start, Set<String> current, Set<Set<String>> result) {
        if (!current.isEmpty()) {
            result.add(new HashSet<>(current));
        }
        for (int i = start; i < values.size(); i++) {
            current.add(values.get(i));
            generateCombinations(values, i + 1, current, result);
            current.remove(values.get(i));
        }
    }
}
