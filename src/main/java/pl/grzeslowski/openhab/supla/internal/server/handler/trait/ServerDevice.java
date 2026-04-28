package pl.grzeslowski.openhab.supla.internal.server.handler.trait;

import java.util.Map;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.thing.ChannelUID;
import org.openhab.core.thing.Thing;
import org.openhab.core.thing.ThingStatus;
import org.openhab.core.thing.ThingStatusDetail;
import org.openhab.core.thing.binding.builder.ThingBuilder;
import org.openhab.core.types.State;
import org.slf4j.Logger;
import pl.grzeslowski.jsupla.protocol.api.types.FromServerProto;
import pl.grzeslowski.jsupla.server.SuplaWriteFuture;
import pl.grzeslowski.openhab.supla.internal.server.cache.StateCache;

@NonNullByDefault
public interface ServerDevice extends HandleCommand, StateCache {
    static final int SENDER_ID = 0;

    Logger getLogger();

    Thing getThing();

    ThingBuilder editThing();

    void updateThing(Thing thing);

    @Nullable
    ServerBridge getBridgeHandler();

    Map<Long, ChannelAndPreviousState> getMessageIdToChannelUID();

    void updateState(ChannelUID uid, State state);

    void updateStatus(ThingStatus thingStatus, ThingStatusDetail thingStatusDetail, String message);

    void updateStatus(ThingStatus thingStatus);

    SuplaWriteFuture write(FromServerProto proto);

    @Nullable
    String setProperty(String name, @Nullable String value);

    default boolean hasRegisteredDeviceChannel(int channelNumber) {
        return false;
    }

    default boolean hasRegisteredElectricityMeterChannel(int channelNumber) {
        return false;
    }

    public static record ChannelAndPreviousState(
            ChannelUID channelUID, @Nullable State previousState) {}
}
