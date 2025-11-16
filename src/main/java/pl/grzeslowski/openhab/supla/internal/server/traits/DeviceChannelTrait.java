package pl.grzeslowski.openhab.supla.internal.server.traits;

import static java.util.Arrays.stream;
import static pl.grzeslowski.jsupla.protocol.api.ChannelFunction.SUPLA_CHANNELFNC_NONE;

import jakarta.annotation.Nullable;
import lombok.Value;
import pl.grzeslowski.jsupla.protocol.api.ChannelFunction;
import pl.grzeslowski.jsupla.protocol.api.channeltype.decoders.HVACValueDecoderImpl;
import pl.grzeslowski.jsupla.protocol.api.channeltype.value.HvacValue;
import pl.grzeslowski.jsupla.protocol.api.structs.HVACValue;
import pl.grzeslowski.jsupla.protocol.api.structs.ds.*;

@Value
public class DeviceChannelTrait {
    int number;
    int type;
    ChannelFunction channelFunction;

    @Nullable
    byte[] value;

    @Nullable
    HvacValue hvacValue;

    @Nullable
    Integer subDeviceId;

    public DeviceChannelTrait(
            int number,
            int type,
            @Nullable Integer channelFunction,
            @Nullable byte[] value,
            @Nullable HvacValue hvacValue,
            @Nullable Integer subDeviceId) {
        this.number = number;
        this.type = type;
        if (channelFunction != null) {
            this.channelFunction = stream(ChannelFunction.values())
                    .filter(cf -> cf.getValue() == channelFunction)
                    .findAny()
                    .orElse(SUPLA_CHANNELFNC_NONE);
        } else {
            this.channelFunction = SUPLA_CHANNELFNC_NONE;
        }
        this.value = value;
        this.hvacValue = hvacValue;
        if (value == null && hvacValue == null) {
            throw new IllegalArgumentException("value and hvacValue must not be null!");
        }
        this.subDeviceId = subDeviceId;
    }

    public DeviceChannelTrait(SuplaDeviceChannel channel) {
        this(channel.number(), channel.type(), null, channel.value(), null, null);
    }

    public DeviceChannelTrait(SuplaDeviceChannelB channel) {
        this(channel.number(), channel.type(), channel.funcList(), channel.value(), null, null);
    }

    public DeviceChannelTrait(SuplaDeviceChannelC channel) {
        this(
                channel.number(),
                channel.type(),
                channel.funcList(),
                channel.value(),
                mapHvacValue(channel.hvacValue()),
                null);
    }

    public DeviceChannelTrait(SuplaDeviceChannelD channel) {
        this(
                channel.number(),
                channel.type(),
                channel.funcList(),
                channel.value(),
                mapHvacValue(channel.hvacValue()),
                null);
    }

    public DeviceChannelTrait(SuplaDeviceChannelE channel) {
        this(
                channel.number(),
                channel.type(),
                channel.funcList(),
                channel.value(),
                mapHvacValue(channel.hvacValue()),
                (int) channel.subDeviceId());
    }

    @Nullable
    private static HvacValue mapHvacValue(@Nullable HVACValue hvacValue) {
        if (hvacValue == null) {
            return null;
        }
        return HVACValueDecoderImpl.INSTANCE.decode(hvacValue);
    }
}
