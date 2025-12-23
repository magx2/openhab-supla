package pl.grzeslowski.openhab.supla.internal.extension.supla;

import org.openhab.core.thing.binding.BridgeHandler;
import org.openhab.core.thing.binding.ThingHandler;
import org.openhab.core.thing.internal.BridgeImpl;
import org.openhab.core.thing.internal.ThingImpl;
import pl.grzeslowski.openhab.supla.internal.OpenHabDevice;

public sealed interface Ctx {
    OpenHabDevice openHabDevice();

    record ThingCtx(ThingImpl thing, ThingHandler handler, OpenHabDevice openHabDevice) implements Ctx {}

    record BridgeCtx(BridgeImpl thing, BridgeHandler handler, OpenHabDevice openHabDevice) implements Ctx {}
}
