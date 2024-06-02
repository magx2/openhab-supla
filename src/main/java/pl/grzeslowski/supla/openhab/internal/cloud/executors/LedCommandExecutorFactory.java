package pl.grzeslowski.supla.openhab.internal.cloud.executors;

import org.eclipse.jdt.annotation.NonNullByDefault;
import pl.grzeslowski.supla.openhab.internal.cloud.api.ChannelsCloudApi;

@NonNullByDefault
public interface LedCommandExecutorFactory {
    LedCommandExecutor newLedCommandExecutor(ChannelsCloudApi channelsCloudApi);
}
