package pl.grzeslowski.openhab.supla.internal.server.handler;

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
import org.openhab.core.thing.binding.builder.ChannelBuilder;
import org.openhab.core.types.Command;
import org.openhab.core.types.State;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.grzeslowski.jsupla.protocol.api.channeltype.decoders.ChannelTypeDecoder;
import pl.grzeslowski.jsupla.protocol.api.channeltype.decoders.HVACValueDecoderImpl;
import pl.grzeslowski.jsupla.protocol.api.channeltype.encoders.ChannelTypeEncoderImpl;
import pl.grzeslowski.jsupla.protocol.api.channeltype.value.*;
import pl.grzeslowski.jsupla.protocol.api.structs.HVACValue;
import pl.grzeslowski.jsupla.protocol.api.structs.dcs.LocalTimeRequest;
import pl.grzeslowski.jsupla.protocol.api.structs.dcs.SetCaption;
import pl.grzeslowski.jsupla.protocol.api.structs.dcs.SuplaPingServer;
import pl.grzeslowski.jsupla.protocol.api.structs.dcs.SuplaSetActivityTimeout;
import pl.grzeslowski.jsupla.protocol.api.structs.ds.*;
import pl.grzeslowski.jsupla.protocol.api.structs.dsc.ChannelState;
import pl.grzeslowski.jsupla.protocol.api.structs.sd.SuplaChannelNewValue;
import pl.grzeslowski.jsupla.protocol.api.structs.sd.SuplaRegisterDeviceResult;
import pl.grzeslowski.jsupla.protocol.api.structs.sdc.SuplaPingServerResult;
import pl.grzeslowski.jsupla.protocol.api.structs.sdc.SuplaSetActivityTimeoutResult;
import pl.grzeslowski.jsupla.protocol.api.structs.sdc.UserLocalTimeResult;
import pl.grzeslowski.jsupla.protocol.api.types.ToServerProto;
import pl.grzeslowski.jsupla.server.api.Writer;
import pl.grzeslowski.openhab.supla.internal.handler.AbstractDeviceHandler;
import pl.grzeslowski.openhab.supla.internal.server.ByteArrayToHex;
import pl.grzeslowski.openhab.supla.internal.server.ChannelCallback;
import pl.grzeslowski.openhab.supla.internal.server.ChannelValueToState;
import pl.grzeslowski.openhab.supla.internal.server.traits.*;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.TextStyle;
import java.util.*;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.lang.Short.parseShort;
import static java.lang.String.valueOf;
import static java.time.Instant.now;
import static java.util.Collections.synchronizedMap;
import static java.util.Comparator.comparing;
import static java.util.Objects.requireNonNull;
import static java.util.Objects.requireNonNullElse;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.openhab.core.thing.ChannelUID.CHANNEL_GROUP_SEPARATOR;
import static org.openhab.core.thing.ThingStatus.OFFLINE;
import static org.openhab.core.thing.ThingStatus.ONLINE;
import static org.openhab.core.thing.ThingStatusDetail.*;
import static pl.grzeslowski.jsupla.protocol.api.ProtocolHelpers.parseString;
import static pl.grzeslowski.jsupla.protocol.api.ResultCode.SUPLA_RESULTCODE_TRUE;
import static pl.grzeslowski.openhab.supla.internal.SuplaBindingConstants.BINDING_ID;
import static pl.grzeslowski.openhab.supla.internal.SuplaBindingConstants.ServerDevicesProperties.*;
import static pl.grzeslowski.openhab.supla.internal.server.ByteArrayToHex.bytesToHex;
import static pl.grzeslowski.openhab.supla.internal.server.ByteArrayToHex.hexToBytes;

/**
 * The {@link ServerDeviceHandler} is responsible for handling commands, which are sent to one of the channels.
 */
@NonNullByDefault
@ToString(onlyExplicitlyIncluded = true)
public class ServerDeviceHandler extends AbstractDeviceHandler implements SuplaThing {
    public static final byte ACTIVITY_TIMEOUT = (byte) 100;
    public static final byte VERSION = (byte) 6;
    public static final byte VERSION_MIN = (byte) 1;
    protected Logger logger = LoggerFactory.getLogger(this.getClass());

    private final Map<Integer, Integer> channelTypes = synchronizedMap(new HashMap<>());
    private final Object editThingLock = new Object();

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
    private SuplaBridge bridgeHandler;

    private final AtomicReference<@Nullable Writer> writer = new AtomicReference<>();
    @Nullable
    private OpenHabMessageHandler handler;

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
        var bridgeClasses = findAllowedBridgeClasses();
        var bridgeHasCorrectClass = rawBridgeHandler != null && bridgeClasses.stream().anyMatch(clazz -> clazz.isInstance(rawBridgeHandler));
        if (!bridgeHasCorrectClass) {
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
            // timeouts
            var bridgeHandlerTimeoutConfiguration =
                    requireNonNullElse(localBridgeHandler.getTimeoutConfiguration(), new TimeoutConfiguration(10, 8, 12));

            var timeoutConfiguration = new TimeoutConfiguration(
                    requireNonNullElse(config.getTimeout(), bridgeHandlerTimeoutConfiguration.timeout()),
                    requireNonNullElse(config.getTimeoutMin(), bridgeHandlerTimeoutConfiguration.min()),
                    requireNonNullElse(config.getTimeoutMax(), bridgeHandlerTimeoutConfiguration.max()));

            // auth data
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

    protected List<Class<? extends SuplaBridge>> findAllowedBridgeClasses() {
        return List.of(ServerBridgeHandler.class, ServerGatewayHandler.class);
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
                updateStatus(new DeviceChannelValueTrait(value));
            } else if (entity instanceof SuplaDeviceChannelValueB value) {
                updateStatus(new DeviceChannelValueTrait(value));
            } else if (entity instanceof SuplaDeviceChannelValueC value) {
                updateStatus(new DeviceChannelValueTrait(value));
            } else if (entity instanceof SuplaDeviceChannelExtendedValue value) {
                var extendedValue = value.value;
                updateStatus(value.channelNumber, extendedValue.type, extendedValue.value);
            } else if (entity instanceof LocalTimeRequest value) {
                consumeLocalTimeRequest(writer);
            } else if (entity instanceof SetCaption value) {
                consumeSetCaption(value);
            } else if (entity instanceof ChannelState value) {
                consumeChannelState(value);
            } else if (entity instanceof SubdeviceDetails value) {
                consumeSubDeviceDetails(value);
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

        // set properties
        thing.setProperty(SOFT_VERSION_PROPERTY, registerEntity.getSoftVer());
        if (registerEntity.getManufacturerId() != null) {
            thing.setProperty(MANUFACTURER_ID_PROPERTY, valueOf(registerEntity.getManufacturerId()));
        }
        if (registerEntity.getProductId() != null) {
            thing.setProperty(PRODUCT_ID_PROPERTY, valueOf(registerEntity.getProductId()));
        }

        return afterRegister(registerEntity);
    }

    protected boolean afterRegister(RegisterDeviceTrait registerEntity) {
        var flags = registerEntity.getFlags();
//        if (flags.isCalcfgSubdevicePairing()) {
//            updateStatus(OFFLINE, CONFIGURATION_ERROR,
//                    "Device should be created as %s. See %s for more information."
//                            .formatted(SUPLA_THING_BRIDGE_TYPE.getId(), THING_BRIDGE));
//            return false;
//        }

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

    private void consumeLocalTimeRequest(Writer writer) {
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

        writer.write(
                new UserLocalTimeResult(year, month, day, dayOfWeek, hour, minute, seconds, timeZoneSize, timeZone));
    }

    private void consumeSetCaption(SetCaption value) {
        var id = findId(value);
        if (id == null) {
            return;
        }
        var channelUID = new ChannelUID(thing.getUID(), valueOf(id));
        var channel = thing.getChannel(channelUID);
        var channels = new ArrayList<Channel>(1);
        if (channel == null) {
            // look for group channels
            channels.addAll(thing.getChannels()
                    .stream()
                    .filter(c -> {
                        var uid = c.getUID();
                        if (!uid.isInGroup()) return false;
                        var guid = uid.getGroupId();
                        if (guid == null) return false;
                        return guid.equals(valueOf(id));
                    })
                    .toList());
            if (channels.isEmpty()) {
                logger.warn("There is no channel with ID {} that I can set value to. value={}, caption={}",
                        id, value, parseString(value.caption));
                return;
            }
        } else {
            channels.add(channel);
        }
        var channelsWithCaption = channels.stream()
                .map(c -> ChannelBuilder.create(c)
                        .withLabel(parseString(value.caption) + " > " + c.getLabel())
                        .build())
                .toList();
        var updatedChannelsIds = channelsWithCaption.stream()
                .map(Channel::getUID)
                .collect(Collectors.toSet());
        synchronized (editThingLock) {
            var newChannels = new ArrayList<>(thing.getChannels()
                    .stream()
                    .filter(c -> !updatedChannelsIds.contains(c.getUID()))
                    .toList());
            newChannels.addAll(channelsWithCaption);
            updateChannels(newChannels);
        }
    }

    @Nullable
    private Integer findId(SetCaption value) {
        if (value.id != null) {
            return value.id;
        }

        if (value.channelNumber != null) {
            return Integer.valueOf(value.channelNumber);
        }

        logger.debug("Cannot set caption, because ID is null. value={}", value);
        return null;
    }

    private void consumeChannelState(ChannelState value) {
        // there is nothing interesting that I can do with it, ignore
        logger.debug("value={}", value);
    }

    private void consumeSubDeviceDetails(SubdeviceDetails value) {
        // there is nothing interesting that I can do with it, ignore
        if (logger.isDebugEnabled()) {
            logger.debug("SubdeviceDetails(subDeviceId={}, name={}, softVer={}, productCode={}, serialNumber={})",
                    value.subDeviceId,
                    parseString(value.name),
                    parseString(value.softVer),
                    parseString(value.productCode),
                    parseString(value.serialNumber));
        }
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
        var key = thing.getProperties().get(CONFIG_AUTH_PROPERTY);
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
            var adjustLabel = deviceChannels.size() > 1;
            var digits = deviceChannels.isEmpty() ? 1 : ((int) Math.log10(deviceChannels.size()) + 1);
            var idx = new AtomicInteger(1);
            var channels = deviceChannels.stream()
                    .flatMap(deviceChannel -> createChannel(deviceChannel, adjustLabel, idx.getAndIncrement(), digits))
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
        return findState(deviceChannel.getType(), deviceChannel.getNumber(), deviceChannel.getValue(), deviceChannel.getHvacValue());
    }

    private ChannelUID createChannelUid(int channelNumber) {
        return new ChannelUID(getThing().getUID(), valueOf(channelNumber));
    }

    private Stream<Pair<ChannelUID, State>> findState(int type, int channelNumber,
                                                      @Nullable @jakarta.annotation.Nullable byte[] value,
                                                      @jakarta.annotation.Nullable @Nullable HVACValue hvacValue) {
        val valueSwitch = new ChannelValueSwitch<>(
                new ChannelValueToState(
                        getThing().getUID(), channelNumber));
        ChannelValue channelValue;
        if (value != null) {
            channelValue = ChannelTypeDecoder.INSTANCE.decode(type, value);
        } else if (hvacValue != null) {
            channelValue = HVACValueDecoderImpl.INSTANCE.decode(hvacValue);
        } else {
            throw new IllegalArgumentException("value and hvacValue cannot be null!");
        }
        return valueSwitch.doSwitch(channelValue);
    }

    private void updateStatus(DeviceChannelValueTrait trait) {
        if (trait.isOffline()) {
            logger.debug("Channel Value is offline, ignoring it. trait={}", trait);
            return;
        }
        var type = channelTypes.get(trait.getChannelNumber());
        updateStatus(trait.getChannelNumber(), type, trait.getValue());
    }

    private void updateStatus(int channelNumber, int type, byte[] channelValue) {
        logger.debug("Updating status for channelNumber={}, type={}", channelNumber, type);
        findState(type, channelNumber, channelValue, null)
                .forEach(pair -> {
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

    private Stream<Channel> createChannel(DeviceChannelTrait deviceChannel, boolean adjustLabel, int idx, int digits) {
        var channelCallback = new ChannelCallback(getThing().getUID(), deviceChannel.getNumber());
        var channelValueSwitch = new ChannelClassSwitch<>(channelCallback);
        var clazz = ChannelTypeDecoder.INSTANCE.findClass(deviceChannel.getType());
        var channels = channelValueSwitch.doSwitch(clazz);
        channelTypes.put(deviceChannel.getNumber(), deviceChannel.getType());
        if (adjustLabel) {
            return channels.map(channel -> new Pair<>(ChannelBuilder.create(channel), channel.getLabel()))
                    .map(pair -> pair.getValue0().withLabel(pair.getValue1() + (" (#%0" + digits + "d)").formatted(idx)))
                    .map(ChannelBuilder::build);
        }
        return channels;
    }

    private void updateChannels(List<Channel> channels) {
        synchronized (editThingLock) {
            new ArrayList<>(channels).sort((o1, o2) -> comparing((Channel id) -> {
                try {
                    var stringId = id.getUID().getId();
                    if (stringId.contains(CHANNEL_GROUP_SEPARATOR)) {
                        stringId = stringId.split(CHANNEL_GROUP_SEPARATOR)[0];
                    }
                    return Integer.parseInt(stringId);
                } catch (NumberFormatException e) {
                    return Integer.MAX_VALUE;
                }
            })
                    .compare(o1, o2));

            var thingBuilder = editThing();
            thingBuilder.withChannels(channels);
            updateThing(thingBuilder.build());
        }
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
