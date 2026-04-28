package pl.grzeslowski.openhab.supla.internal.server.handler;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.openhab.core.library.types.HSBType;
import org.openhab.core.library.types.OnOffType;
import org.openhab.core.library.types.OpenClosedType;
import org.openhab.core.library.types.PercentType;
import org.openhab.core.library.types.QuantityType;
import org.openhab.core.library.types.StopMoveType;
import org.openhab.core.library.types.StringType;
import org.openhab.core.library.types.UpDownType;
import org.openhab.core.thing.ChannelUID;
import org.openhab.core.thing.Thing;
import pl.grzeslowski.jsupla.protocol.api.ChannelType;
import pl.grzeslowski.jsupla.protocol.api.structs.dcs.SetCaption;
import pl.grzeslowski.jsupla.protocol.api.structs.ds.SubdeviceDetails;
import pl.grzeslowski.jsupla.protocol.api.structs.ds.SuplaChannelNewValueResult;
import pl.grzeslowski.jsupla.protocol.api.structs.dsc.ChannelState;
import pl.grzeslowski.jsupla.protocol.api.types.FromServerProto;
import pl.grzeslowski.jsupla.server.SuplaWriteFuture;
import pl.grzeslowski.openhab.supla.internal.handler.InitializationException;
import pl.grzeslowski.openhab.supla.internal.server.handler.trait.ServerBridge;
import pl.grzeslowski.openhab.supla.internal.server.handler.trait.ServerDevice;
import pl.grzeslowski.openhab.supla.internal.server.traits.DeviceChannelValue;
import pl.grzeslowski.openhab.supla.internal.server.traits.RegisterDeviceTrait;

final class TestServerSuplaDeviceHandler extends ServerSuplaDeviceHandler {
    private final Map<Long, ServerDevice.ChannelAndPreviousState> messageIdToChannelUID =
            Collections.synchronizedMap(new HashMap<>());

    TestServerSuplaDeviceHandler(Thing thing) {
        super(thing, NoopServerDeviceActionServiceRegistry.INSTANCE);
    }

    @Override
    protected List<Class<? extends ServerBridge>> findAllowedBridgeClasses() {
        return List.of();
    }

    @Override
    protected void afterRegister(RegisterDeviceTrait registerEntity) throws InitializationException {}

    @Override
    void consumeDeviceChannelValueTrait(DeviceChannelValue trait) {}

    @Override
    void consumeSuplaDeviceChannelExtendedValue(int channelNumber, ChannelType type, byte[] value) {}

    @Override
    void consumeSetCaption(SetCaption value) {}

    @Override
    void consumeChannelState(ChannelState value) {}

    @Override
    void consumeSubDeviceDetails(SubdeviceDetails value) {}

    @Override
    void consumeSuplaChannelNewValueResult(SuplaChannelNewValueResult value) {}

    @Override
    public void handleRefreshCommand(ChannelUID channelUID) {}

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
    public void handleStopMoveTypeCommand(ChannelUID channelUID, StopMoveType command) {}

    @Override
    public void handleStringCommand(ChannelUID channelUID, StringType command) {}

    @Override
    public void handleQuantityType(ChannelUID channelUID, QuantityType<?> command) {}

    @Override
    public String setProperty(String name, String value) {
        return thing.setProperty(name, value);
    }

    @Override
    public Map<Long, ChannelAndPreviousState> getMessageIdToChannelUID() {
        return messageIdToChannelUID;
    }

    @Override
    public SuplaWriteFuture write(FromServerProto proto) {
        throw new UnsupportedOperationException();
    }
}
