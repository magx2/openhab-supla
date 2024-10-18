package pl.grzeslowski.openhab.supla.internal.server.handler;

import static java.lang.String.valueOf;
import static java.time.Instant.now;
import static java.util.Objects.requireNonNull;
import static java.util.Objects.requireNonNullElse;
import static java.util.concurrent.TimeUnit.SECONDS;
import static java.util.stream.Collectors.joining;
import static lombok.AccessLevel.PROTECTED;
import static org.openhab.core.thing.ThingStatus.OFFLINE;
import static org.openhab.core.thing.ThingStatus.ONLINE;
import static org.openhab.core.thing.ThingStatusDetail.*;
import static pl.grzeslowski.jsupla.protocol.api.ProtocolHelpers.parseString;
import static pl.grzeslowski.jsupla.protocol.api.ResultCode.SUPLA_RESULTCODE_TRUE;
import static pl.grzeslowski.openhab.supla.internal.SuplaBindingConstants.BINDING_ID;
import static pl.grzeslowski.openhab.supla.internal.SuplaBindingConstants.ServerDevicesProperties.*;
import static pl.grzeslowski.openhab.supla.internal.server.ByteArrayToHex.bytesToHex;
import static pl.grzeslowski.openhab.supla.internal.server.ByteArrayToHex.hexToBytes;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.TextStyle;
import java.util.*;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import lombok.Getter;
import lombok.ToString;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.common.ThreadPoolManager;
import org.openhab.core.library.types.*;
import org.openhab.core.thing.ChannelUID;
import org.openhab.core.thing.Thing;
import org.openhab.core.thing.ThingStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.grzeslowski.jsupla.protocol.api.channeltype.value.*;
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
import pl.grzeslowski.openhab.supla.internal.server.traits.*;

/**
 * The {@link ServerAbstractDeviceHandler} is responsible for handling commands, which are sent to one of the channels.
 */
@NonNullByDefault
@ToString(onlyExplicitlyIncluded = true)
public abstract class ServerAbstractDeviceHandler extends AbstractDeviceHandler implements SuplaThing, HandleProto {
    public static final byte ACTIVITY_TIMEOUT = (byte) 100;
    public static final byte VERSION = (byte) 6;
    public static final byte VERSION_MIN = (byte) 1;

    @Getter
    protected Logger logger = LoggerFactory.getLogger(this.getClass());

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

    @Getter(PROTECTED)
    private final AtomicReference<@Nullable Writer> writer = new AtomicReference<>();

    @Nullable
    private OpenHabMessageHandler handler;

    public ServerAbstractDeviceHandler(Thing thing) {
        super(thing);
    }

    @Override
    protected void internalInitialize() {
        var bridge = getBridge();
        if (bridge == null) {
            updateStatus(
                    OFFLINE, BRIDGE_UNINITIALIZED, "There is no bridge for this thing. Remove it and add it again.");
            return;
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
            updateStatus(
                    OFFLINE,
                    CONFIGURATION_ERROR,
                    "Bridge has wrong type! Should be one of:" + allowedBridgeClasses + ", but was " + simpleName);
            return;
        }
        var localBridgeHandler = this.bridgeHandler = (SuplaBridge) rawBridgeHandler;

        var config = getConfigAs(ServerDeviceHandlerConfiguration.class);
        guid = config.getGuid();
        if (guid == null || guid.isEmpty()) {
            guid = null;
            updateStatus(OFFLINE, CONFIGURATION_ERROR, "There is no guid for this thing.");
            return;
        }
        logger = LoggerFactory.getLogger(this.getClass().getName() + "." + guid);

        {
            var timeoutConfiguration = buildTimeoutConfiguration(localBridgeHandler, config);
            var authData = buildAuthData(localBridgeHandler, config);

            deviceConfiguration = new DeviceConfiguration(timeoutConfiguration, authData);
        }

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
            if (entity instanceof SuplaPingServer ping) {
                consumeSuplaPingServer(ping, writer);
            } else if (entity instanceof SuplaSetActivityTimeout) {
                consumeSuplaSetActivityTimeout(writer);
            } else if (entity instanceof SuplaDeviceChannelValue value) {
                consumeDeviceChannelValueTrait(new DeviceChannelValueTrait(value));
            } else if (entity instanceof SuplaDeviceChannelValueB value) {
                consumeDeviceChannelValueTrait(new DeviceChannelValueTrait(value));
            } else if (entity instanceof SuplaDeviceChannelValueC value) {
                consumeDeviceChannelValueTrait(new DeviceChannelValueTrait(value));
            } else if (entity instanceof SuplaDeviceChannelExtendedValue value) {
                var extendedValue = value.value;
                consumeSuplaDeviceChannelExtendedValue(value.channelNumber, extendedValue.type, extendedValue.value);
            } else if (entity instanceof LocalTimeRequest value) {
                consumeLocalTimeRequest(writer);
            } else if (entity instanceof SetCaption value) {
                consumeSetCaption(value);
            } else if (entity instanceof ChannelState value) {
                consumeChannelState(value);
            } else if (entity instanceof SubdeviceDetails value) {
                consumeSubDeviceDetails(value);
            } else if (entity instanceof SuplaChannelNewValueResult value) {
                consumeSuplaChannelNewValueResult(value);
            } else {
                logger.debug("Not supporting message:\n{}", entity);
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
        var local = bridgeHandler;
        if (local != null) {
            local.deviceDisconnected();
        }
    }

    @Override
    public boolean register(RegisterDeviceTrait registerEntity, OpenHabMessageHandler handler) {
        updateStatus(OFFLINE, HANDLER_CONFIGURATION_PENDING, "Device is authorizing...");
        this.handler = handler;
        {
            var local = pingSchedule;
            pingSchedule = null;
            if (local != null) {
                local.cancel(false);
            }
        }

        // auth
        logger.debug("Authorizing...");
        authorized = false;
        if (registerEntity instanceof RegisterLocationDeviceTrait registerDevice) {
            authorized = authorizeForLocation(registerDevice.getLocationId(), registerDevice.getLocationPwd());
            if (!authorized) {
                updateStatus(
                        OFFLINE,
                        CONFIGURATION_ERROR,
                        "Device authorization failed. Device tried to log in with locationId=%s and locationPassword=%s"
                                .formatted(
                                        registerDevice.getLocationId(), parseString(registerDevice.getLocationPwd())));
            }
        } else if (registerEntity instanceof RegisterEmailDeviceTrait registerDevice) {
            authorized = authorizeForEmail(registerDevice.getEmail(), registerDevice.getAuthKey());
            if (!authorized) {
                updateStatus(
                        OFFLINE,
                        CONFIGURATION_ERROR,
                        "Device authorization failed. Device tried to log in with email=%s and authKey=%s"
                                .formatted(registerDevice.getEmail(), bytesToHex(registerDevice.getAuthKey())));
            }
        } else {
            updateStatus(
                    OFFLINE,
                    COMMUNICATION_ERROR,
                    "Do not know how to handle %s during registration"
                            .formatted(registerEntity.getClass().getSimpleName()));
        }
        if (!authorized) {
            return false;
        }
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
        if (register) {
            requireNonNull(getWriter().get())
                    .write(new SuplaRegisterDeviceResult(
                            SUPLA_RESULTCODE_TRUE.getValue(), ACTIVITY_TIMEOUT, VERSION, VERSION_MIN));
            updateStatus(ONLINE);
        }
        return register;
    }

    protected abstract boolean afterRegister(RegisterDeviceTrait registerEntity);

    @Override
    public final void consumeSuplaSetActivityTimeout(Writer writer) {
        var timeout = requireNonNull(deviceConfiguration).timeoutConfiguration();
        var data = new SuplaSetActivityTimeoutResult(
                (short) timeout.timeout(), (short) timeout.min(), (short) timeout.max());
        writer.write(data).addCompleteListener(() -> logger.trace("setActivityTimeout {}", data));
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
        var response = new SuplaPingServerResult(ping.now);
        writer.write(response).addCompleteListener(() -> {
            logger.trace("pingServer {}s {}ms", response.now.tvSec, response.now.tvUsec);
            lastMessageFromDevice.set(now().getEpochSecond());
        });
    }

    private boolean authorizeForLocation(int accessId, byte[] accessIdPassword) {
        if (deviceConfiguration == null) {
            return false;
        }
        var locationAuthData = deviceConfiguration.authData().locationAuthData();
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
        if (deviceConfiguration == null) {
            return false;
        }
        var emailAuthData = deviceConfiguration.authData().emailAuthData();
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
        if (deviceConfiguration == null) {
            return;
        }
        var timeout = deviceConfiguration.timeoutConfiguration();
        var now = now().getEpochSecond();
        var lastPing = lastMessageFromDevice.get();
        var delta = now - lastPing;
        if (delta > timeout.max()) {
            var lastPingDate = new Date(SECONDS.toMillis(lastPing));
            updateStatus(
                    OFFLINE,
                    COMMUNICATION_ERROR,
                    "Device did not send ping message in last " + delta + " seconds. Last message was from "
                            + lastPingDate);
            {
                var local = pingSchedule;
                pingSchedule = null;
                if (local != null) {
                    local.cancel(true);
                }
            }
        }
    }

    private ChannelUID createChannelUid(int channelNumber) {
        return new ChannelUID(getThing().getUID(), valueOf(channelNumber));
    }

    @Override
    public void dispose() {
        {
            var local = pingSchedule;
            pingSchedule = null;
            if (local != null) {
                local.cancel(true);
            }
        }
        {
            var localHandler = handler;
            handler = null;
            if (localHandler != null) {
                localHandler.clear();
            }
        }
        writer.set(null);
        logger = LoggerFactory.getLogger(this.getClass());
        authorized = false;
    }
}
