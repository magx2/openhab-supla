package pl.grzeslowski.openhab.supla.internal.server.traits;

import static java.util.Objects.requireNonNull;
import static pl.grzeslowski.jsupla.protocol.api.ChannelFunction.SUPLA_CHANNELFNC_NONE;
import static pl.grzeslowski.jsupla.protocol.api.Preconditions.unsignedByteSize;
import static pl.grzeslowski.jsupla.protocol.api.ProtocolHelpers.toSignedInt;

import jakarta.annotation.Nullable;
import java.util.Arrays;
import java.util.Optional;
import java.util.Set;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import lombok.extern.slf4j.Slf4j;
import pl.grzeslowski.jsupla.protocol.api.*;
import pl.grzeslowski.jsupla.protocol.api.channeltype.decoders.HvacTypeDecoder;
import pl.grzeslowski.jsupla.protocol.api.channeltype.value.ActionTrigger;
import pl.grzeslowski.jsupla.protocol.api.channeltype.value.HvacValue;
import pl.grzeslowski.jsupla.protocol.api.structs.HVACValue;
import pl.grzeslowski.jsupla.protocol.api.structs.ds.*;

/**
 * @param number
 * @param offline
 * @param type
 * @param flags
 * @param channelFunction
 * @param rgbwBitFunctions
 * @param value
 * @param action
 * @param hvacValue
 * @param subDeviceId
 * @param valueValidityTimeSec
 * @param functions
 * @param defaultIcon see <a href="https://github.com/magx2/openhab-supla/blob/master/docs/supla/icons.md">icons.md</a>
 *     for the documentation
 */
@Slf4j
public record DeviceChannel(
        int number,
        boolean offline,
        @Nullable ChannelType type,
        Set<ChannelFlag> flags,
        @Nullable ChannelFunction channelFunction,
        Set<RgbwBitFunction> rgbwBitFunctions,
        byte[] value,
        @Nullable ActionTrigger action,
        @Nullable HvacValue hvacValue,
        @Nullable Integer subDeviceId,
        @Min(0) long valueValidityTimeSec,
        Set<BitFunction> functions,
        @Min(0) @Max(255) int defaultIcon) {

    // todo use codex to test this method
    public static DeviceChannel fromProto(SuplaDeviceChannel proto) {
        return switch (proto) {
            case SuplaDeviceChannelA r -> a(r);
            case SuplaDeviceChannelB r -> b(r);
            case SuplaDeviceChannelC r -> c(r);
            case SuplaDeviceChannelD r -> d(r);
            case SuplaDeviceChannelE r -> e(r);
        };
    }

    public DeviceChannel {
        if (value == null && hvacValue == null && action == null) {
            throw new IllegalArgumentException("value or hvacValue or action must not be null!");
        }
        requireNonNull(flags);
        requireNonNull(rgbwBitFunctions);
        requireNonNull(functions);
        unsignedByteSize(defaultIcon);
    }

    private static Integer findSubDeviceId(short subDeviceId) {
        return subDeviceId > 0 ? (int) subDeviceId : null;
    }

    private static Set<ChannelFlag> findFlags(long flags) {
        return ChannelFlag.findByMask(flags);
    }

    @Nullable
    private static ChannelType findChannelType(int type) {
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

    private static DeviceChannel a(SuplaDeviceChannelA a) {
        return new DeviceChannel(
                a.number(),
                false,
                findChannelType(a.type()),
                Set.of(),
                null,
                Set.of(),
                a.value(),
                null,
                null,
                null,
                0L,
                Set.of(),
                0);
    }

    private static DeviceChannel b(SuplaDeviceChannelB b) {
        return new DeviceChannel(
                b.number(),
                false,
                findChannelType(b.type()),
                Set.of(),
                findChannelFunction(b.defaultValue()),
                Set.of(),
                b.value(),
                null,
                null,
                null,
                0L,
                findBitFunction(b.funcList()),
                0);
    }

    private static DeviceChannel c(SuplaDeviceChannelC c) {
        return new DeviceChannel(
                c.number(),
                false,
                findChannelType(c.type()),
                findFlags(c.flags()),
                findChannelFunction(c.defaultValue()),
                Set.of(),
                c.value(),
                mapAction(c),
                mapHvacValue(c.hvacValue()),
                null,
                0L,
                findBitFunction(c.funcList()),
                0);
    }

    private static DeviceChannel d(SuplaDeviceChannelD d) {
        return new DeviceChannel(
                d.number(),
                d.offline() > 0,
                findChannelType(d.type()),
                findFlags(d.flags()),
                findChannelFunction(d.defaultValue()),
                Set.of(),
                d.value(),
                mapAction(d),
                mapHvacValue(d.hvacValue()),
                null,
                d.valueValidityTimeSec(),
                findBitFunction(d.funcList()),
                d.defaultIcon());
    }

    private static Set<BitFunction> findBitFunction(@Nullable Integer funcList) {
        return Optional.ofNullable(funcList).map(BitFunction::findByMask).orElse(Set.of());
    }

    private static DeviceChannel e(SuplaDeviceChannelE e) {
        return new DeviceChannel(
                e.number(),
                e.offline() > 0,
                findChannelType(e.type()),
                findFlags(e.flags()),
                findChannelFunction(e.defaultValue()),
                findRgbwBitFunctions(e.rGBWFuncList()),
                e.value(),
                mapAction(e),
                mapHvacValue(e.hvacValue()),
                findSubDeviceId(e.subDeviceId()),
                e.valueValidityTimeSec(),
                findBitFunction(e.funcList()),
                e.defaultIcon());
    }

    private static ChannelFunction findChannelFunction(@Nullable Integer defaultValue) {
        return Optional.ofNullable(defaultValue)
                .flatMap(ChannelFunction::findByValue)
                .orElse(SUPLA_CHANNELFNC_NONE);
    }

    private static Set<RgbwBitFunction> findRgbwBitFunctions(@Nullable Long mask) {
        if (mask == null) {
            return Set.of();
        }
        return RgbwBitFunction.findByMask(mask);
    }

    @Nullable
    private static HvacValue mapHvacValue(@Nullable HVACValue hvacValue) {
        if (hvacValue == null) {
            return null;
        }
        return HvacTypeDecoder.INSTANCE.decode(hvacValue);
    }

    @Override
    public String toString() {
        return "DeviceChannel{" +
               "number=" + number +
               ", offline=" + offline +
               ", type=" + type +
               ", flags=" + flags +
               ", channelFunction=" + channelFunction +
               ", rgbwBitFunctions=" + rgbwBitFunctions +
               ", value=" + Arrays.toString(value) +
               ", action=" + action +
               ", hvacValue=" + hvacValue +
               ", subDeviceId=" + subDeviceId +
               ", valueValidityTimeSec=" + valueValidityTimeSec +
               ", functions=" + functions +
               ", defaultIcon=" + defaultIcon +
               '}';
    }
}
