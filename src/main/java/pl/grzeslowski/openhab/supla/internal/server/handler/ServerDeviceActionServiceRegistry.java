package pl.grzeslowski.openhab.supla.internal.server.handler;

import java.util.Set;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.core.thing.binding.ThingActions;

@NonNullByDefault
public interface ServerDeviceActionServiceRegistry {
    void updateActionServices(ServerSuplaDeviceHandler handler, Set<Class<? extends ThingActions>> actionServices);

    void unregisterActionServices(ServerSuplaDeviceHandler handler);
}
