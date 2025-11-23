package pl.grzeslowski.openhab.supla.internal.handler;

import static org.openhab.core.thing.ThingStatus.OFFLINE;

import java.io.Serial;
import org.openhab.core.thing.ThingStatusDetail;

public class OfflineInitializationException extends InitializationException {
    @Serial
    private static final long serialVersionUID = 1L;

    public OfflineInitializationException(
            ThingStatusDetail statusDetail, String description, Object... descriptionArguments) {
        super(OFFLINE, statusDetail, description, descriptionArguments);
    }

    public OfflineInitializationException(ThingStatusDetail statusDetail, String description, Exception exception) {
        super(OFFLINE, statusDetail, description, exception);
    }
}
