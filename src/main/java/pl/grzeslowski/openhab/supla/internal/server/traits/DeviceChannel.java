package pl.grzeslowski.openhab.supla.internal.server.traits;

import static java.util.Arrays.stream;
import static pl.grzeslowski.jsupla.protocol.api.ChannelFunction.SUPLA_CHANNELFNC_NONE;

import jakarta.annotation.Nullable;
import pl.grzeslowski.jsupla.protocol.api.ChannelFunction;
import pl.grzeslowski.jsupla.protocol.api.channeltype.decoders.HVACValueDecoderImpl;
import pl.grzeslowski.jsupla.protocol.api.channeltype.value.HvacValue;
import pl.grzeslowski.jsupla.protocol.api.structs.HVACValue;
import pl.grzeslowski.jsupla.protocol.api.structs.ds.*;

public record DeviceChannel(
        int number, int type, ChannelFunction channelFunction, byte[] value, HvacValue hvacValue, Integer subDeviceId) {
    public static DeviceChannel fromProto(SuplaDeviceChannel proto) {
        return switch (proto) {
            case SuplaDeviceChannelA a -> new DeviceChannel(a.number(), a.type(), a.value());
            case SuplaDeviceChannelB b -> new DeviceChannel(b.number(), b.type(), b.funcList(), b.value(), null, null);
            case SuplaDeviceChannelC c -> new DeviceChannel(
                    c.number(), c.type(), c.funcList(), c.value(), mapHvacValue(c.hvacValue()), null);
            case SuplaDeviceChannelD d -> new DeviceChannel(
                    d.number(), d.type(), d.funcList(), d.value(), mapHvacValue(d.hvacValue()), null);
            case SuplaDeviceChannelE e -> new DeviceChannel(
                    e.number(), e.type(), e.funcList(), e.value(), mapHvacValue(e.hvacValue()), (int) e.subDeviceId());
        };
    }

    public DeviceChannel {
        if (value == null && hvacValue == null) {
            throw new IllegalArgumentException("value and hvacValue must not be null!");
        }
    }

    private DeviceChannel(int number, int type, byte[] value) {
        this(number, type, (ChannelFunction) null, value, null, null);
    }

    private DeviceChannel(
            int number,
            int type,
            @Nullable Integer channelFunction,
            @Nullable byte[] value,
            @Nullable HvacValue hvacValue,
            @Nullable Integer subDeviceId) {
        this(number, type, findChannelFunction(channelFunction), value, hvacValue, subDeviceId);
    }

    private static ChannelFunction findChannelFunction(Integer channelFunction) {
        if (channelFunction == null) {
            return SUPLA_CHANNELFNC_NONE;
        }
        return stream(ChannelFunction.values())
                .filter(cf -> cf.getValue() == channelFunction)
                .findAny()
                .orElse(SUPLA_CHANNELFNC_NONE);
    }

    @Nullable
    private static HvacValue mapHvacValue(@Nullable HVACValue hvacValue) {
        if (hvacValue == null) {
            return null;
        }
        return HVACValueDecoderImpl.INSTANCE.decode(hvacValue);
    }
}
