package pl.grzeslowski.supla.openhab.internal.server.traits;

import lombok.RequiredArgsConstructor;
import lombok.Value;
import pl.grzeslowski.jsupla.protocol.api.structs.ds.*;

@Value
@RequiredArgsConstructor
public class DeviceChannelTrait {
    int number;
    int type;
    byte[] value;

    public DeviceChannelTrait(SuplaDeviceChannel channel) {
        this(channel.number, channel.type, channel.value);
    }

    public DeviceChannelTrait(SuplaDeviceChannelB channel) {
        this(channel.number, channel.type, channel.value);
    }

    public DeviceChannelTrait(SuplaDeviceChannelC channel) {
        this(channel.number, channel.type, channel.value);
    }

    public DeviceChannelTrait(SuplaDeviceChannelD channel) {
        this(channel.number, channel.type, channel.value);
    }
}
