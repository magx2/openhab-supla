package pl.grzeslowski.supla.openhab.internal.cloud.api;

import static java.util.Objects.requireNonNull;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import org.eclipse.jdt.annotation.NonNullByDefault;

@NonNullByDefault
public class CacheApiFactory implements ChannelsCloudApiFactory, IoDevicesCloudApiFactory, ServerCloudApiFactory {
    private final Map<String, CacheApi> tokenToApi = Collections.synchronizedMap(new HashMap<>());
    private final ChannelsCloudApiFactory channelsCloudApiFactory;
    private final IoDevicesCloudApiFactory ioDevicesCloudApiFactory;
    private final ServerCloudApiFactory serverCloudApiFactory;
    private final int cacheEvictTime;

    public CacheApiFactory(
            ChannelsCloudApiFactory channelsCloudApiFactory,
            IoDevicesCloudApiFactory ioDevicesCloudApiFactory,
            ServerCloudApiFactory serverCloudApiFactory,
            int cacheEvictTime) {
        this.channelsCloudApiFactory = channelsCloudApiFactory;
        this.ioDevicesCloudApiFactory = ioDevicesCloudApiFactory;
        this.serverCloudApiFactory = serverCloudApiFactory;
        this.cacheEvictTime = cacheEvictTime;
    }

    public CacheApiFactory(SwaggerApiFactory factory, int cacheEvictTime) {
        this(factory, factory, factory, cacheEvictTime);
    }

    @Override
    public ChannelsCloudApi newChannelsCloudApi(final String token) {
        return requireNonNull(tokenToApi.computeIfAbsent(
                token,
                t -> new CacheApi(
                        cacheEvictTime,
                        channelsCloudApiFactory.newChannelsCloudApi(t),
                        ioDevicesCloudApiFactory.newIoDevicesCloudApi(t),
                        serverCloudApiFactory.newServerCloudApi(t))));
    }

    @Override
    public IoDevicesCloudApi newIoDevicesCloudApi(final String token) {
        return requireNonNull(tokenToApi.computeIfAbsent(
                token,
                t -> new CacheApi(
                        cacheEvictTime,
                        channelsCloudApiFactory.newChannelsCloudApi(t),
                        ioDevicesCloudApiFactory.newIoDevicesCloudApi(t),
                        serverCloudApiFactory.newServerCloudApi(t))));
    }

    @Override
    public ServerCloudApi newServerCloudApi(final String token) {
        return requireNonNull(tokenToApi.computeIfAbsent(
                token,
                t -> new CacheApi(
                        cacheEvictTime,
                        channelsCloudApiFactory.newChannelsCloudApi(t),
                        ioDevicesCloudApiFactory.newIoDevicesCloudApi(t),
                        serverCloudApiFactory.newServerCloudApi(t))));
    }
}
