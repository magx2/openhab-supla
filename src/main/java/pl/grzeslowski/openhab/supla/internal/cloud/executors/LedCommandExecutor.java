package pl.grzeslowski.openhab.supla.internal.cloud.executors;

import io.swagger.client.ApiException;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.core.library.types.HSBType;
import org.openhab.core.library.types.PercentType;

@NonNullByDefault
public interface LedCommandExecutor {
    void setLedState(final int channelId, final PercentType brightness);

    void setLedState(final int channelId, final HSBType hsb);

    void changeColor(final int channelId, final HSBType command) throws ApiException;

    void changeColorBrightness(final int channelId, final PercentType command) throws ApiException;

    void changeBrightness(final int channelId, final PercentType command) throws ApiException;
}
