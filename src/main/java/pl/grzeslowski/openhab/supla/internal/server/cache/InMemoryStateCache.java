package pl.grzeslowski.openhab.supla.internal.server.cache;

import static java.time.Instant.now;
import static org.openhab.core.types.UnDefType.UNDEF;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.thing.ChannelUID;
import org.openhab.core.types.State;
import org.slf4j.Logger;

@RequiredArgsConstructor
public class InMemoryStateCache implements StateCache {
    private record CachedState(State state, @Nullable Instant validUntil) {}

    private final Map<ChannelUID, CachedState> stateCache = new HashMap<>();
    private final Logger logger;

    @Override
    public Optional<State> findState(ChannelUID uid) {
        synchronized (stateCache) {
            var cachedState = stateCache.get(uid);
            if (cachedState == null) {
                return Optional.empty();
            }
            var now = now();
            if (expired(cachedState.validUntil, now)) {
                logger.debug("State {} expired (now={}). Returning UNDEF", cachedState, now);
                return Optional.of(UNDEF);
            }
            logger.debug("Current state for {} is {}", uid, cachedState);
            return Optional.ofNullable(cachedState.state);
        }
    }

    private static boolean expired(@Nullable Instant validUntil, Instant now) {
        return validUntil != null && now.isAfter(validUntil);
    }

    @Override
    public void saveState(ChannelUID uid, @Nullable State state, @Nullable Duration validityTime) {
        synchronized (stateCache) {
            var validUntil =
                    Optional.ofNullable(validityTime).map(d -> now().plus(d)).orElse(null);
            logger.debug(
                    "Saving state {}={}, valid {} (until {})",
                    uid,
                    state,
                    validityTime != null ? validityTime : "<none>",
                    validUntil != null ? validUntil : "<none>");
            stateCache.put(uid, new CachedState(state, validUntil));
        }
    }

    @Override
    public void close() {
        synchronized (stateCache) {
            logger.debug("Clearing state cache");
            stateCache.clear();
        }
    }
}
