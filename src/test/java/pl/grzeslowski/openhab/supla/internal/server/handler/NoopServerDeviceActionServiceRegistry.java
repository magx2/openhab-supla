package pl.grzeslowski.openhab.supla.internal.server.handler;

import java.util.Set;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.core.thing.binding.ThingActions;

@NonNullByDefault
enum NoopServerDeviceActionServiceRegistry implements ServerDeviceActionServiceRegistry {
    INSTANCE;

    @Override
    public void updateActionServices(
            ServerSuplaDeviceHandler handler, Set<Class<? extends ThingActions>> actionServices) {}

    @Override
    public void unregisterActionServices(ServerSuplaDeviceHandler handler) {}
}
