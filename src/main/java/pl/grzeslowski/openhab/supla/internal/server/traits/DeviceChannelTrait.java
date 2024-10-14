package pl.grzeslowski.openhab.supla.internal.server.traits;

import jakarta.annotation.Nullable;
import lombok.Value;
import pl.grzeslowski.jsupla.protocol.api.structs.HVACValue;
import pl.grzeslowski.jsupla.protocol.api.structs.ds.*;

@Value
public class DeviceChannelTrait {
    int number;
    int type;
    @Nullable
    byte[] value;
    @Nullable
    pl.grzeslowski.jsupla.protocol.api.structs.HVACValue hvacValue;

    public DeviceChannelTrait(int number, int type, @Nullable byte[] value, @Nullable HVACValue hvacValue) {
        this.number = number;
        this.type = type;
        this.value = value;
        this.hvacValue = hvacValue;
        if (value == null && hvacValue == null) {
            throw new IllegalArgumentException("value and hvacValue must not be null!");
        }
    }

    public DeviceChannelTrait(SuplaDeviceChannel channel) {
        this(channel.number, channel.type, channel.value, null);
    }

    public DeviceChannelTrait(SuplaDeviceChannelB channel) {
        this(channel.number, channel.type, channel.value, null);
    }

    public DeviceChannelTrait(SuplaDeviceChannelC channel) {
        this(channel.number, channel.type, channel.value, channel.hvacValue);
    }

    public DeviceChannelTrait(SuplaDeviceChannelD channel) {
        this(channel.number, channel.type, channel.value, channel.hvacValue);
    }

    public DeviceChannelTrait(SuplaDeviceChannelE channel) {
        this(channel.number, channel.type, channel.value, channel.hvacValue);
    }
}
