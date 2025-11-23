package pl.grzeslowski.openhab.supla.internal.handler;

import static org.openhab.core.thing.ThingStatus.OFFLINE;
import static org.openhab.core.thing.ThingStatusDetail.CONFIGURATION_ERROR;
import static pl.grzeslowski.openhab.supla.internal.GuidLogger.attachGuid;

import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.thing.Bridge;
import org.openhab.core.thing.binding.BaseBridgeHandler;
import org.slf4j.Logger;

public abstract class SuplaBridge extends BaseBridgeHandler {
    public SuplaBridge(Bridge bridge) {
        super(bridge);
    }

    @Override
    public final void initialize() {
        attachGuid(findGuid(), () -> {
            try {
                internalInitialize();
            } catch (InitializationException e) {
                updateStatus(e.getStatus(), e.getStatusDetail(), e.getMessage());
            } catch (Exception e) {
                getLogger().error("Error occurred while initializing!", e);
                updateStatus(
                        OFFLINE, CONFIGURATION_ERROR, "Error occurred while initializing! " + e.getLocalizedMessage());
            }
        });
    }

    @Nullable
    protected abstract String findGuid();

    protected abstract void internalInitialize() throws InitializationException;

    protected abstract Logger getLogger();
}
