package pl.grzeslowski.openhab.supla.internal.server.traits;

import static java.util.Arrays.stream;
import static pl.grzeslowski.jsupla.protocol.api.ChannelFunction.SUPLA_CHANNELFNC_NONE;
import static pl.grzeslowski.jsupla.protocol.api.ProtocolHelpers.toSignedInt;

import jakarta.annotation.Nullable;
import java.util.Arrays;
import pl.grzeslowski.jsupla.protocol.api.ChannelFunction;
import pl.grzeslowski.jsupla.protocol.api.channeltype.decoders.HVACValueDecoderImpl;
import pl.grzeslowski.jsupla.protocol.api.channeltype.value.ActionTrigger;
import pl.grzeslowski.jsupla.protocol.api.channeltype.value.HvacValue;
import pl.grzeslowski.jsupla.protocol.api.structs.HVACValue;
import pl.grzeslowski.jsupla.protocol.api.structs.ds.*;

public record DeviceChannel(
        int number,
        int type,
        ChannelFunction channelFunction,
        byte[] value,
        ActionTrigger action,
        HvacValue hvacValue,
        Integer subDeviceId) {

    public static DeviceChannel fromProto(SuplaDeviceChannel proto) {
        return switch (proto) {
            case SuplaDeviceChannelA r -> new DeviceChannel(r.number(), r.type(), r.value());
            case SuplaDeviceChannelB r -> new DeviceChannel(r.number(), r.type(), r.funcList(), r.value(), null, null);
            case SuplaDeviceChannelC r ->
                new DeviceChannel(
                        r.number(), r.type(), r.funcList(), r.value(), mapAction(r), mapHvacValue(r.hvacValue()), null);
            case SuplaDeviceChannelD r ->
                new DeviceChannel(
                        r.number(), r.type(), r.funcList(), r.value(), mapAction(r), mapHvacValue(r.hvacValue()), null);
            case SuplaDeviceChannelE r ->
                new DeviceChannel(
                        r.number(), r.type(), r.funcList(), r.value(), mapAction(r), mapHvacValue(r.hvacValue()), (int)
                                r.subDeviceId());
        };
    }

    @Nullable
    private static ActionTrigger mapAction(SuplaDeviceChannelC r) {
        if (r.actionTriggerCaps() == null || r.actionTriggerProperties() == null) {
            return null;
        }
        return new ActionTrigger(toSignedInt(r.actionTriggerCaps()));
    }

    @Nullable
    private static ActionTrigger mapAction(SuplaDeviceChannelD r) {
        if (r.actionTriggerCaps() == null || r.actionTriggerProperties() == null) {
            return null;
        }
        return new ActionTrigger(toSignedInt(r.actionTriggerCaps()));
    }

    @Nullable
    private static ActionTrigger mapAction(SuplaDeviceChannelE r) {
        if (r.actionTriggerCaps() == null || r.actionTriggerProperties() == null) {
            return null;
        }
        return new ActionTrigger(toSignedInt(r.actionTriggerCaps()));
    }

    public DeviceChannel {
        if (value == null && hvacValue == null && action == null) {
            throw new IllegalArgumentException("value or hvacValue or action must not be null!");
        }
    }

    // Used by type A
    private DeviceChannel(int number, int type, byte[] value) {
        this(number, type, (ChannelFunction) null, value, null, null, null);
    }

    // Used by type B
    private DeviceChannel(
            int number,
            int type,
            @Nullable Integer channelFunction,
            @Nullable byte[] value,
            @Nullable HvacValue hvacValue,
            @Nullable Integer subDeviceId) {
        this(number, type, findChannelFunction(channelFunction), value, null, hvacValue, subDeviceId);
    }

    // Used by type C, D, E
    private DeviceChannel(
            int number,
            int type,
            @Nullable Integer channelFunction,
            @Nullable byte[] value,
            @Nullable ActionTrigger action,
            @Nullable HvacValue hvacValue,
            @Nullable Integer subDeviceId) {
        this(number, type, findChannelFunction(channelFunction), value, action, hvacValue, subDeviceId);
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

    @Override
    public String toString() {
        return "DeviceChannel{" + "number="
                + number + ", type="
                + type + ", channelFunction="
                + channelFunction + ", value="
                + Arrays.toString(value) + ", action="
                + action + ", hvacValue="
                + hvacValue + ", subDeviceId="
                + subDeviceId + '}';
    }
}
