package pl.grzeslowski.openhab.supla.internal.server.handler;

import org.eclipse.jdt.annotation.NonNullByDefault;
import pl.grzeslowski.jsupla.protocol.api.structs.dcs.SetCaption;
import pl.grzeslowski.jsupla.protocol.api.structs.dcs.SuplaPingServer;
import pl.grzeslowski.jsupla.protocol.api.structs.ds.*;
import pl.grzeslowski.jsupla.protocol.api.structs.dsc.ChannelState;
import pl.grzeslowski.jsupla.server.SuplaWriter;
import pl.grzeslowski.openhab.supla.internal.server.traits.DeviceChannelValueTrait;

@NonNullByDefault
public interface HandleProto {
    void consumeSuplaPingServer(SuplaPingServer ping, SuplaWriter writer);

    void consumeSuplaSetActivityTimeout(SuplaWriter writer);

    void consumeDeviceChannelValueTrait(DeviceChannelValueTrait trait);

    void consumeSuplaDeviceChannelExtendedValue(int channelNumber, int type, byte[] value);

    void consumeLocalTimeRequest(SuplaWriter writer);

    void consumeSetCaption(SetCaption value);

    void consumeChannelState(ChannelState value);

    void consumeSubDeviceDetails(SubdeviceDetails value);

    void consumeSuplaChannelNewValueResult(SuplaChannelNewValueResult value);

    void consumeSetDeviceConfigResult(SetDeviceConfigResult value);

    void consumeSetDeviceConfig(SetDeviceConfig value);

    void consumeSetChannelConfigResult(SetChannelConfigResult value);
}
