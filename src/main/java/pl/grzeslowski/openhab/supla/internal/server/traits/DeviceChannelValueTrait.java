package pl.grzeslowski.openhab.supla.internal.server.traits;

import jakarta.annotation.Nullable;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import pl.grzeslowski.jsupla.protocol.api.structs.ds.SuplaDeviceChannelValue;
import pl.grzeslowski.jsupla.protocol.api.structs.ds.SuplaDeviceChannelValueB;
import pl.grzeslowski.jsupla.protocol.api.structs.ds.SuplaDeviceChannelValueC;

@RequiredArgsConstructor
@Value
public class DeviceChannelValueTrait {
    int channelNumber;
    byte[] value;
    // B
    /** If true then value is ignored */
    boolean offline;
    // C
    /** uint */
    @Nullable
    Long validityTimeSec;

    public DeviceChannelValueTrait(SuplaDeviceChannelValue value) {
        this(value.channelNumber(), value.value(), false, null);
    }

    public DeviceChannelValueTrait(SuplaDeviceChannelValueB value) {
        this(value.channelNumber(), value.value(), value.offline() != 0, null);
    }

    public DeviceChannelValueTrait(SuplaDeviceChannelValueC value) {
        this(value.channelNumber(), value.value(), value.offline() != 0, value.validityTimeSec());
    }
}
