package pl.grzeslowski.openhab.supla.internal.cloud.api;

import org.eclipse.jdt.annotation.NonNullByDefault;

@NonNullByDefault
public interface ServerCloudApiFactory {
    ServerCloudApi newServerCloudApi(String token);
}
