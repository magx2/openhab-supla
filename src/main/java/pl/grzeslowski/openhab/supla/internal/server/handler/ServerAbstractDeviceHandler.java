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
import static pl.grzeslowski.jsupla.protocol.api.ProtocolHelpers.parseString;
import static pl.grzeslowski.jsupla.protocol.api.ResultCode.SUPLA_RESULTCODE_TRUE;
import static pl.grzeslowski.jsupla.protocol.api.consts.ProtoConsts.SUPLA_PROTO_VERSION_MIN;
import static pl.grzeslowski.openhab.supla.internal.SuplaBindingConstants.BINDING_ID;
import static pl.grzeslowski.openhab.supla.internal.SuplaBindingConstants.ServerDevicesProperties.*;
import static pl.grzeslowski.openhab.supla.internal.server.ByteArrayToHex.bytesToHex;
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
import pl.grzeslowski.jsupla.protocol.api.structs.sd.SuplaRegisterDeviceResult;
import pl.grzeslowski.jsupla.protocol.api.structs.sdc.SuplaPingServerResult;
import pl.grzeslowski.jsupla.protocol.api.structs.sdc.SuplaSetActivityTimeoutResult;
import pl.grzeslowski.jsupla.protocol.api.structs.sdc.UserLocalTimeResult;
import pl.grzeslowski.jsupla.protocol.api.types.ToServerProto;
import pl.grzeslowski.jsupla.server.api.Writer;
import pl.grzeslowski.openhab.supla.internal.handler.AbstractDeviceHandler;
import pl.grzeslowski.openhab.supla.internal.handler.InitializationException;
import pl.grzeslowski.openhab.supla.internal.handler.OfflineInitializationException;
import pl.grzeslowski.openhab.supla.internal.server.SuplaServerDeviceActions;
import pl.grzeslowski.openhab.supla.internal.server.handler.device_config.DeviceConfigResult;
import pl.grzeslowski.openhab.supla.internal.server.handler.device_config.DeviceConfigUtil;
import pl.grzeslowski.openhab.supla.internal.server.traits.DeviceChannelValueTrait;
import pl.grzeslowski.openhab.supla.internal.server.traits.RegisterDeviceTrait;
import pl.grzeslowski.openhab.supla.internal.server.traits.RegisterEmailDeviceTrait;
import pl.grzeslowski.openhab.supla.internal.server.traits.RegisterLocationDeviceTrait;

/**
 * The {@link ServerAbstractDeviceHandler} is responsible for handling commands, which are sent to one of the channels.
 */
@NonNullByDefault
@ToString(onlyExplicitlyIncluded = true)
public abstract class ServerAbstractDeviceHandler extends AbstractDeviceHandler implements SuplaThing, HandleProto {
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
    private SuplaBridge bridgeHandler;

    @Getter
    private final AtomicReference<@Nullable Writer> writer = new AtomicReference<>();

    private final AtomicReference<@Nullable SetDeviceConfigResult> setDeviceConfigResult = new AtomicReference<>();

    @Delegate(types = StateCache.class)
    private final StateCache stateCache = new InMemoryStateCache(logger);

    @Nullable
    private OpenHabMessageHandler handler;

    public ServerAbstractDeviceHandler(Thing thing) {
        super(thing);
    }

    @Override
    protected void internalInitialize() throws InitializationException {
        var bridge = getBridge();
        if (bridge == null) {
            throw new OfflineInitializationException(
                    BRIDGE_UNINITIALIZED, "There is no bridge for this thing. Remove it and add it again.");
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
                    CONFIGURATION_ERROR,
                    "Bridge has wrong type! Should be one of:" + allowedBridgeClasses + ", but was " + simpleName);
        }
        var localBridgeHandler = this.bridgeHandler = (SuplaBridge) rawBridgeHandler;

        var config = getConfigAs(ServerDeviceHandlerConfiguration.class);
        guid = config.getGuid();
        if (guid == null || guid.isEmpty()) {
            guid = null;
            throw new OfflineInitializationException(CONFIGURATION_ERROR, "There is no guid for this thing.");
        }
        logger = LoggerFactory.getLogger(baseLogger() + "." + guid);

        {
            var timeoutConfiguration = buildTimeoutConfiguration(localBridgeHandler, config);
            var authData = buildAuthData(localBridgeHandler, config);

            deviceConfiguration = new DeviceConfiguration(timeoutConfiguration, authData);
        }

        clearDeviceConfig();

        updateStatus(
                ThingStatus.UNKNOWN,
                HANDLER_CONFIGURATION_PENDING,
                "Waiting for Supla device to connect with the server");
    }

    protected AuthData buildAuthData(SuplaBridge localBridgeHandler, ServerDeviceHandlerConfiguration config) {
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
        return new AuthData(locationAuthData, emailAuthData);
    }

    protected TimeoutConfiguration buildTimeoutConfiguration(
            SuplaBridge localBridgeHandler, ServerDeviceHandlerConfiguration config) {
        var bridgeHandlerTimeoutConfiguration =
                requireNonNullElse(localBridgeHandler.getTimeoutConfiguration(), new TimeoutConfiguration(10, 8, 12));

        return new TimeoutConfiguration(
                requireNonNullElse(config.getTimeout(), bridgeHandlerTimeoutConfiguration.timeout()),
                requireNonNullElse(config.getTimeoutMin(), bridgeHandlerTimeoutConfiguration.min()),
                requireNonNullElse(config.getTimeoutMax(), bridgeHandlerTimeoutConfiguration.max()));
    }

    protected abstract List<Class<? extends SuplaBridge>> findAllowedBridgeClasses();

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
                case SuplaDeviceChannelValue value -> consumeDeviceChannelValueTrait(
                        new DeviceChannelValueTrait(value));
                case SuplaDeviceChannelValueB value -> consumeDeviceChannelValueTrait(
                        new DeviceChannelValueTrait(value));
                case SuplaDeviceChannelValueC value -> consumeDeviceChannelValueTrait(
                        new DeviceChannelValueTrait(value));
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
            updateStatus(OFFLINE, COMMUNICATION_ERROR, "Error in message pipeline. " + message);
        }
    }

    @Override
    public void active(Writer writer) {
        this.writer.set(writer);
    }

    @Override
    public void inactive() {
        updateStatus(OFFLINE, COMMUNICATION_ERROR, "Channel disconnected");
        this.writer.set(null);
        dispose();
    }

    @Override
    public boolean register(@NonNull RegisterDeviceTrait registerEntity, OpenHabMessageHandler handler) {
        updateStatus(OFFLINE, HANDLER_CONFIGURATION_PENDING, "Device is authorizing...");
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
        authorized = authorize(registerEntity);
        if (!authorized) {
            updateStatus(OFFLINE, CONFIGURATION_ERROR, findNonAuthMessage(registerEntity));
            return false;
        }
        logger.debug("Authorized!");
        {
            var local = bridgeHandler;
            if (local != null) {
                local.deviceConnected();
            }
        }

        // set properties
        thing.setProperty(SOFT_VERSION_PROPERTY, registerEntity.getSoftVer());
        if (registerEntity.getManufacturerId() != null) {
            thing.setProperty(MANUFACTURER_ID_PROPERTY, valueOf(registerEntity.getManufacturerId()));
        }
        if (registerEntity.getProductId() != null) {
            thing.setProperty(PRODUCT_ID_PROPERTY, valueOf(registerEntity.getProductId()));
        }

        var register = afterRegister(registerEntity);
        logger.debug("register={}", register);
        if (register) {
            var w = requireNonNull(getWriter().get(), "There is no writer!");
            w.write(new SuplaRegisterDeviceResult(
                    SUPLA_RESULTCODE_TRUE.getValue(), ACTIVITY_TIMEOUT, (byte) w.getVersion(), (byte)
                            SUPLA_PROTO_VERSION_MIN));
            // thing will have status ONLINE after receiving proto from the device (method `handle(ToServerProto)`)
            updateStatus(
                    ThingStatus.UNKNOWN,
                    CONFIGURATION_PENDING,
                    "Waiting for registration confirmation from the device...");
            lastMessageFromDevice.set(now().getEpochSecond());
        }
        return register;
    }

    private static String findNonAuthMessage(RegisterDeviceTrait registerEntity) {
        return switch (registerEntity) {
            case RegisterLocationDeviceTrait
            registerDevice -> "Device authorization failed. Device tried to log in with locationId=%s and locationPassword=%s"
                    .formatted(registerDevice.getLocationId(), parseString(registerDevice.getLocationPwd()));
            case RegisterEmailDeviceTrait
            registerDevice -> "Device authorization failed. Device tried to log in with email=%s and authKey=%s"
                    .formatted(registerDevice.getEmail(), bytesToHex(registerDevice.getAuthKey()));
        };
    }

    private boolean authorize(RegisterDeviceTrait registerEntity) {
        return switch (registerEntity) {
            case RegisterLocationDeviceTrait registerDevice -> authorizeForLocation(
                    registerDevice.getLocationId(), registerDevice.getLocationPwd());
            case RegisterEmailDeviceTrait registerDevice -> authorizeForEmail(
                    registerDevice.getEmail(), registerDevice.getAuthKey());
        };
    }

    protected abstract boolean afterRegister(RegisterDeviceTrait registerEntity);

    @Override
    public final void consumeSuplaSetActivityTimeout(Writer writer) {
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

    @Override
    public final void consumeLocalTimeRequest(Writer writer) {
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

    @Override
    public final void consumeSuplaPingServer(SuplaPingServer ping, Writer writer) {
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

    private boolean authorizeForLocation(int accessId, byte[] accessIdPassword) {
        var localDeviceConfiguration = deviceConfiguration;
        if (localDeviceConfiguration == null) {
            return false;
        }
        var locationAuthData = localDeviceConfiguration.authData().locationAuthData();
        if (locationAuthData == null) {
            // not using access id authorization
            return false;
        }
        if (locationAuthData.serverAccessId() != accessId) {
            logger.debug("Wrong accessId {} != {}", accessId, locationAuthData.serverAccessId());
            return false;
        }
        if (!isGoodPassword(locationAuthData.serverAccessIdPassword().toCharArray(), accessIdPassword)) {
            logger.debug("Wrong accessIdPassword");
            return false;
        }
        return true;
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

    private boolean authorizeForEmail(String email, byte[] authKey) {
        var localDeviceConfiguration = deviceConfiguration;
        if (localDeviceConfiguration == null) {
            return false;
        }
        var emailAuthData = localDeviceConfiguration.authData().emailAuthData();
        if (emailAuthData == null) {
            // not using email authorization
            return false;
        }
        if (!emailAuthData.email().equals(email)) {
            logger.debug("Wrong email; {} != {}", email, emailAuthData.email());
            return false;
        }
        var key = emailAuthData.authKey();
        if (key == null) {
            logger.debug("Device is missing {} property", CONFIG_AUTH_PROPERTY);
            return false;
        }
        var byteKey = hexToBytes(key);
        if (!Arrays.equals(byteKey, authKey)) {
            logger.debug("Wrong auth key; {} != {}", bytesToHex(authKey), key);
            return false;
        }
        return true;
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
                    OFFLINE,
                    COMMUNICATION_ERROR,
                    "Device did not send ping message in last " + delta + " seconds. Last message was from "
                            + formatter.format(lastPingDate));
            dispose();
        }
    }

    private ChannelUID createChannelUid(int channelNumber) {
        return new ChannelUID(getThing().getUID(), valueOf(channelNumber));
    }

    @Override
    public void dispose() {
        logger.debug("Disposing handler");
        disposePing();
        disposeHandler();
        disposeBridgeHandler();
        writer.set(null);
        logger = LoggerFactory.getLogger(baseLogger());
        authorized = false;
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

    @Override
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

    @Override
    public void consumeSetChannelConfigResult(SetChannelConfigResult value) {
        var result = DeviceConfigResult.findConfigResult(value.result());
        if (!result.isSuccess()) {
            logger.warn("Did not succeed ({}) with setting config for device", result);
        } else {
            logger.debug("Set config for channel. result={}", value);
        }
    }

    @Override
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

    @Override
    public void socketException(Throwable exception) {
        String text;
        if (exception instanceof ReadTimeoutException) {
            logger.warn("Got timeout from socket. Going offline");
            text = "Socket timeout";
        } else if (exception instanceof SocketException) {
            logger.warn("Got socket exception from socket. Going offline");
            text = "Socket exception. " + exception.getLocalizedMessage();
        } else {
            logger.warn("Got exception from socket. Going offline", exception);
            text = "%s: %s".formatted(exception.getClass().getSimpleName(), exception.getLocalizedMessage());
        }
        updateStatus(OFFLINE, COMMUNICATION_ERROR, text);
        dispose();
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
}
