package pl.grzeslowski.openhab.supla.internal.server.cache;

import static java.util.Collections.synchronizedMap;

import java.util.HashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.thing.ChannelUID;
import org.openhab.core.types.State;
import org.slf4j.Logger;

@RequiredArgsConstructor
public class InMemoryStateCache implements StateCache {
    private final Map<ChannelUID, State> stateCache = synchronizedMap(new HashMap<>());
    private final Logger logger;

    @Override
    public @Nullable State findState(ChannelUID uid) {
        var state = stateCache.get(uid);
        logger.debug("Current state for {} is {}", uid, state);
        return state;
    }

    @Override
    public void saveState(ChannelUID uid, @Nullable State state) {
        logger.debug("Saving state {}={}", uid, state);
        stateCache.put(uid, state);
    }
}
