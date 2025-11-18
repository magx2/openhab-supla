package pl.grzeslowski.openhab.supla.internal.server.traits;

import jakarta.annotation.Nullable;
import pl.grzeslowski.jsupla.protocol.api.structs.ds.SuplaDeviceChannelValue;
import pl.grzeslowski.jsupla.protocol.api.structs.ds.SuplaDeviceChannelValueB;
import pl.grzeslowski.jsupla.protocol.api.structs.ds.SuplaDeviceChannelValueC;
import pl.grzeslowski.jsupla.protocol.api.structs.ds.SuplaDeviceChannelValueTrait;

/**
 * @param channelNumber
 * @param value
 * @param offline If true then value is ignored
 * @param validityTimeSec uint
 */
public record DeviceChannelValue(int channelNumber, byte[] value, boolean offline, @Nullable Long validityTimeSec) {
    public static DeviceChannelValue fromProto(SuplaDeviceChannelValueTrait proto) {
        return switch (proto) {
            case SuplaDeviceChannelValue a -> new DeviceChannelValue(a.channelNumber(), a.value(), false, null);
            case SuplaDeviceChannelValueB b -> new DeviceChannelValue(
                    b.channelNumber(), b.value(), b.offline() != 0, null);
            case SuplaDeviceChannelValueC c -> new DeviceChannelValue(
                    c.channelNumber(), c.value(), c.offline() != 0, c.validityTimeSec());
        };
    }
}
