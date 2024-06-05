package pl.grzeslowski.openhab.supla.internal.cloud.api;

import io.swagger.client.ApiClient;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.slf4j.Logger;

@NonNullByDefault
public interface ApiClientFactory {
    ApiClient newApiClient(String token, @Nullable Logger logger);
}
