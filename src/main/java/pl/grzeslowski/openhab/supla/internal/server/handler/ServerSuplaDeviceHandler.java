package pl.grzeslowski.openhab.supla.internal.server.handler;

import static java.lang.String.valueOf;
import static java.time.Instant.now;
import static java.util.Objects.requireNonNull;
import static java.util.Objects.requireNonNullElse;
import static java.util.concurrent.TimeUnit.SECONDS;
import static java.util.stream.Collectors.joining;
import static org.openhab.core.thing.ThingStatus.OFFLINE;
import static org.openhab.core.thing.ThingStatus.ONLINE;
import static org.openhab.core.thing.ThingStatusDetail.*;
import static pl.grzeslowski.jsupla.protocol.api.ResultCode.SUPLA_RESULTCODE_TRUE;
import static pl.grzeslowski.jsupla.protocol.api.consts.ProtoConsts.SUPLA_PROTO_VERSION_MIN;
import static pl.grzeslowski.openhab.supla.internal.GuidLogger.attachGuid;
import static pl.grzeslowski.openhab.supla.internal.Localization.text;
import static pl.grzeslowski.openhab.supla.internal.SuplaBindingConstants.BINDING_ID;
import static pl.grzeslowski.openhab.supla.internal.SuplaBindingConstants.ServerDevicesProperties.*;
import static pl.grzeslowski.openhab.supla.internal.server.ByteArrayToHex.hexToBytes;

import io.netty.handler.timeout.ReadTimeoutException;
import java.math.BigInteger;
import java.net.SocketException;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.*;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import lombok.Getter;
import lombok.NonNull;
import lombok.ToString;
import lombok.experimental.Delegate;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.common.ThreadPoolManager;
import org.openhab.core.thing.ChannelUID;
import org.openhab.core.thing.Thing;
import org.openhab.core.thing.ThingStatus;
import org.openhab.core.thing.binding.ThingHandlerService;
import org.openhab.core.types.Command;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.grzeslowski.jsupla.protocol.api.structs.SuplaTimeval;
import pl.grzeslowski.jsupla.protocol.api.structs.dcs.LocalTimeRequest;
import pl.grzeslowski.jsupla.protocol.api.structs.dcs.SetCaption;
import pl.grzeslowski.jsupla.protocol.api.structs.dcs.SuplaPingServer;
import pl.grzeslowski.jsupla.protocol.api.structs.dcs.SuplaSetActivityTimeout;
import pl.grzeslowski.jsupla.protocol.api.structs.ds.*;
import pl.grzeslowski.jsupla.protocol.api.structs.dsc.ChannelState;
import pl.grzeslowski.jsupla.protocol.api.structs.sd.SuplaRegisterDeviceResultA;
import pl.grzeslowski.jsupla.protocol.api.structs.sdc.SuplaPingServerResult;
import pl.grzeslowski.jsupla.protocol.api.structs.sdc.SuplaSetActivityTimeoutResult;
import pl.grzeslowski.jsupla.protocol.api.structs.sdc.UserLocalTimeResult;
import pl.grzeslowski.jsupla.protocol.api.types.ToServerProto;
import pl.grzeslowski.jsupla.server.MessageHandler;
import pl.grzeslowski.jsupla.server.SuplaWriter;
import pl.grzeslowski.openhab.supla.internal.GuidLogger.GuidLogged;
import pl.grzeslowski.openhab.supla.internal.handler.InitializationException;
import pl.grzeslowski.openhab.supla.internal.handler.OfflineInitializationException;
import pl.grzeslowski.openhab.supla.internal.handler.SuplaDevice;
import pl.grzeslowski.openhab.supla.internal.server.SuplaServerDeviceActions;
import pl.grzeslowski.openhab.supla.internal.server.cache.InMemoryStateCache;
import pl.grzeslowski.openhab.supla.internal.server.cache.StateCache;
import pl.grzeslowski.openhab.supla.internal.server.device_config.DeviceConfigResult;
import pl.grzeslowski.openhab.supla.internal.server.device_config.DeviceConfigUtil;
import pl.grzeslowski.openhab.supla.internal.server.handler.trait.ServerBridge;
import pl.grzeslowski.openhab.supla.internal.server.netty.OpenHabMessageHandler;
import pl.grzeslowski.openhab.supla.internal.server.oh_config.AuthData;
import pl.grzeslowski.openhab.supla.internal.server.oh_config.DeviceConfiguration;
import pl.grzeslowski.openhab.supla.internal.server.oh_config.ServerDeviceHandlerConfiguration;
import pl.grzeslowski.openhab.supla.internal.server.oh_config.TimeoutConfiguration;
import pl.grzeslowski.openhab.supla.internal.server.traits.DeviceChannelValue;
import pl.grzeslowski.openhab.supla.internal.server.traits.RegisterDeviceTrait;
import pl.grzeslowski.openhab.supla.internal.server.traits.RegisterEmailDeviceTrait;
import pl.grzeslowski.openhab.supla.internal.server.traits.RegisterLocationDeviceTrait;

/** The {@link ServerSuplaDeviceHandler} is responsible for handling commands, which are sent to one of the channels. */
@NonNullByDefault
@ToString(onlyExplicitlyIncluded = true)
public abstract class ServerSuplaDeviceHandler extends SuplaDevice implements MessageHandler {
    public static final byte ACTIVITY_TIMEOUT = (byte) 100;
    public static final String AVAILABLE_FIELDS = "AVAILABLE_FIELDS";
    private static final AtomicLong ID = new AtomicLong();

    private final long id = ID.incrementAndGet();

    @Getter
    protected Logger logger = LoggerFactory.getLogger(baseLogger());

    @ToString.Include
    @Nullable
    @Getter
    private String guid;

    @ToString.Include
    @Nullable
    private DeviceConfiguration deviceConfiguration;

    @ToString.Include
    private boolean authorized = false;

    private final AtomicLong lastMessageFromDevice = new AtomicLong();

    @Nullable
    private ScheduledFuture<?> pingSchedule;

    @Nullable
    @Getter
    private ServerBridge bridgeHandler;

    @Getter
    private final AtomicReference<@Nullable SuplaWriter> writer = new AtomicReference<>();

    private final AtomicReference<@Nullable SetDeviceConfigResult> setDeviceConfigResult = new AtomicReference<>();

    @Delegate(types = StateCache.class)
    private final StateCache stateCache = new InMemoryStateCache(logger);

    @Nullable
    private OpenHabMessageHandler handler;

    public ServerSuplaDeviceHandler(Thing thing) {
        super(thing);
    }

    @Override
    protected void internalInitialize() throws InitializationException {
        var bridge = getBridge();
        if (bridge == null) {
            throw new OfflineInitializationException(BRIDGE_UNINITIALIZED, text("supla.offline.no-bridge"));
        }
        var rawBridgeHandler = bridge.getHandler();
        var bridgeClasses = findAllowedBridgeClasses();
        var bridgeHasCorrectClass = rawBridgeHandler != null
                && bridgeClasses.stream().anyMatch(clazz -> clazz.isInstance(rawBridgeHandler));
        if (!bridgeHasCorrectClass) {
            String simpleName;
            if (rawBridgeHandler != null) {
                simpleName = rawBridgeHandler.getClass().getSimpleName();
            } else {
                simpleName = "<null>";
            }
            var allowedBridgeClasses =
                    bridgeClasses.stream().map(Class::getSimpleName).collect(joining(", ", "[", "]"));
            throw new OfflineInitializationException(
                    CONFIGURATION_ERROR, text("supla.server.bridge-type-wrong", allowedBridgeClasses, simpleName));
        }
        var localBridgeHandler = this.bridgeHandler = (ServerBridge) rawBridgeHandler;

        var config = getConfigAs(ServerDeviceHandlerConfiguration.class);
        guid = config.getGuid();
        if (guid == null || guid.isEmpty()) {
            guid = null;
            throw new OfflineInitializationException(CONFIGURATION_ERROR, text("supla.server.guid-missing"));
        }
        logger = LoggerFactory.getLogger(baseLogger() + "." + guid);

        {
            var timeoutConfiguration = buildTimeoutConfiguration(localBridgeHandler, config);
            var authData = buildAuthData(localBridgeHandler, config);

            deviceConfiguration = new DeviceConfiguration(timeoutConfiguration, authData);
        }

        clearDeviceConfig();

        updateStatus(ThingStatus.UNKNOWN, HANDLER_CONFIGURATION_PENDING, text("supla.server.waiting-for-connection"));
    }

    @Override
    protected @Nullable String findGuid() {
        if (guid != null) {
            return guid;
        }
        var config = getConfigAs(ServerDeviceHandlerConfiguration.class);
        var guid = config.getGuid();
        if (guid == null || guid.isEmpty()) {
            return null;
        }
        return guid;
    }

    protected AuthData buildAuthData(ServerBridge localBridgeHandler, ServerDeviceHandlerConfiguration config)
            throws OfflineInitializationException {
        var bridgeAuthData = requireNonNull(localBridgeHandler.getAuthData(), "No auth data in bridge!");
        AuthData.@Nullable LocationAuthData locationAuthData;
        if (config.getServerAccessId() != null && config.getServerAccessIdPassword() != null) {
            locationAuthData = new AuthData.LocationAuthData(
                    config.getServerAccessId().intValue(), config.getServerAccessIdPassword());
        } else {
            locationAuthData = bridgeAuthData.locationAuthData();
        }
        AuthData.@Nullable EmailAuthData emailAuthData;
        var configEmail = config.getEmail();
        var configAuthKey = config.getAuthKey();
        var bridgeEmailAuthData = bridgeAuthData.emailAuthData();

        if ((configEmail == null || configAuthKey == null) && bridgeEmailAuthData == null) {
            emailAuthData = null;
        } else {
            String email;
            //noinspection ReplaceNullCheck
            if (configEmail != null) {
                email = configEmail;
            } else {
                email = bridgeEmailAuthData.email();
            }
            String authKey;
            if (configAuthKey != null) {
                authKey = configAuthKey;
            } else {
                authKey = bridgeEmailAuthData.authKey();
            }
            emailAuthData = new AuthData.EmailAuthData(email, authKey);
        }
        if (locationAuthData == null && emailAuthData == null) {
            throw new OfflineInitializationException(
                    CONFIGURATION_ERROR, "You need to configure location authorization and/or email authorization!");
        }
        return new AuthData(locationAuthData, emailAuthData);
    }

    protected TimeoutConfiguration buildTimeoutConfiguration(
            ServerBridge localBridgeHandler, ServerDeviceHandlerConfiguration config) {
        var bridgeHandlerTimeoutConfiguration =
                requireNonNullElse(localBridgeHandler.getTimeoutConfiguration(), new TimeoutConfiguration(10, 8, 12));

        return new TimeoutConfiguration(
                requireNonNullElse(config.getTimeout(), bridgeHandlerTimeoutConfiguration.timeout()),
                requireNonNullElse(config.getTimeoutMin(), bridgeHandlerTimeoutConfiguration.min()),
                requireNonNullElse(config.getTimeoutMax(), bridgeHandlerTimeoutConfiguration.max()));
    }

    protected abstract List<Class<? extends ServerBridge>> findAllowedBridgeClasses();

    @Override
    public void handle(ToServerProto entity) {
        var writer = this.writer.get();
        if (writer == null) {
            logger.warn("The write channel is not active, messages should not be incoming!");
            return;
        }
        try {
            switch (entity) {
                case SuplaPingServer ping -> consumeSuplaPingServer(ping, writer);
                case SuplaSetActivityTimeout suplaSetActivityTimeout -> consumeSuplaSetActivityTimeout(writer);
                case SuplaDeviceChannelValueA value -> consumeDeviceChannelValueTrait(
                        DeviceChannelValue.fromProto(value));
                case SuplaDeviceChannelValueB value -> consumeDeviceChannelValueTrait(
                        DeviceChannelValue.fromProto(value));
                case SuplaDeviceChannelValueC value -> consumeDeviceChannelValueTrait(
                        DeviceChannelValue.fromProto(value));
                case SuplaDeviceChannelExtendedValue value -> {
                    var extendedValue = value.value();
                    consumeSuplaDeviceChannelExtendedValue(
                            value.channelNumber(), extendedValue.type(), extendedValue.value());
                }
                case LocalTimeRequest value -> consumeLocalTimeRequest(writer);
                case SetCaption value -> consumeSetCaption(value);
                case ChannelState value -> consumeChannelState(value);
                case SubdeviceDetails value -> consumeSubDeviceDetails(value);
                case SuplaChannelNewValueResult value -> consumeSuplaChannelNewValueResult(value);
                case SetDeviceConfigResult value -> consumeSetDeviceConfigResult(value);
                case SetDeviceConfig value -> consumeSetDeviceConfig(value);
                case SetChannelConfigResult value -> consumeSetChannelConfigResult(value);
                default -> logger.debug("Not supporting message: {}", entity);
            }
            updateStatus(ONLINE);
        } catch (Exception ex) {
            logger.error("Error in message pipeline", ex);
            var message = ex.getLocalizedMessage();
            updateStatus(OFFLINE, COMMUNICATION_ERROR, text("supla.offline.message-pipeline", message));
        }
    }

    @Override
    public void active(SuplaWriter writer) {
        this.writer.set(writer);
    }

    @Override
    public void inactive() {
        updateStatus(OFFLINE, COMMUNICATION_ERROR, text("supla.offline.channel-disconnected"));
        this.writer.set(null);
        dispose();
    }

    public void register(@NonNull RegisterDeviceTrait registerEntity, OpenHabMessageHandler handler)
            throws InitializationException {
        updateStatus(OFFLINE, HANDLER_CONFIGURATION_PENDING, text("supla.offline.device-authorizing"));
        var oldHandler = this.handler;
        if (oldHandler != null) {
            logger.warn(
                    "Having old handler. Should not happen! handler={}#{}",
                    oldHandler.getClass().getSimpleName(),
                    oldHandler.hashCode());
            oldHandler.clear();
        }
        this.handler = handler;
        disposePing();

        // auth
        logger.debug("Authorizing...");
        authorize(registerEntity);
        authorized = true;
        logger.debug("Authorized!");
        {
            var local = bridgeHandler;
            if (local != null) {
                local.deviceConnected();
            }
        }

        // set properties
        thing.setProperty(SOFT_VERSION_PROPERTY, registerEntity.softVer());
        if (registerEntity.manufacturerId() != null) {
            thing.setProperty(MANUFACTURER_ID_PROPERTY, valueOf(registerEntity.manufacturerId()));
        }
        if (registerEntity.productId() != null) {
            thing.setProperty(PRODUCT_ID_PROPERTY, valueOf(registerEntity.productId()));
        }

        afterRegister(registerEntity);
        var w = requireNonNull(getWriter().get(), "There is no writer!");
        // TODO should I send version A or B?
        w.write(new SuplaRegisterDeviceResultA(
                SUPLA_RESULTCODE_TRUE.getValue(), ACTIVITY_TIMEOUT, (byte) w.getVersion(), (byte)
                        SUPLA_PROTO_VERSION_MIN));
        // thing will have status ONLINE after receiving proto from the device (method `handle(ToServerProto)`)
        updateStatus(ThingStatus.UNKNOWN, CONFIGURATION_PENDING, text("supla.offline.waiting-for-registration"));
        lastMessageFromDevice.set(now().getEpochSecond());
    }

    private void authorize(RegisterDeviceTrait registerEntity) throws InitializationException {
        switch (registerEntity) {
            case RegisterLocationDeviceTrait registerDevice -> authorizeForLocation(
                    registerDevice.locationId(), registerDevice.locationPwd());
            case RegisterEmailDeviceTrait registerDevice -> authorizeForEmail(
                    registerDevice.email(), registerDevice.authKey());
        }
    }

    @GuidLogged
    protected abstract void afterRegister(RegisterDeviceTrait registerEntity) throws InitializationException;

    public final void consumeSuplaSetActivityTimeout(SuplaWriter writer) {
        var timeout = requireNonNull(deviceConfiguration).timeoutConfiguration();
        var data = new SuplaSetActivityTimeoutResult(
                (short) timeout.timeout(), (short) timeout.min(), (short) timeout.max());
        writer.write(data).addListener(f -> logger.trace("setActivityTimeout {}", data));
        {
            var local = pingSchedule;
            if (local != null) {
                logger.warn("Ping schedule was not set to null!");
                local.cancel(true);
            }
        }
        pingSchedule = ThreadPoolManager.getScheduledPool(BINDING_ID)
                .scheduleWithFixedDelay(this::checkIfDeviceIsUp, timeout.timeout() * 2L, timeout.timeout(), SECONDS);
    }

    public final void consumeLocalTimeRequest(SuplaWriter writer) {
        // Get current local date and time
        var now = LocalDateTime.now();

        // Extract year, month, day, etc.
        var year = now.getYear();
        var month = (short) now.getMonthValue();
        var day = (short) now.getDayOfMonth();
        // 1 = Sunday, 2 = Monday, …, 7 = Saturday
        var dayOfWeek = (short) ((now.getDayOfWeek().getValue() + 1) % 7);
        var hour = (short) now.getHour();
        var minute = (short) now.getMinute();
        var seconds = (short) now.getSecond();

        // Get the system's default time zone
        var zoneId = ZoneId.systemDefault();
        var timeZoneName = zoneId.getDisplayName(TextStyle.SHORT, Locale.getDefault());
        var timeZone = timeZoneName.getBytes(); // Convert to byte array
        var timeZoneSize = timeZone.length;

        var proto = new UserLocalTimeResult(year, month, day, dayOfWeek, hour, minute, seconds, timeZoneSize, timeZone);
        logger.debug("Setting local time to {}", proto);
        writer.write(proto);
    }

    public final void consumeSuplaPingServer(SuplaPingServer ping, SuplaWriter writer) {
        var epochSecond = now().getEpochSecond();
        var response = new SuplaPingServerResult(new SuplaTimeval(epochSecond, 0));
        writer.write(response).addListener(f -> {
            var millis =
                    SECONDS.toMillis(response.now().tvSec()) + response.now().tvUsec();
            var formatter = DateTimeFormatter.ofPattern("HH:mm:ss.SSS z");
            var date = Instant.ofEpochMilli(millis).atZone(ZoneId.of("UTC")).withZoneSameLocal(ZoneId.systemDefault());
            logger.trace(
                    "pingServer {} ({}s {}ms)",
                    formatter.format(date),
                    response.now().tvSec(),
                    response.now().tvUsec());
            lastMessageFromDevice.set(epochSecond);
        });
    }

    private void authorizeForLocation(int accessId, byte[] accessIdPassword) throws InitializationException {
        var locationAuthData = Optional.ofNullable(deviceConfiguration)
                .map(DeviceConfiguration::authData)
                .map(AuthData::locationAuthData)
                .orElseThrow(() ->
                        new OfflineInitializationException(CONFIGURATION_ERROR, "No location authorization data!"));
        if (locationAuthData.serverAccessId() != accessId) {
            throw new OfflineInitializationException(
                    CONFIGURATION_ERROR,
                    "Wrong access ID! Expected %s but got %s.".formatted(locationAuthData.serverAccessId(), accessId));
        }
        if (!isGoodPassword(locationAuthData.serverAccessIdPassword().toCharArray(), accessIdPassword)) {
            throw new OfflineInitializationException(CONFIGURATION_ERROR, "Wrong location password!");
        }
    }

    private boolean isGoodPassword(char[] password, byte[] givenPassword) {
        if (password.length > givenPassword.length) {
            return false;
        }
        for (int i = 0; i < password.length; i++) {
            if (password[i] != givenPassword[i]) {
                return false;
            }
        }
        return true;
    }

    private void authorizeForEmail(String email, byte[] authKey) throws InitializationException {
        var emailAuthData = Optional.ofNullable(deviceConfiguration)
                .map(DeviceConfiguration::authData)
                .map(AuthData::emailAuthData)
                .orElseThrow(
                        () -> new OfflineInitializationException(CONFIGURATION_ERROR, "No email authentication data!"));
        if (!emailAuthData.email().equals(email)) {
            throw new OfflineInitializationException(
                    CONFIGURATION_ERROR,
                    "Wrong email! Expected %s but got %s.".formatted(email, emailAuthData.email()));
        }
        var key = emailAuthData.authKey();
        if (key == null) {
            throw new OfflineInitializationException(CONFIGURATION_ERROR, "Missing email auth key!");
        }
        var byteKey = hexToBytes(key);
        if (!Arrays.equals(byteKey, authKey)) {
            throw new OfflineInitializationException(CONFIGURATION_ERROR, "Wrong email auth key!");
        }
    }

    private void checkIfDeviceIsUp() {
        var localDeviceConfiguration = deviceConfiguration;
        if (localDeviceConfiguration == null) {
            return;
        }
        var timeout = localDeviceConfiguration.timeoutConfiguration();
        var now = now().getEpochSecond();
        var lastPing = lastMessageFromDevice.get();
        var delta = now - lastPing;
        if (delta > timeout.max()) {
            var lastPingDate = new Date(SECONDS.toMillis(lastPing));
            var formatter = new SimpleDateFormat("HH:mm:ss z");
            updateStatus(
                    OFFLINE, COMMUNICATION_ERROR, text("supla.offline.no-ping", delta, formatter.format(lastPingDate)));
            disposePing();
        }
    }

    private ChannelUID createChannelUid(int channelNumber) {
        return new ChannelUID(getThing().getUID(), valueOf(channelNumber));
    }

    @GuidLogged
    @Override
    public void dispose() {
        attachGuid(findGuid(), () -> {
            logger.debug("Disposing handler");
            disposePing();
            disposeHandler();
            disposeBridgeHandler();
            writer.set(null);
            logger = LoggerFactory.getLogger(baseLogger());
            authorized = false;
        });
    }

    private void disposePing() {
        var local = pingSchedule;
        pingSchedule = null;
        if (local != null) {
            local.cancel(true);
        }
        lastMessageFromDevice.set(0);
    }

    private void disposeHandler() {
        var localHandler = handler;
        handler = null;
        if (localHandler != null) {
            localHandler.clear();
        }
    }

    private void disposeBridgeHandler() {
        var local = bridgeHandler;
        bridgeHandler = null;
        if (local != null && authorized) {
            local.deviceDisconnected();
        }
    }

    @Override
    public Collection<Class<? extends ThingHandlerService>> getServices() {
        return Set.of(SuplaServerDeviceActions.class);
    }

    public void consumeSetDeviceConfigResult(SetDeviceConfigResult value) {
        var result = DeviceConfigResult.findConfigResult(value.result());
        if (!result.isSuccess()) {
            logger.warn("Did not succeed ({}) with setting config for device", result);
        } else {
            logger.debug("Set config for device. result={}", value);
        }
        var previous = setDeviceConfigResult.getAndSet(value);
        if (previous != null) {
            logger.warn(
                    "Previous setDeviceConfigResult was not null. Wierd thing might happen! " + "previous={}",
                    previous);
        }
    }

    public synchronized SetDeviceConfigResult listenForSetDeviceConfigResult(long maxTime, TimeUnit timeUnit)
            throws InterruptedException, TimeoutException {
        var maxMillis = timeUnit.toMillis(maxTime);
        var started = System.currentTimeMillis();
        var sleep = Duration.ofSeconds(1L);
        SetDeviceConfigResult result;
        do {
            result = setDeviceConfigResult.get();
            if (result == null) {
                var now = System.currentTimeMillis();
                if (now > started + maxMillis) {
                    break;
                }
                Thread.sleep(sleep.toMillis());
            }
        } while (result == null);
        if (result == null) {
            throw new TimeoutException("Did not get SetDeviceConfigResult in " + Duration.ofMillis(maxMillis));
        }
        setDeviceConfigResult.set(null);
        return result;
    }

    public void consumeSetChannelConfigResult(SetChannelConfigResult value) {
        var result = DeviceConfigResult.findConfigResult(value.result());
        if (!result.isSuccess()) {
            logger.warn("Did not succeed ({}) with setting config for device", result);
        } else {
            logger.debug("Set config for channel. result={}", value);
        }
    }

    public void consumeSetDeviceConfig(SetDeviceConfig value) {
        if (value.endOfDataFlag() == 0) {
            logger.warn("SetDeviceConfig has more data but I'm not supporting it! config={}", value);
            return;
        }
        setAvailableFields(value.availableFields());
        consumeSetDeviceConfig(value.fields().longValue(), value.config());
    }

    public void consumeSetDeviceConfig(long fields, byte[] config) {
        var map = DeviceConfigUtil.buildDeviceConfig(fields, config);
        logger.debug("Setting device config to: {}", map);
        map.forEach((k, v) -> thing.setProperty(k, v));
    }

    private void clearDeviceConfig() {
        // remove all properties with prefix `DEVICE_CONFIG_`
        thing.getProperties().keySet().stream()
                .filter(key -> key.startsWith(DeviceConfigUtil.PREFIX))
                .forEach(key -> thing.setProperty(key, null));
    }

    @Nullable
    public BigInteger getAvailableFields() {
        var af = thing.getProperties().get(AVAILABLE_FIELDS);
        if (af != null) {
            try {
                return new BigInteger(af);
            } catch (NumberFormatException ex) {
                logger.debug("Cannot parse BigInteger from " + af, ex);
            }
        }
        return null;
    }

    public void setAvailableFields(@Nullable BigInteger availableFields) {
        thing.setProperty(
                AVAILABLE_FIELDS,
                Optional.ofNullable(availableFields)
                        .map(BigInteger::longValue)
                        .map(Objects::toString)
                        .orElse(null));
    }

    @GuidLogged
    @Override
    public void socketException(Throwable exception) {
        var text =
                switch (exception) {
                    case ReadTimeoutException readTimeoutException -> {
                        logger.warn("Got timeout from socket. Going offline");
                        yield text("supla.offline.socket-timeout");
                    }
                    case SocketException socketException -> {
                        logger.warn("Got socket exception from socket. Going offline");
                        yield text("supla.offline.socket-exception", exception.getLocalizedMessage());
                    }
                    default -> {
                        logger.warn("Got exception from socket. Going offline", exception);
                        yield text(
                                "supla.offline.socket-generic",
                                exception.getClass().getSimpleName(),
                                exception.getLocalizedMessage());
                    }
                };
        updateStatus(OFFLINE, COMMUNICATION_ERROR, text);
        disposePing();
    }

    private String baseLogger() {
        return this.getClass().getName() + "#" + id;
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
        if (!authorized) {
            logger.warn("Not handling command {} on channel {}, because device is not authorize!", command, channelUID);
            return;
        }
        super.handleCommand(channelUID, command);
    }

    abstract void consumeDeviceChannelValueTrait(DeviceChannelValue trait);

    abstract void consumeSuplaDeviceChannelExtendedValue(int channelNumber, int type, byte[] value);

    abstract void consumeSetCaption(SetCaption value);

    abstract void consumeChannelState(ChannelState value);

    abstract void consumeSubDeviceDetails(SubdeviceDetails value);

    abstract void consumeSuplaChannelNewValueResult(SuplaChannelNewValueResult value);
}
