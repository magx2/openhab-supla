package pl.grzeslowski.openhab.supla.internal.cloud.api;

import static com.squareup.okhttp.logging.HttpLoggingInterceptor.Level.BODY;

import io.swagger.client.ApiClient;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.slf4j.Logger;

@NonNullByDefault
public final class CloudApiClientFactory implements ApiClientFactory {
    @Override
    public ApiClient newApiClient(String token, @Nullable Logger logger) {
        final ApiClient apiClient = pl.grzeslowski.jsupla.api.internal.ApiClientFactory.INSTANCE.newApiClient(token);
        if (logger != null) {
            if (logger.isDebugEnabled()) {
                apiClient.getHttpClient().interceptors().add(new OneLineHttpLoggingInterceptor(logger::trace, BODY));
            }
        }
        return apiClient;
    }
}
