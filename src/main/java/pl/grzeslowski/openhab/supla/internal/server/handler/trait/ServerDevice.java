package pl.grzeslowski.openhab.supla.internal.server.handler.trait;

import io.netty.channel.ChannelFuture;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
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
import pl.grzeslowski.openhab.supla.internal.server.cache.StateCache;

@NonNullByDefault
public interface ServerDevice extends HandleCommand, StateCache {
    Logger getLogger();

    Thing getThing();

    Map<Integer, Integer> getChannelTypes();

    ThingBuilder editThing();

    void updateThing(Thing thing);

    @Nullable
    ServerBridge getBridgeHandler();

    AtomicInteger getSenderId();

    Map<Integer, ChannelAndPreviousState> getSenderIdToChannelUID();

    void updateState(ChannelUID uid, State state);

    void updateStatus(ThingStatus thingStatus, ThingStatusDetail thingStatusDetail, String message);

    void updateStatus(ThingStatus thingStatus);

    ChannelFuture write(FromServerProto proto);

    String setProperty(String name, @Nullable String value);

    public static record ChannelAndPreviousState(
            ChannelUID channelUID, @Nullable State previousState) {}
}
