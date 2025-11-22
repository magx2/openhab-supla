package pl.grzeslowski.openhab.supla.internal.server.handler;

import static java.util.Collections.synchronizedMap;
import static java.util.Objects.requireNonNull;
import static org.openhab.core.thing.ThingStatus.OFFLINE;
import static org.openhab.core.thing.ThingStatusDetail.CONFIGURATION_ERROR;
import static pl.grzeslowski.openhab.supla.internal.Documentation.THING_BRIDGE;
import static pl.grzeslowski.openhab.supla.internal.SuplaBindingConstants.SUPLA_GATEWAY_DEVICE_TYPE;

import io.netty.channel.ChannelFuture;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import lombok.Getter;
import lombok.experimental.Delegate;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.thing.ChannelUID;
import org.openhab.core.thing.Thing;
import org.openhab.core.thing.ThingStatus;
import org.openhab.core.thing.ThingStatusDetail;
import org.openhab.core.thing.binding.builder.ThingBuilder;
import org.openhab.core.types.State;
import pl.grzeslowski.jsupla.protocol.api.structs.dcs.SetCaption;
import pl.grzeslowski.jsupla.protocol.api.structs.ds.SubdeviceDetails;
import pl.grzeslowski.jsupla.protocol.api.structs.ds.SuplaChannelNewValueResult;
import pl.grzeslowski.jsupla.protocol.api.structs.dsc.ChannelState;
import pl.grzeslowski.jsupla.protocol.api.types.FromServerProto;
import pl.grzeslowski.openhab.supla.internal.server.ChannelUtil;
import pl.grzeslowski.openhab.supla.internal.server.handler.trait.HandleCommand;
import pl.grzeslowski.openhab.supla.internal.server.handler.trait.HandlerCommandTrait;
import pl.grzeslowski.openhab.supla.internal.server.handler.trait.ServerBridge;
import pl.grzeslowski.openhab.supla.internal.server.handler.trait.ServerDevice;
import pl.grzeslowski.openhab.supla.internal.server.traits.DeviceChannel;
import pl.grzeslowski.openhab.supla.internal.server.traits.DeviceChannelValue;
import pl.grzeslowski.openhab.supla.internal.server.traits.RegisterDeviceTrait;

@NonNullByDefault
public class SingleDevice extends ServerSuplaDeviceHandler implements ServerDevice {
    @Getter
    private final Map<Integer, Integer> channelTypes = synchronizedMap(new HashMap<>());

    @Getter
    private final AtomicInteger senderId = new AtomicInteger(1);

    @Getter
    private final Map<Integer, ChannelAndPreviousState> senderIdToChannelUID = synchronizedMap(new HashMap<>());

    private final ChannelUtil channelUtil = new ChannelUtil(this);

    @Delegate(types = HandleCommand.class)
    private final HandlerCommandTrait handlerCommandTrait = new HandlerCommandTrait(this);

    public SingleDevice(Thing thing) {
        super(thing);
    }

    @Override
    protected boolean afterRegister(RegisterDeviceTrait registerEntity) {
        var flags = registerEntity.flags();
        if (flags.calcfgSubdevicePairing()) {
            updateStatus(
                    OFFLINE,
                    CONFIGURATION_ERROR,
                    "Device should be created as %s. See %s for more information."
                            .formatted(SUPLA_GATEWAY_DEVICE_TYPE.getId(), THING_BRIDGE));
            return false;
        }

        channelUtil.buildChannels(registerEntity.channels());

        return true;
    }

    private void setChannels(List<DeviceChannel> deviceChannels) {}

    @Override
    public void consumeDeviceChannelValueTrait(DeviceChannelValue trait) {
        channelUtil.updateStatus(trait);
    }

    @Override
    public void consumeSuplaDeviceChannelExtendedValue(int channelNumber, int type, byte[] value) {
        channelUtil.updateStatus(channelNumber, type, value);
    }

    @Override
    public void consumeSetCaption(SetCaption value) {
        channelUtil.setCaption(value);
    }

    @Override
    public void consumeChannelState(ChannelState value) {
        channelUtil.consumeChannelState(value);
    }

    @Override
    public void consumeSubDeviceDetails(SubdeviceDetails value) {
        logger.warn("Not supporting `consumeSubDeviceDetails({})`", value);
    }

    @Override
    public void consumeSuplaChannelNewValueResult(SuplaChannelNewValueResult value) {
        channelUtil.consumeSuplaChannelNewValueResult(value);
    }

    @Override
    protected List<Class<? extends ServerBridge>> findAllowedBridgeClasses() {
        return List.of(ServerBridgeHandler.class, GatewayDeviceHandler.class);
    }

    @Override
    public void updateState(ChannelUID uid, State state) {
        super.updateState(uid, state);
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
    public void updateStatus(ThingStatus thingStatus, ThingStatusDetail thingStatusDetail, String message) {
        super.updateStatus(thingStatus, thingStatusDetail, message);
    }

    @Override
    public void updateStatus(ThingStatus thingStatus) {
        super.updateStatus(thingStatus);
    }

    @Override
    public ChannelFuture write(FromServerProto proto) {
        var writer = requireNonNull(getWriter().get(), "There is no writer!");
        logger.debug("Writing proto {}", proto);
        return writer.write(proto);
    }

    @Nullable
    @Override
    public String setProperty(String name, @Nullable String value) {
        return thing.setProperty(name, value);
    }
}
