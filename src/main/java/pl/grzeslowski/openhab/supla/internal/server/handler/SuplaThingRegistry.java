package pl.grzeslowski.openhab.supla.internal.server.handler;

import java.util.Optional;

public interface SuplaThingRegistry {
    Optional<SuplaThing> findSuplaThing(String guid);
}
