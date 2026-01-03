package pl.grzeslowski.openhab.supla.internal.server.traits;

import static java.util.Arrays.stream;
import static pl.grzeslowski.jsupla.protocol.api.ChannelFunction.SUPLA_CHANNELFNC_NONE;
import static pl.grzeslowski.jsupla.protocol.api.ProtocolHelpers.toSignedInt;

import jakarta.annotation.Nullable;
import java.util.Arrays;
import java.util.Set;
import pl.grzeslowski.jsupla.protocol.api.ChannelFlag;
import pl.grzeslowski.jsupla.protocol.api.ChannelFunction;
import pl.grzeslowski.jsupla.protocol.api.ChannelType;
import pl.grzeslowski.jsupla.protocol.api.channeltype.decoders.HVACValueDecoderImpl;
import pl.grzeslowski.jsupla.protocol.api.channeltype.value.ActionTrigger;
import pl.grzeslowski.jsupla.protocol.api.channeltype.value.HvacValue;
import pl.grzeslowski.jsupla.protocol.api.structs.HVACValue;
import pl.grzeslowski.jsupla.protocol.api.structs.ds.*;

public record DeviceChannel(
        int number,
        ChannelType type,
        Set<ChannelFlag> flags,
        @Nullable ChannelFunction channelFunction,
        byte[] value,
        ActionTrigger action,
        HvacValue hvacValue,
        @Nullable Integer subDeviceId) {

    public static DeviceChannel fromProto(SuplaDeviceChannel proto) {
        return switch (proto) {
            case SuplaDeviceChannelA r -> new DeviceChannel(r.number(), finChannelType(r.type()), r.value());
            case SuplaDeviceChannelB r ->
                new DeviceChannel(r.number(), finChannelType(r.type()), r.funcList(), r.value(), null, null);
            case SuplaDeviceChannelC r ->
                new DeviceChannel(
                        r.number(),
                        finChannelType(r.type()),
                        findFlags(r.flags()),
                        r.funcList(),
                        r.value(),
                        mapAction(r),
                        mapHvacValue(r.hvacValue()),
                        null);
            case SuplaDeviceChannelD r ->
                new DeviceChannel(
                        r.number(),
                        finChannelType(r.type()),
                        findFlags(r.flags()),
                        r.funcList(),
                        r.value(),
                        mapAction(r),
                        mapHvacValue(r.hvacValue()),
                        null);
            case SuplaDeviceChannelE r ->
                new DeviceChannel(
                        r.number(),
                        finChannelType(r.type()),
                        findFlags(r.flags()),
                        r.funcList(),
                        r.value(),
                        mapAction(r),
                        mapHvacValue(r.hvacValue()),
                        findSubDeviceId(r.subDeviceId()));
        };
    }

    private static Integer findSubDeviceId(short subDeviceId) {
        return subDeviceId > 0 ? (int) subDeviceId : null;
    }

    private static Set<ChannelFlag> findFlags(long flags) {
        return ChannelFlag.findByMask(flags);
    }

    @Nullable
    private static ChannelType finChannelType(int type) {
        return ChannelType.findByValue(type).orElse(null);
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
    private DeviceChannel(int number, ChannelType type, byte[] value) {
        this(number, type, Set.of(), (ChannelFunction) null, value, null, null, null);
    }

    // Used by type B
    private DeviceChannel(
            int number,
            ChannelType type,
            @Nullable Integer channelFunction,
            @Nullable byte[] value,
            @Nullable HvacValue hvacValue,
            @Nullable Integer subDeviceId) {
        this(number, type, Set.of(), findChannelFunction(channelFunction), value, null, hvacValue, subDeviceId);
    }

    // Used by type C, D, E
    private DeviceChannel(
            int number,
            ChannelType type,
            Set<ChannelFlag> flags,
            @Nullable Integer channelFunction,
            @Nullable byte[] value,
            @Nullable ActionTrigger action,
            @Nullable HvacValue hvacValue,
            @Nullable Integer subDeviceId) {
        this(number, type, flags, findChannelFunction(channelFunction), value, action, hvacValue, subDeviceId);
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
        return "DeviceChannel{"
                + "number=" + number
                + ", type=" + type
                + ", flags=" + flags
                + ", channelFunction=" + channelFunction
                + ", value=" + Arrays.toString(value)
                + ", action=" + action
                + ", hvacValue=" + hvacValue
                + ", subDeviceId=" + subDeviceId
                + '}';
    }
}
