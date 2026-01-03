package pl.grzeslowski.openhab.supla.internal.server.nativesupla;

import jakarta.annotation.Nullable;

import java.util.Comparator;
import java.util.EnumSet;
import java.util.Objects;
import java.util.Set;
import pl.grzeslowski.jsupla.protocol.api.ChannelFlag;
import pl.grzeslowski.jsupla.protocol.api.ChannelFunction;
import pl.grzeslowski.jsupla.protocol.api.ChannelType;

import static java.util.Comparator.comparing;
import static java.util.Comparator.nullsLast;
import static java.util.Objects.requireNonNull;

public record NativeSuplaChannel(
        int number,
        ChannelType type,
        Set<ChannelFlag> flags,
        @Nullable ChannelFunction channelFunction,
        @Nullable Integer subDeviceId) implements Comparable<NativeSuplaChannel> {
    public NativeSuplaChannel {
        if(number<0) {
            throw new IllegalArgumentException("number must be non-negative");
        }
        requireNonNull(type);
        flags = EnumSet.copyOf(requireNonNull(flags));
        if(subDeviceId!= null && subDeviceId <=0 ) {
            throw new IllegalArgumentException("subDeviceId must be null or positive");
        }
    }

    @Override
    public int compareTo(NativeSuplaChannel o) {
        return comparing(NativeSuplaChannel::number)
                .thenComparing(NativeSuplaChannel::subDeviceId, nullsLast(Integer::compareTo))
                .compare(this, o);
    }

    @Override
    public final boolean equals(Object o) {
        if (!(o instanceof NativeSuplaChannel that)) return false;

        return number == that.number && Objects.equals(subDeviceId, that.subDeviceId);
    }

    @Override
    public int hashCode() {
        int result = number;
        result = 31 * result + Objects.hashCode(subDeviceId);
        return result;
    }
}
