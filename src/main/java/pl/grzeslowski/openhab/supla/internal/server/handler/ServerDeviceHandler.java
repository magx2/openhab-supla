package pl.grzeslowski.openhab.supla.internal.server.handler;

import static java.lang.Short.parseShort;
import static java.lang.String.valueOf;
import static java.time.Instant.now;
import static java.util.Collections.synchronizedMap;
import static java.util.Objects.requireNonNull;
import static java.util.Objects.requireNonNullElse;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.openhab.core.thing.ThingStatus.OFFLINE;
import static org.openhab.core.thing.ThingStatus.ONLINE;
import static org.openhab.core.thing.ThingStatusDetail.*;
import static pl.grzeslowski.jsupla.protocol.api.ProtocolHelpers.parseString;
import static pl.grzeslowski.jsupla.protocol.api.ResultCode.SUPLA_RESULTCODE_TRUE;
import static pl.grzeslowski.openhab.supla.internal.SuplaBindingConstants.BINDING_ID;
import static pl.grzeslowski.openhab.supla.internal.SuplaBindingConstants.ServerDevicesProperties.CONFIG_AUTH_PROPERTY;
import static pl.grzeslowski.openhab.supla.internal.SuplaBindingConstants.ServerDevicesProperties.SOFT_VERSION_PROPERTY;

import java.util.*;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.Getter;
import lombok.ToString;
import lombok.val;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.javatuples.Pair;
import org.openhab.core.common.ThreadPoolManager;
import org.openhab.core.library.types.*;
import org.openhab.core.thing.Channel;
import org.openhab.core.thing.ChannelUID;
import org.openhab.core.thing.Thing;
import org.openhab.core.thing.ThingStatus;
import org.openhab.core.thing.binding.builder.ThingBuilder;
import org.openhab.core.types.Command;
import org.openhab.core.types.State;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.grzeslowski.jsupla.protocol.api.channeltype.decoders.ChannelTypeDecoder;
import pl.grzeslowski.jsupla.protocol.api.channeltype.encoders.ChannelTypeEncoderImpl;
import pl.grzeslowski.jsupla.protocol.api.channeltype.value.*;
import pl.grzeslowski.jsupla.protocol.api.structs.dcs.SuplaPingServer;
import pl.grzeslowski.jsupla.protocol.api.structs.dcs.SuplaSetActivityTimeout;
import pl.grzeslowski.jsupla.protocol.api.structs.ds.SuplaDeviceChannelExtendedValue;
import pl.grzeslowski.jsupla.protocol.api.structs.ds.SuplaDeviceChannelValue;
import pl.grzeslowski.jsupla.protocol.api.structs.sd.SuplaChannelNewValue;
import pl.grzeslowski.jsupla.protocol.api.structs.sd.SuplaRegisterDeviceResult;
import pl.grzeslowski.jsupla.protocol.api.structs.sdc.SuplaPingServerResult;
import pl.grzeslowski.jsupla.protocol.api.structs.sdc.SuplaSetActivityTimeoutResult;
import pl.grzeslowski.jsupla.protocol.api.types.ToServerProto;
import pl.grzeslowski.jsupla.server.api.Writer;
import pl.grzeslowski.openhab.supla.internal.handler.AbstractDeviceHandler;
import pl.grzeslowski.openhab.supla.internal.server.ChannelCallback;
import pl.grzeslowski.openhab.supla.internal.server.ChannelValueToState;
import pl.grzeslowski.openhab.supla.internal.server.traits.DeviceChannelTrait;
import pl.grzeslowski.openhab.supla.internal.server.traits.RegisterDeviceTrait;
import pl.grzeslowski.openhab.supla.internal.server.traits.RegisterEmailDeviceTrait;
import pl.grzeslowski.openhab.supla.internal.server.traits.RegisterLocationDeviceTrait;

/** The {@link ServerDeviceHandler} is responsible for handling commands, which are sent to one of the channels. */
@NonNullByDefault
@ToString(onlyExplicitlyIncluded = true)
public class ServerDeviceHandler extends AbstractDeviceHandler implements SuplaThing {
    public static final byte ACTIVITY_TIMEOUT = (byte) 100;
    public static final byte VERSION = (byte) 6;
    public static final byte VERSION_MIN = (byte) 1;
    private Logger logger = LoggerFactory.getLogger(ServerDeviceHandler.class);

    private final Map<Integer, Integer> channelTypes = synchronizedMap(new HashMap<>());

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
    private ServerBridgeHandler bridgeHandler;

    private final AtomicReference<@Nullable Writer> writer = new AtomicReference<>();

    public ServerDeviceHandler(Thing thing) {
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
        if (!(rawBridgeHandler instanceof ServerBridgeHandler bridgeHandler)) {
            String simpleName;
            if (rawBridgeHandler != null) {
                simpleName = rawBridgeHandler.getClass().getSimpleName();
            } else {
                simpleName = "<null>";
            }
            updateStatus(
                    OFFLINE,
                    CONFIGURATION_ERROR,
                    "Bridge has wrong type! Should be " + ServerBridgeHandler.class.getSimpleName() + ", but was "
                            + simpleName);
            return;
        }
        this.bridgeHandler = bridgeHandler;

        var config = getConfigAs(ServerDeviceHandlerConfiguration.class);
        guid = config.getGuid();
        if (guid == null || guid.isEmpty()) {
            guid = null;
            updateStatus(OFFLINE, CONFIGURATION_ERROR, "There is no guid for this thing.");
            return;
        }
        logger = LoggerFactory.getLogger(ServerDeviceHandler.class.getName() + "." + guid);

        {
            // timeouts
            var bridgeHandlerTimeoutConfiguration =
                    requireNonNullElse(bridgeHandler.getTimeoutConfiguration(), new TimeoutConfiguration(10, 8, 12));

            var timeoutConfiguration = new TimeoutConfiguration(
                    requireNonNullElse(config.getTimeout(), bridgeHandlerTimeoutConfiguration.timeout()),
                    requireNonNullElse(config.getTimeoutMin(), bridgeHandlerTimeoutConfiguration.min()),
                    requireNonNullElse(config.getTimeoutMax(), bridgeHandlerTimeoutConfiguration.max()));

            // auth data
            var bridgeAuthData = requireNonNull(bridgeHandler.getAuthData(), "No auth data in bridge!");
            AuthData.@Nullable LocationAuthData locationAuthData;
            if (config.getServerAccessId() != null && config.getServerAccessIdPassword() != null) {
                locationAuthData = new AuthData.LocationAuthData(
                        config.getServerAccessId().intValue(), config.getServerAccessIdPassword());
            } else {
                locationAuthData = bridgeAuthData.locationAuthData();
            }
            AuthData.@Nullable EmailAuthData emailAuthData;
            var configEmail = config.getEmail();
            var bridgeEmailAuthData = bridgeAuthData.emailAuthData();

            if (configEmail == null && bridgeEmailAuthData == null) {
                emailAuthData = null;
            } else {
                String email;
                //noinspection ReplaceNullCheck
                if (configEmail != null) {
                    email = configEmail;
                } else {
                    email = bridgeEmailAuthData.email();
                }
                emailAuthData = new AuthData.EmailAuthData(email);
            }
            var authData = new AuthData(locationAuthData, emailAuthData);

            deviceConfiguration = new DeviceConfiguration(timeoutConfiguration, authData);
        }

        updateStatus(
                ThingStatus.UNKNOWN,
                HANDLER_CONFIGURATION_PENDING,
                "Waiting for Supla device to connect with the server");
    }

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
                updateStatus(value.channelNumber, value.value);
            } else if (entity instanceof SuplaDeviceChannelExtendedValue value) {
                var extendedValue = value.value;
                updateStatus(value.channelNumber, extendedValue.type, extendedValue.value);
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
    public boolean register(RegisterDeviceTrait registerEntity) {
        updateStatus(OFFLINE, HANDLER_CONFIGURATION_PENDING, "Device is authorizing...");
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
                                .formatted(registerDevice.getEmail(), registerDevice.getAuthKey()));
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

        // set the software version
        thing.setProperty(SOFT_VERSION_PROPERTY, registerEntity.getSoftVer());

        setChannels(registerEntity.getChannels());
        requireNonNull(writer.get())
                .write(new SuplaRegisterDeviceResult(
                        SUPLA_RESULTCODE_TRUE.getValue(), ACTIVITY_TIMEOUT, VERSION, VERSION_MIN));

        return true;
    }

    private void consumeSuplaSetActivityTimeout(Writer writer) {
        var timeout = requireNonNull(deviceConfiguration).timeoutConfiguration();
        var data = new SuplaSetActivityTimeoutResult(
                (short) timeout.timeout(), (short) timeout.min(), (short) timeout.max());
        writer.write(data).addCompleteListener(() -> logger.trace("setActivityTimeout {}", data));
        pingSchedule = ThreadPoolManager.getScheduledPool(BINDING_ID)
                .scheduleWithFixedDelay(this::checkIfDeviceIsUp, timeout.timeout() * 2L, timeout.timeout(), SECONDS);
    }

    private void consumeSuplaPingServer(SuplaPingServer ping, Writer writer) {
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

    private boolean authorizeForEmail(String email, String authKey) {
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
        var key = thing.getProperties().get(CONFIG_AUTH_PROPERTY);
        if (key == null) {
            logger.debug("Device is missing {} property", CONFIG_AUTH_PROPERTY);
            return false;
        }
        if (!key.equals(authKey)) {
            logger.debug("Wrong auth key; {} != {}", authKey, key);
            return false;
        }
        return true;
    }

    private void sendCommandToSuplaServer(ChannelUID channelUID, ChannelValue channelValue, Command command) {
        val localWriter = writer.get();
        if (localWriter == null) {
            logger.debug("There is no writer for channelUID={}", channelUID);
            return;
        }
        var id = channelUID.getId();
        short channelNumber;
        try {
            channelNumber = parseShort(id);
        } catch (NumberFormatException ex) {
            logger.warn("Cannot parse ID {} from {}", id, channelUID, ex);
            return;
        }
        var encode = ChannelTypeEncoderImpl.INSTANCE.encode(channelValue);
        var channelNewValue = new SuplaChannelNewValue(1, channelNumber, 100L, null, encode);
        try {
            localWriter.write(channelNewValue).addCompleteListener(() -> {
                logger.debug("Changed value of channel for {} command {}", channelUID, command);
                updateStatus(ONLINE);
            });
        } catch (Exception ex) {
            var msg = "Couldn't Change value of channel for %s command %s.".formatted(channelUID, command);
            logger.debug(msg, ex);
            updateStatus(OFFLINE, COMMUNICATION_ERROR, msg + ex.getLocalizedMessage());
        }
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

    @Override
    protected void handleRefreshCommand(ChannelUID channelUID) {
        // there is no need to handle refresh command
    }

    @Override
    protected void handleOnOffCommand(ChannelUID channelUID, OnOffType command) {
        var toSend =
                switch (command) {
                    case ON -> OnOff.ON;
                    case OFF -> OnOff.OFF;
                };
        sendCommandToSuplaServer(channelUID, toSend, command);
    }

    @Override
    protected void handleUpDownCommand(ChannelUID channelUID, UpDownType command) {
        // TODO handle this command
    }

    @SuppressWarnings("deprecation")
    @Override
    protected void handleHsbCommand(ChannelUID channelUID, HSBType command) {
        RgbValue toSend = new RgbValue(
                command.getBrightness().intValue(),
                255, // TODO I don't know if this is
                // correct
                command.getRed().intValue(),
                command.getGreen().intValue(),
                command.getBlue().intValue());
        sendCommandToSuplaServer(channelUID, toSend, command);
    }

    @Override
    protected void handleOpenClosedCommand(ChannelUID channelUID, OpenClosedType command) {
        var toSend =
                switch (command) {
                    case OPEN -> OnOff.ON;
                    case CLOSED -> OnOff.OFF;
                };
        sendCommandToSuplaServer(channelUID, toSend, command);
    }

    @Override
    protected void handlePercentCommand(ChannelUID channelUID, PercentType command) {
        sendCommandToSuplaServer(channelUID, new PercentValue(command.intValue()), command);
    }

    @Override
    protected void handleDecimalCommand(ChannelUID channelUID, DecimalType command) {
        sendCommandToSuplaServer(channelUID, new DecimalValue(command.toBigDecimal()), command);
    }

    @Override
    protected void handleStopMoveTypeCommand(@NonNull ChannelUID channelUID, @NonNull StopMoveType command) {
        logger.warn(
                "Not handling `{}` ({}) on channel `{}`",
                command,
                command.getClass().getSimpleName(),
                channelUID);
    }

    @Override
    protected void handleStringCommand(ChannelUID channelUID, StringType command) throws Exception {
        logger.warn(
                "Not handling `{}` ({}) on channel `{}`",
                command,
                command.getClass().getSimpleName(),
                channelUID);
    }

    private void setChannels(List<DeviceChannelTrait> deviceChannels) {
        {
            var channels = deviceChannels.stream()
                    .sorted(Comparator.comparingInt(DeviceChannelTrait::getNumber))
                    .flatMap(this::createChannel)
                    .toList();
            if (logger.isDebugEnabled()) {
                var rawChannels = deviceChannels.stream()
                        .map(DeviceChannelTrait::toString)
                        .collect(Collectors.joining("\n"));
                var string = channels.stream()
                        .map(channel -> channel.getUID() + " -> " + channel.getChannelTypeUID())
                        .collect(Collectors.joining("\n"));
                logger.debug(
                        """
                                Registering channels:
                                 > Raw:
                                {}

                                 > OpenHABs:
                                {}""",
                        rawChannels,
                        string);
            }
            updateChannels(channels);
        }
        deviceChannels.stream()
                .flatMap(this::channelForUpdate)
                .forEach(pair -> updateState(pair.getValue0(), pair.getValue1()));
    }

    private Stream<Pair<ChannelUID, State>> channelForUpdate(DeviceChannelTrait deviceChannel) {
        Class<? extends ChannelValue> clazz = ChannelTypeDecoder.INSTANCE.findClass(deviceChannel.getType());
        if (clazz.isAssignableFrom(ElectricityMeterValue.class)) {
            return Stream.empty();
        }
        return findState(deviceChannel.getType(), deviceChannel.getNumber(), deviceChannel.getValue());
    }

    private ChannelUID createChannelUid(int channelNumber) {
        return new ChannelUID(getThing().getUID(), valueOf(channelNumber));
    }

    private Stream<Pair<ChannelUID, State>> findState(int type, int channelNumber, byte[] value) {
        val valueSwitch =
                new ChannelValueSwitch<>(new ChannelValueToState(getThing().getUID(), channelNumber));
        return valueSwitch.doSwitch(ChannelTypeDecoder.INSTANCE.decode(type, value));
    }

    private void updateStatus(int channelNumber, byte[] channelValue) {
        var type = channelTypes.get(channelNumber);
        updateStatus(channelNumber, type, channelValue);
    }

    private void updateStatus(int channelNumber, int type, byte[] channelValue) {
        logger.debug("Updating status for channel {}", channelNumber);
        findState(type, channelNumber, channelValue).forEach(pair -> {
            var channelUID = pair.getValue0();
            var state = pair.getValue1();
            logger.debug(
                    "Updating state for channel {}, channelNumber {}, type {}, state={}",
                    channelUID,
                    channelNumber,
                    type,
                    state);
            updateState(channelUID, state);
        });
    }

    private Stream<Channel> createChannel(DeviceChannelTrait deviceChannel) {
        var channelCallback = new ChannelCallback(getThing().getUID(), deviceChannel.getNumber());
        var channelValueSwitch = new ChannelClassSwitch<>(channelCallback);
        var clazz = ChannelTypeDecoder.INSTANCE.findClass(deviceChannel.getType());
        var channels = channelValueSwitch.doSwitch(clazz);
        channelTypes.put(deviceChannel.getNumber(), deviceChannel.getType());
        return channels;
    }

    private void updateChannels(List<Channel> channels) {
        ThingBuilder thingBuilder = editThing();
        thingBuilder.withChannels(channels);
        updateThing(thingBuilder.build());
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
        logger = LoggerFactory.getLogger(ServerDeviceHandler.class);
        authorized = false;
    }
}
