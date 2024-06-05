package pl.grzeslowski.openhab.supla.internal.cloud.api;

import org.eclipse.jdt.annotation.NonNullByDefault;

@FunctionalInterface
@NonNullByDefault
public interface ChannelsCloudApiFactory {
    ChannelsCloudApi newChannelsCloudApi(String token);
}
