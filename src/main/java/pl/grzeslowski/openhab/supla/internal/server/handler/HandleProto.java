package pl.grzeslowski.openhab.supla.internal.server.handler;

import org.eclipse.jdt.annotation.NonNullByDefault;
import pl.grzeslowski.jsupla.protocol.api.structs.dcs.SetCaption;
import pl.grzeslowski.jsupla.protocol.api.structs.dcs.SuplaPingServer;
import pl.grzeslowski.jsupla.protocol.api.structs.ds.SubdeviceDetails;
import pl.grzeslowski.jsupla.protocol.api.structs.ds.SuplaChannelNewValueResult;
import pl.grzeslowski.jsupla.protocol.api.structs.dsc.ChannelState;
import pl.grzeslowski.jsupla.server.api.Writer;
import pl.grzeslowski.openhab.supla.internal.server.traits.DeviceChannelValueTrait;

@NonNullByDefault
public interface HandleProto {
    void consumeSuplaPingServer(SuplaPingServer ping, Writer writer);

    void consumeSuplaSetActivityTimeout(Writer writer);

    void consumeDeviceChannelValueTrait(DeviceChannelValueTrait trait);

    void consumeSuplaDeviceChannelExtendedValue(int channelNumber, int type, byte[] value);

    void consumeLocalTimeRequest(Writer writer);

    void consumeSetCaption(SetCaption value);

    void consumeChannelState(ChannelState value);

    void consumeSubDeviceDetails(SubdeviceDetails value);

    void consumeSuplaChannelNewValueResult(SuplaChannelNewValueResult value);
}
