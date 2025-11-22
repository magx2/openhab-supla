package pl.grzeslowski.openhab.supla.internal.server.handler;

import static java.util.Collections.unmodifiableList;
import static java.util.concurrent.TimeUnit.SECONDS;
import static java.util.stream.Collectors.toUnmodifiableSet;
import static org.openhab.core.thing.ThingStatus.OFFLINE;
import static org.openhab.core.thing.ThingStatusDetail.CONFIGURATION_ERROR;
import static pl.grzeslowski.jsupla.protocol.api.ProtocolHelpers.parseString;
import static pl.grzeslowski.openhab.supla.internal.GuidLogger.attachGuid;
import static pl.grzeslowski.openhab.supla.internal.SuplaBindingConstants.GATEWAY_CONNECTED_DEVICES_CHANNEL_ID;
import static pl.grzeslowski.openhab.supla.internal.SuplaBindingConstants.ServerDevicesProperties.*;
import static pl.grzeslowski.openhab.supla.internal.server.ChannelUtil.findId;

import io.netty.channel.ChannelFuture;
import java.util.*;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import lombok.Getter;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.javatuples.Pair;
import org.openhab.core.common.ThreadPoolManager;
import org.openhab.core.library.types.*;
import org.openhab.core.thing.ChannelUID;
import org.openhab.core.thing.Thing;
import org.openhab.core.thing.ThingStatus;
import org.openhab.core.thing.ThingStatusDetail;
import org.openhab.core.thing.binding.ThingHandler;
import org.openhab.core.thing.binding.builder.ThingBuilder;
import org.openhab.core.types.State;
import pl.grzeslowski.jsupla.protocol.api.structs.dcs.SetCaption;
import pl.grzeslowski.jsupla.protocol.api.structs.ds.SubdeviceDetails;
import pl.grzeslowski.jsupla.protocol.api.structs.ds.SuplaChannelNewValueResult;
import pl.grzeslowski.jsupla.protocol.api.structs.dsc.ChannelState;
import pl.grzeslowski.jsupla.protocol.api.types.FromServerProto;
import pl.grzeslowski.openhab.supla.internal.GuidLogger;
import pl.grzeslowski.openhab.supla.internal.server.ChannelUtil;
import pl.grzeslowski.openhab.supla.internal.server.discovery.ServerDiscoveryService;
import pl.grzeslowski.openhab.supla.internal.server.handler.trait.ServerBridge;
import pl.grzeslowski.openhab.supla.internal.server.handler.trait.ServerDevice;
import pl.grzeslowski.openhab.supla.internal.server.oh_config.AuthData;
import pl.grzeslowski.openhab.supla.internal.server.oh_config.ServerDeviceHandlerConfiguration;
import pl.grzeslowski.openhab.supla.internal.server.oh_config.TimeoutConfiguration;
import pl.grzeslowski.openhab.supla.internal.server.traits.DeviceChannel;
import pl.grzeslowski.openhab.supla.internal.server.traits.DeviceChannelValue;
import pl.grzeslowski.openhab.supla.internal.server.traits.RegisterDeviceTrait;

@NonNullByDefault
public class GatewayDeviceHandler extends ServerAbstractDeviceHandler implements ServerBridge, ServerDevice {
    private final AtomicInteger numberOfConnectedDevices = new AtomicInteger();
    private final Map<Integer, SubDeviceHandler> childHandlers = Collections.synchronizedMap(new HashMap<>());
    private Map<Integer, Integer> channelNumberToHandlerId = Map.of();

    @Getter
    @Nullable
    private TimeoutConfiguration timeoutConfiguration;

    private final ChannelUtil channelUtil = new ChannelUtil(this);

    @Getter
    @Nullable
    private AuthData authData;

    private List<DeviceChannel> channels = List.of();
    private final AtomicReference<@Nullable ScheduledFuture<?>> initServiceDiscoverySchedule = new AtomicReference<>();
    private final ServerDiscoveryService serverDiscoveryService;
    private final Set<Integer> discoveredIds = Collections.synchronizedSet(new HashSet<>());

    public GatewayDeviceHandler(Thing thing, ServerDiscoveryService serverDiscoveryService) {
        super(thing);
        this.serverDiscoveryService = serverDiscoveryService;
    }

    @Override
    protected List<Class<? extends ServerBridge>> findAllowedBridgeClasses() {
        return List.of(ServerBridgeHandler.class);
    }

    @Override
    protected TimeoutConfiguration buildTimeoutConfiguration(
            ServerBridge localBridgeHandler, ServerDeviceHandlerConfiguration config) {
        timeoutConfiguration = super.buildTimeoutConfiguration(localBridgeHandler, config);
        return timeoutConfiguration;
    }

    @Override
    protected AuthData buildAuthData(ServerBridge localBridgeHandler, ServerDeviceHandlerConfiguration config) {
        authData = super.buildAuthData(localBridgeHandler, config);
        return authData;
    }

    @Override
    protected boolean afterRegister(RegisterDeviceTrait registerEntity) {
        var flags = registerEntity.flags();
        if (!flags.calcfgSubdevicePairing()) {
            updateStatus(OFFLINE, CONFIGURATION_ERROR, "This is not a gateway device!");
            return false;
        }

        channels = unmodifiableList(registerEntity.channels());
        childHandlers.values().forEach(this::initChannels);
        channelNumberToHandlerId = channels.stream()
                .filter(c -> c.subDeviceId() != null)
                .map(c -> new Pair<>(c.number(), c.subDeviceId()))
                .collect(Collectors.toMap(Pair::getValue0, Pair::getValue1));

        if (!channels.isEmpty()) {
            var scheduledPool =
                    ThreadPoolManager.getScheduledPool(this.getClass().getSimpleName());
            this.initServiceDiscoverySchedule.set(
                    scheduledPool.schedule(() -> initServiceDiscovery(registerEntity.name()), 30, SECONDS));
        }

        var notSubDeviceChannels =
                channels.stream().filter(c -> c.subDeviceId() == null).toList();
        if (!notSubDeviceChannels.isEmpty()) {
            logger.warn("Gateway has channels, but it is not supported by addon! channels={}", notSubDeviceChannels);
        }

        return true;
    }

    private void initChannels(SubDeviceHandler subDeviceHandler) {
        var channels = this.channels.stream()
                .filter(c -> c.subDeviceId() != null && c.subDeviceId().equals(subDeviceHandler.getSubDeviceId()))
                .toList();
        subDeviceHandler.setChannels(channels);
    }

    private void initServiceDiscovery(String name) {
        this.initServiceDiscoverySchedule.set(null);
        var childIds = childHandlers.keySet();
        var discoveredIds = channels.stream()
                .map(DeviceChannel::subDeviceId)
                .filter(Objects::nonNull)
                .filter(i -> !childIds.contains(i))
                .collect(toUnmodifiableSet());
        this.discoveredIds.addAll(discoveredIds);

        discoveredIds.forEach(id -> serverDiscoveryService.addSubDevice(id, name));
    }

    @Override
    public void handleRefreshCommand(ChannelUID channelUID) {
        updateConnectedDevices(numberOfConnectedDevices.get());
    }

    @Override
    public void handleOnOffCommand(ChannelUID channelUID, OnOffType command) {}

    @Override
    public void handleUpDownCommand(ChannelUID channelUID, UpDownType command) {}

    @Override
    public void handleHsbCommand(ChannelUID channelUID, HSBType command) {}

    @Override
    public void handleOpenClosedCommand(ChannelUID channelUID, OpenClosedType command) {}

    @Override
    public void handlePercentCommand(ChannelUID channelUID, PercentType command) {}

    @Override
    public void handleDecimalCommand(ChannelUID channelUID, DecimalType command) {}

    @Override
    public void handleStopMoveTypeCommand(ChannelUID channelUID, StopMoveType command) {}

    @Override
    public void handleStringCommand(ChannelUID channelUID, StringType command) {}

    @Override
    public void handleQuantityType(ChannelUID channelUID, QuantityType<?> command) {}

    @GuidLogger.GuidLogged
    @Override
    public void childHandlerInitialized(ThingHandler childHandler, Thing childThing) {
        attachGuid(findGuid(), () -> {
            if (!(childHandler instanceof SubDeviceHandler subDevice)) {
                return;
            }
            var subDeviceId = subDevice.getSubDeviceId();
            if (childHandlers.containsKey(subDeviceId)) {
                var existing = childHandlers.get(subDeviceId);
                logger.warn(
                        "childHandlers already contains sub device with ID {}! "
                                + "Will override this handler. "
                                + "existing={}, subDevice={}",
                        subDeviceId,
                        existing,
                        subDevice);
            }
            childHandlers.put(subDeviceId, subDevice);
            if (discoveredIds.contains(subDeviceId)) {
                discoveredIds.remove(subDeviceId);
                serverDiscoveryService.removeSubDevice(subDeviceId);
            }
            if (!channels.isEmpty()) {
                initChannels(subDevice);
            }
        });
    }

    @GuidLogger.GuidLogged
    @Override
    public void childHandlerDisposed(ThingHandler childHandler, Thing childThing) {
        attachGuid(findGuid(), () -> {
            if (!(childHandler instanceof SubDeviceHandler subDevice)) {
                return;
            }
            var subDeviceId = subDevice.getSubDeviceId();
            logger.debug("Remove Handler {}", subDevice);
            if (!childHandlers.containsKey(subDeviceId)) {
                logger.warn("childHandlers do not contains sub device with ID {}!", subDeviceId);
                return;
            }
            childHandlers.remove(subDeviceId);
        });
    }

    @Override
    public void deviceConnected() {
        changeNumberOfConnectedDevices(1);
    }

    @Override
    public void deviceDisconnected() {
        changeNumberOfConnectedDevices(-1);
    }

    @GuidLogger.GuidLogged
    private void changeNumberOfConnectedDevices(int delta) {
        attachGuid(findGuid(), () -> {
            var number = numberOfConnectedDevices.addAndGet(delta);
            logger.debug("Number of connected devices: {} (delta: {})", number, delta);
            updateConnectedDevices(number);
        });
    }

    private void updateConnectedDevices(int numberOfConnectedDevices) {
        updateState(GATEWAY_CONNECTED_DEVICES_CHANNEL_ID, new DecimalType(numberOfConnectedDevices));
    }

    @Override
    public void dispose() {
        {
            var schedule = this.initServiceDiscoverySchedule.getAndSet(null);
            if (schedule != null) {
                schedule.cancel(true);
            }
        } // initServiceDiscoverySchedule
        super.dispose();
    }

    @GuidLogger.GuidLogged
    @Override
    public void consumeSuplaDeviceChannelExtendedValue(int channelNumber, int type, byte[] value) {
        attachGuid(findGuid(), () -> {
            var optional = findId(channelNumber, null).flatMap(this::findSubDevice);
            if (optional.isEmpty()) {
                logger.warn("There is no channel number for ExtendedValue! value={}", value);
                return;
            }
            //            optional.ifPresent(sd -> sd.consumeSuplaDeviceChannelExtendedValue(channelNumber, type,
            // value));
        });
    }

    @GuidLogger.GuidLogged
    @Override
    public void consumeSetCaption(SetCaption value) {
        attachGuid(findGuid(), () -> {
            var optional = findId(value.id(), value.channelNumber()).flatMap(this::findSubDevice);
            if (optional.isEmpty()) {
                logger.warn("There is no channel number for SetCaption! value={}", value);
                return;
            }
            optional.ifPresent(sd -> sd.consumeSetCaption(value));
        });
    }

    @Override
    public void consumeChannelState(ChannelState value) {
        channelUtil.consumeChannelState(value);
    }

    @GuidLogger.GuidLogged
    @Override
    public void consumeDeviceChannelValueTrait(DeviceChannelValue trait) {
        attachGuid(findGuid(), () -> {
            var optional = findId(trait.channelNumber(), null).flatMap(this::findSubDevice);
            if (optional.isEmpty()) {
                logger.warn("There is no channel number for ChannelState! value={}", trait);
                return;
            }
            optional.ifPresent(sd -> sd.consumeDeviceChannelValueTrait(trait));
        });
    }

    @Override
    public void consumeSubDeviceDetails(SubdeviceDetails value) {
        var subDeviceId = (int) value.subDeviceId();
        var name = parseString(value.name());
        var softVer = parseString(value.softVer());
        var productCode = parseString(value.productCode());
        var serialNumber = parseString(value.serialNumber());

        findSubDevice(subDeviceId).ifPresent(subDevice -> {
            var builder = subDevice.editThing();
            builder.withLabel(name);
            builder.withProperty(SOFT_VERSION_PROPERTY, softVer);
            builder.withProperty(PRODUCT_CODE_PROPERTY, productCode);
            builder.withProperty(SERIAL_NUMBER_PROPERTY, serialNumber);
            subDevice.updateThing(builder.build());
        });
    }

    private Optional<SubDeviceHandler> findSubDevice(Integer channelNumber) {
        if (!channelNumberToHandlerId.containsKey(channelNumber)) {
            return Optional.empty();
        }
        var handlerId = channelNumberToHandlerId.get(channelNumber);
        if (!childHandlers.containsKey(handlerId)) {
            return Optional.empty();
        }
        return Optional.of(childHandlers.get(handlerId));
    }

    @GuidLogger.GuidLogged
    @Override
    public void consumeSuplaChannelNewValueResult(SuplaChannelNewValueResult value) {
        attachGuid(findGuid(), () -> {
            var handlerId = channelNumberToHandlerId.get((int) value.channelNumber());
            if (handlerId == null) {
                // todo
                return;
            }
            var handler = childHandlers.get(handlerId);
            if (handler == null) {
                // todo log
                return;
            }
            handler.consumeSuplaChannelNewValueResult(value);
        });
    }

    @Override
    public Map<Integer, Integer> getChannelTypes() {
        throw new UnsupportedOperationException("ServerGatewayDeviceHandler.getChannelTypes()");
    }

    @Override
    public ThingBuilder editThing() {
        return super.editThing();
    }

    @Override
    public void updateThing(Thing thing) {
        super.updateThing(thing);
    }

    @Override
    public AtomicInteger getSenderId() {
        throw new UnsupportedOperationException("ServerGatewayDeviceHandler.getSenderId()");
    }

    @Override
    public Map<Integer, ChannelAndPreviousState> getSenderIdToChannelUID() {
        throw new UnsupportedOperationException("ServerGatewayDeviceHandler.getSenderIdToChannelUID()");
    }

    @Override
    public void updateState(ChannelUID uid, State state) {
        super.updateState(uid, state);
    }

    @Override
    public void updateStatus(ThingStatus thingStatus, ThingStatusDetail thingStatusDetail, String message) {
        super.updateStatus(thingStatus, thingStatusDetail, message);
    }

    @Override
    public void updateStatus(ThingStatus thingStatus) {
        super.updateStatus(thingStatus);
    }

    @Override
    public ChannelFuture write(FromServerProto proto) {
        throw new UnsupportedOperationException("ServerGatewayDeviceHandler.write(proto)");
    }

    @Nullable
    @Override
    public String setProperty(String name, @Nullable String value) {
        return thing.setProperty(name, value);
    }
}
