package pl.grzeslowski.openhab.supla.internal.server.handler;

import static java.util.Collections.synchronizedMap;
import static java.util.Objects.requireNonNull;
import static org.openhab.core.thing.ThingStatusDetail.CONFIGURATION_ERROR;
import static pl.grzeslowski.openhab.supla.internal.Documentation.THING_BRIDGE;
import static pl.grzeslowski.openhab.supla.internal.Localization.text;
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
import org.openhab.core.thing.Thing;
import pl.grzeslowski.jsupla.protocol.api.ChannelType;
import pl.grzeslowski.jsupla.protocol.api.structs.dcs.SetCaption;
import pl.grzeslowski.jsupla.protocol.api.structs.ds.SubdeviceDetails;
import pl.grzeslowski.jsupla.protocol.api.structs.ds.SuplaChannelNewValueResult;
import pl.grzeslowski.jsupla.protocol.api.structs.dsc.ChannelState;
import pl.grzeslowski.jsupla.protocol.api.types.FromServerProto;
import pl.grzeslowski.openhab.supla.internal.handler.OfflineInitializationException;
import pl.grzeslowski.openhab.supla.internal.server.ChannelUtil;
import pl.grzeslowski.openhab.supla.internal.server.handler.trait.HandleCommand;
import pl.grzeslowski.openhab.supla.internal.server.handler.trait.HandlerCommandTrait;
import pl.grzeslowski.openhab.supla.internal.server.handler.trait.ServerBridge;
import pl.grzeslowski.openhab.supla.internal.server.traits.DeviceChannelValue;
import pl.grzeslowski.openhab.supla.internal.server.traits.RegisterDeviceTrait;

@NonNullByDefault
public class SingleDevice extends ServerSuplaDeviceHandler {
    @Getter
    private final Map<Integer, ChannelType> channelTypes = synchronizedMap(new HashMap<>());

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
    protected void afterRegister(RegisterDeviceTrait registerEntity) throws OfflineInitializationException {
        var flags = registerEntity.flags();
        if (flags.calcfgSubdevicePairing()) {
            throw new OfflineInitializationException(
                    CONFIGURATION_ERROR,
                    text("supla.offline.should-be-gateway", SUPLA_GATEWAY_DEVICE_TYPE.getId(), THING_BRIDGE));
        }

        channelUtil.buildChannels(registerEntity.channels());
    }

    @Override
    public void consumeDeviceChannelValueTrait(DeviceChannelValue trait) {
        channelUtil.updateStatus(trait);
    }

    @Override
    public void consumeSuplaDeviceChannelExtendedValue(int channelNumber, ChannelType type, byte[] value) {
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
