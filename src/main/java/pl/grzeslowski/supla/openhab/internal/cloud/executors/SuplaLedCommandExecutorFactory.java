package pl.grzeslowski.supla.openhab.internal.cloud.executors;

import org.eclipse.jdt.annotation.NonNullByDefault;
import pl.grzeslowski.supla.openhab.internal.cloud.api.ChannelsCloudApi;

@NonNullByDefault
public final class SuplaLedCommandExecutorFactory implements LedCommandExecutorFactory {
    public static final SuplaLedCommandExecutorFactory FACTORY = new SuplaLedCommandExecutorFactory();

    @Override
    public LedCommandExecutor newLedCommandExecutor(final ChannelsCloudApi channelsCloudApi) {
        return new SuplaLedCommandExecutor(channelsCloudApi);
    }
}
