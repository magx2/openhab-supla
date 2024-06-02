package pl.grzeslowski.supla.openhab.internal.cloud;

import java.util.Objects;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;

@NonNullByDefault
public final class ChannelInfo {
    private final int channelId;

    @Nullable
    private final AdditionalChannelType additionalChannelType;

    @Nullable
    private final Integer idx;

    public ChannelInfo(
            final int channelId,
            final @Nullable AdditionalChannelType additionalChannelType,
            final @Nullable Integer idx) {
        this.channelId = channelId;
        this.additionalChannelType = additionalChannelType;
        this.idx = idx;
    }

    public int getChannelId() {
        return channelId;
    }

    @Nullable
    public AdditionalChannelType getAdditionalChannelType() {
        return additionalChannelType;
    }

    @Nullable
    public Integer getIdx() {
        return idx;
    }

    @Override
    public boolean equals(@Nullable final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        final ChannelInfo that = (ChannelInfo) o;
        return channelId == that.channelId
                && additionalChannelType == that.additionalChannelType
                && Objects.equals(idx, that.idx);
    }

    @Override
    public int hashCode() {
        return Objects.hash(channelId, additionalChannelType, idx);
    }

    @Override
    public String toString() {
        return "ChannelInfo{" + //
                "channelId="
                + channelId + //
                ", additionalChannelType="
                + additionalChannelType + //
                ", idx="
                + idx + //
                '}';
    }
}
