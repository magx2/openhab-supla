package pl.grzeslowski.openhab.supla.internal.handler;

import org.openhab.core.thing.Bridge;
import org.openhab.core.thing.binding.BaseBridgeHandler;

public abstract class SuplaBridge extends BaseBridgeHandler {
    public SuplaBridge(Bridge bridge) {
        super(bridge);
    }
}
