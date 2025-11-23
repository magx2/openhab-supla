package pl.grzeslowski.openhab.supla.internal.handler;

import java.io.Serial;
import lombok.Getter;
import lombok.ToString;
import org.openhab.core.thing.ThingStatus;
import org.openhab.core.thing.ThingStatusDetail;

@Getter
@ToString
public class InitializationException extends Exception {
    @Serial
    private static final long serialVersionUID = 1L;

    private final ThingStatus status;
    private final ThingStatusDetail statusDetail;
    private final Object[] descriptionArguments;

    public InitializationException(
            ThingStatus status, ThingStatusDetail statusDetail, String description, Object... descriptionArguments) {
        super(description);
        this.status = status;
        this.statusDetail = statusDetail;
        this.descriptionArguments = descriptionArguments;
    }

    public InitializationException(
            ThingStatus status,
            ThingStatusDetail statusDetail,
            String description,
            Exception exception,
            Object... descriptionArguments) {
        super(description, exception);
        this.status = status;
        this.statusDetail = statusDetail;
        this.descriptionArguments = descriptionArguments;
    }
}
