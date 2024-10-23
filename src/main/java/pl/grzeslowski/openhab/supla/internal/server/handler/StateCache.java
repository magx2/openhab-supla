package pl.grzeslowski.openhab.supla.internal.server.handler;

import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.thing.ChannelUID;
import org.openhab.core.types.State;

public interface StateCache {
    @Nullable
    State findState(ChannelUID uid);

    void saveState(ChannelUID uid, @Nullable State state);
}
