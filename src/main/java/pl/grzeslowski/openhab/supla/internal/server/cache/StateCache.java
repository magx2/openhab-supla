package pl.grzeslowski.openhab.supla.internal.server.cache;

import java.time.Duration;
import java.util.Optional;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.thing.ChannelUID;
import org.openhab.core.types.State;

public interface StateCache extends AutoCloseable {
    Optional<State> findState(ChannelUID uid);

    void saveState(ChannelUID uid, @Nullable State state, @Nullable Duration validityTime);

    @Override
    void close();
}
