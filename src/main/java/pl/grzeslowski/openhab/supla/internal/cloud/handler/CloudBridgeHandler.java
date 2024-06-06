package pl.grzeslowski.openhab.supla.internal.cloud.handler;

import static java.math.BigDecimal.ZERO;
import static java.math.RoundingMode.HALF_UP;
import static java.util.Objects.requireNonNull;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.openhab.core.thing.ThingStatus.OFFLINE;
import static org.openhab.core.thing.ThingStatus.ONLINE;
import static org.openhab.core.thing.ThingStatusDetail.CONFIGURATION_ERROR;
import static org.openhab.core.types.RefreshType.REFRESH;
import static pl.grzeslowski.openhab.supla.internal.SuplaBindingConstants.CloudBridgeHandlerConstants.*;
import static pl.grzeslowski.openhab.supla.internal.SuplaBindingConstants.THREAD_POOL_NAME;

import io.swagger.client.ApiException;
import io.swagger.client.model.Channel;
import io.swagger.client.model.ChannelExecuteActionRequest;
import io.swagger.client.model.Device;
import java.math.BigDecimal;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ScheduledFuture;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.common.ThreadPoolManager;
import org.openhab.core.library.types.DateTimeType;
import org.openhab.core.library.types.DecimalType;
import org.openhab.core.library.types.PercentType;
import org.openhab.core.library.types.StringType;
import org.openhab.core.thing.Bridge;
import org.openhab.core.thing.ChannelUID;
import org.openhab.core.thing.Thing;
import org.openhab.core.thing.binding.BaseBridgeHandler;
import org.openhab.core.thing.binding.ThingHandler;
import org.openhab.core.types.Command;
import org.openhab.core.types.RefreshType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.grzeslowski.jsupla.api.internal.ApiClientFactory;
import pl.grzeslowski.openhab.supla.internal.ReadWriteMonad;
import pl.grzeslowski.openhab.supla.internal.cloud.api.*;

@NonNullByDefault
public class CloudBridgeHandler extends BaseBridgeHandler implements IoDevicesCloudApi, ChannelsCloudApi {
    private final Logger logger = LoggerFactory.getLogger(CloudBridgeHandler.class);
    private final ReadWriteMonad<Set<CloudDeviceHandler>> cloudDeviceHandlers = new ReadWriteMonad<>(new HashSet<>());

    @Nullable
    private ScheduledFuture<?> scheduledFuture;

    @Nullable
    private ScheduledFuture<?> scheduledFutureForHandler;

    @Nullable
    private ServerCloudApi serverCloudApi;

    @Nullable
    private IoDevicesCloudApi ioDevicesCloudApi;

    @Nullable
    private ChannelsCloudApi channelsApi;

    public CloudBridgeHandler(final Bridge bridge) {
        super(bridge);
    }

    @Override
    public void initialize() {
        try {
            internalInitialize();
        } catch (Exception ex) {
            updateStatus(OFFLINE, CONFIGURATION_ERROR, "Cannot start server! " + ex.getMessage());
        }
    }

    private void internalInitialize() throws Exception {
        // init bridge api client
        var config = this.getConfigAs(CloudBridgeHandlerConfig.class);

        ServerCloudApi localServerCloudApi;
        try {
            var cacheEvict = config.getCacheEvict();
            if (cacheEvict > 0) {
                var serverCloudApiFactory =
                        new CacheApiFactory(new SwaggerApiFactory(new CloudApiClientFactory()), cacheEvict);
                localServerCloudApi = serverCloudApi = serverCloudApiFactory.newServerCloudApi(config.getOAuthToken());
                ioDevicesCloudApi = serverCloudApiFactory.newIoDevicesCloudApi(config.getOAuthToken());
                channelsApi = serverCloudApiFactory.newChannelsCloudApi(config.getOAuthToken());
            } else {
                var serverCloudApiFactory = new SwaggerApiFactory(new CloudApiClientFactory());
                localServerCloudApi = serverCloudApi = serverCloudApiFactory.newServerCloudApi(config.getOAuthToken());
                ioDevicesCloudApi = serverCloudApiFactory.newIoDevicesCloudApi(config.getOAuthToken());
                channelsApi = serverCloudApiFactory.newChannelsCloudApi(config.getOAuthToken());
            }
        } catch (Exception e) {
            updateStatus(
                    OFFLINE,
                    CONFIGURATION_ERROR,
                    "Cannot create client to Supla Cloud! Probably oAuth token is incorrect! " + e.getMessage());
            return;
        }

        // update channels
        updateServerInfo();
        updateApiCalls();

        // check if current api is supported
        var apiVersion = ApiClientFactory.getApiVersion();
        var serverInfo = localServerCloudApi.getServerInfo();
        List<String> supportedApiVersions = serverInfo.getSupportedApiVersions();
        if (!supportedApiVersions.contains(apiVersion)) {
            updateStatus(
                    OFFLINE,
                    CONFIGURATION_ERROR,
                    "This API version `" + apiVersion + "` is not supported! Supported api versions: ["
                            + String.join(", ", supportedApiVersions) + "].");
            return;
        }

        var scheduledPool = ThreadPoolManager.getScheduledPool(THREAD_POOL_NAME);
        {
            var refreshInterval = config.getRefreshInterval();
            this.scheduledFuture = scheduledPool.scheduleWithFixedDelay(
                    this::refreshCloudDevices, refreshInterval * 2L, refreshInterval, SECONDS);
        }
        {
            var refreshHandlerInterval = config.getRefreshHandlerInterval();
            this.scheduledFutureForHandler = scheduledPool.scheduleWithFixedDelay(
                    this::refreshHandler, refreshHandlerInterval * 2L, refreshHandlerInterval, SECONDS);
        }

        // done
        updateStatus(ONLINE);
    }

    @Override
    public void dispose() {
        logger.debug("Disposing CloudBridgeHandler");
        super.dispose();
        {
            var local = scheduledFuture;
            if (local != null) {
                local.cancel(true);
                scheduledFuture = null;
            }
        }
        {
            var local = scheduledFutureForHandler;
            if (local != null) {
                local.cancel(true);
                scheduledFutureForHandler = null;
            }
        }
    }

    @Override
    public void handleCommand(final ChannelUID channelUID, final Command command) {
        final String channelId = channelUID.getId();
        if (command instanceof RefreshType) {
            switch (channelId) {
                case ADDRESS_CHANNEL_ID, API_VERSION_CHANNEL_ID, CLOUD_VERSION_CHANNEL_ID -> updateServerInfo();
                case API_CALLS_IDH_CHANNEL_ID,
                        REMAINING_API_CALLS_CHANNEL_ID,
                        API_CALLS_ID_PERCENTAGE_CHANNEL_ID,
                        REMAINING_API_CALLS_ID_PERCENTAGE_CHANNEL_ID,
                        RATE_LIMIT_MAX_CHANNEL_ID,
                        RATE_LIMIT_RESET_DATE_TIME_CHANNEL_ID,
                        REQ_PER_S_CHANNEL_ID,
                        REQ_PER_M_CHANNEL_ID,
                        REQ_PER_H_CHANNEL_ID -> updateApiCalls();
            }
        }
    }

    private void updateServerInfo() {
        try {
            var api = this.serverCloudApi;
            if (api != null) {
                var serverInfo = api.getServerInfo();
                updateState(ADDRESS_CHANNEL_ID, new StringType(serverInfo.getAddress()));
                updateState(API_VERSION_CHANNEL_ID, new StringType(serverInfo.getApiVersion()));
                updateState(CLOUD_VERSION_CHANNEL_ID, new StringType(serverInfo.getCloudVersion()));
            } else {
                var msg = "<UNKNOWN>";
                updateState(ADDRESS_CHANNEL_ID, new StringType(msg));
                updateState(API_VERSION_CHANNEL_ID, new StringType(msg));
                updateState(CLOUD_VERSION_CHANNEL_ID, new StringType(msg));
            }
        } catch (Exception e) {
            var msg = e.getLocalizedMessage();
            updateState(ADDRESS_CHANNEL_ID, new StringType(msg));
            updateState(API_VERSION_CHANNEL_ID, new StringType(msg));
            updateState(CLOUD_VERSION_CHANNEL_ID, new StringType(msg));
        }
    }

    private void updateApiCalls() {
        var api = this.serverCloudApi;
        if (api != null) {
            var apiCalls = api.getApiCalls();

            updateState(API_CALLS_IDH_CHANNEL_ID, new DecimalType(apiCalls.apiCalls()));
            updateState(REMAINING_API_CALLS_CHANNEL_ID, new DecimalType(apiCalls.remainingApiCalls()));

            {
                BigDecimal apiCallsPercentage;
                if (apiCalls.limit() > 0) {
                    apiCallsPercentage =
                            new BigDecimal(apiCalls.apiCalls() * 100).divide(new BigDecimal(apiCalls.limit()), HALF_UP);
                } else {
                    apiCallsPercentage = ZERO;
                }
                updateState(API_CALLS_ID_PERCENTAGE_CHANNEL_ID, new PercentType(apiCallsPercentage));
            }

            {
                BigDecimal remainingApiCallsPercentage;
                if (apiCalls.limit() > 0) {
                    remainingApiCallsPercentage = new BigDecimal(apiCalls.remainingApiCalls() * 100)
                            .divide(new BigDecimal(apiCalls.limit()), HALF_UP);
                } else {
                    remainingApiCallsPercentage = ZERO;
                }
                updateState(REMAINING_API_CALLS_ID_PERCENTAGE_CHANNEL_ID, new PercentType(remainingApiCallsPercentage));
            }

            updateState(RATE_LIMIT_MAX_CHANNEL_ID, new DecimalType(apiCalls.limit()));
            updateState(RATE_LIMIT_RESET_DATE_TIME_CHANNEL_ID, new DateTimeType(apiCalls.resetDateTime()));

            updateState(REQ_PER_S_CHANNEL_ID, new DecimalType(apiCalls.requestPerSecond()));
            updateState(REQ_PER_M_CHANNEL_ID, new DecimalType(apiCalls.requestPerMinute()));
            updateState(REQ_PER_H_CHANNEL_ID, new DecimalType(apiCalls.requestPerHour()));
        }
    }

    @Override
    public void childHandlerInitialized(ThingHandler childHandler, Thing childThing) {
        super.childHandlerInitialized(childHandler, childThing);
        if (childHandler instanceof CloudDeviceHandler) {
            logger.trace(
                    "Add `{}` to cloudDeviceHandlers", childHandler.getThing().getUID());
            cloudDeviceHandlers.doInWriteLock(
                    cloudDeviceHandlers -> cloudDeviceHandlers.add((CloudDeviceHandler) childHandler));
        }
    }

    @Override
    public void childHandlerDisposed(ThingHandler childHandler, Thing childThing) {
        super.childHandlerDisposed(childHandler, childThing);
        if (childHandler instanceof CloudDeviceHandler) {
            logger.trace(
                    "Remove `{}` to cloudDeviceHandlers",
                    childHandler.getThing().getUID());
            cloudDeviceHandlers.doInWriteLock(cloudDeviceHandlers -> cloudDeviceHandlers.remove(childHandler));
        }
    }

    private void refreshCloudDevices() {
        try {
            cloudDeviceHandlers.doInReadLock(
                    cloudDeviceHandlers -> cloudDeviceHandlers.forEach(CloudDeviceHandler::refresh));
        } catch (Exception e) {
            logger.error("Cannot refresh cloud devices!", e);
        }
    }

    private void refreshHandler() {
        try {
            this.getThing().getChannels().forEach(channel -> handleCommand(channel.getUID(), REFRESH));
        } catch (Exception e) {
            logger.error("Cannot refresh channels!", e);
        }
    }

    @Override
    public void executeAction(ChannelExecuteActionRequest body, Integer id) throws ApiException {
        requireNonNull(channelsApi).executeAction(body, id);
    }

    @Override
    public Channel getChannel(int id, List<String> include) throws Exception {
        return requireNonNull(channelsApi).getChannel(id, include);
    }

    @Override
    public List<Channel> getChannels(List<String> include) throws Exception {
        return requireNonNull(channelsApi).getChannels(include);
    }

    @Override
    public Device getIoDevice(int id, List<String> include) throws Exception {
        return requireNonNull(ioDevicesCloudApi).getIoDevice(id, include);
    }

    @Override
    public List<Device> getIoDevices(List<String> include) throws Exception {
        return requireNonNull(ioDevicesCloudApi).getIoDevices(include);
    }
}
