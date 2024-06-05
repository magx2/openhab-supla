package pl.grzeslowski.openhab.supla.internal.cloud.executors;

import org.eclipse.jdt.annotation.NonNullByDefault;
import pl.grzeslowski.openhab.supla.internal.cloud.api.ChannelsCloudApi;

@NonNullByDefault
public interface LedCommandExecutorFactory {
    LedCommandExecutor newLedCommandExecutor(ChannelsCloudApi channelsCloudApi);
}
