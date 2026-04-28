package pl.grzeslowski.openhab.supla.actions;

import java.util.Optional;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.thing.binding.ThingActions;
import org.openhab.core.thing.binding.ThingHandler;
import pl.grzeslowski.openhab.supla.internal.server.handler.ServerSuplaDeviceHandler;

@NonNullByDefault
@Slf4j
abstract class SuplaServerActionsSupport implements ThingActions {
    protected static final int NO_DATA_TYPE = 0;
    protected static final byte[] EMPTY_DATA = new byte[0];
    protected static final byte SUPER_USER_AUTHORIZED = 1;
    protected static final int NOT_BOUND_TO_CHANNEL = -1;
    protected static final int SENDER_ID = 1;

    @Getter
    @Nullable
    private ServerSuplaDeviceHandler thingHandler;

    @Override
    public void setThingHandler(ThingHandler handler) {
        if (!(handler instanceof ServerSuplaDeviceHandler suplaHandler)) {
            var handlerClass = Optional.of(handler)
                    .map(ThingHandler::getClass)
                    .map(Class::getSimpleName)
                    .orElse("<null>");
            log.warn(
                    "Handler {} has wrong class, actualClass={}, expectedClass={}",
                    handler,
                    handlerClass,
                    ServerSuplaDeviceHandler.class.getSimpleName());
            return;
        }
        this.thingHandler = suplaHandler;
    }

    @Nullable
    protected ServerSuplaDeviceHandler getThingHandlerOrWarn() {
        var localHandler = thingHandler;
        if (localHandler == null) {
            log.warn("Thing handler is null!");
        }
        return localHandler;
    }
}
