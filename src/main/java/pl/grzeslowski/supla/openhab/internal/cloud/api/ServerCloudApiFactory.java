package pl.grzeslowski.supla.openhab.internal.cloud.api;

import org.eclipse.jdt.annotation.NonNullByDefault;

@NonNullByDefault
public interface ServerCloudApiFactory {
    ServerCloudApi newServerCloudApi(String token);
}
