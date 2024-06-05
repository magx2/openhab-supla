package pl.grzeslowski.openhab.supla.internal.cloud.api;

import io.swagger.client.model.ServerInfo;
import java.time.ZonedDateTime;
import org.eclipse.jdt.annotation.NonNullByDefault;

@NonNullByDefault
public interface ServerCloudApi {
    ServerInfo getServerInfo() throws Exception;

    ApiCalls getApiCalls();

    public record ApiCalls(
            ZonedDateTime resetDateTime,
            int limit,
            int apiCalls,
            int remainingApiCalls,
            double requestPerSecond,
            double requestPerMinute,
            double requestPerHour) {}
}
