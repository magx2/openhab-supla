package pl.grzeslowski.openhab.supla.internal.server.handler.trait;

import java.time.DateTimeException;
import java.time.Duration;
import java.util.Optional;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.thing.binding.BridgeHandler;
import pl.grzeslowski.openhab.supla.internal.server.oh_config.AuthData;
import pl.grzeslowski.openhab.supla.internal.server.oh_config.ServerBridgeHandlerConfiguration;
import pl.grzeslowski.openhab.supla.internal.server.oh_config.TimeoutConfiguration;

public interface ServerBridge extends BridgeHandler {
    TimeoutConfiguration getTimeoutConfiguration();

    AuthData getAuthData();

    void deviceDisconnected();

    void deviceConnected();

    static AuthData buildAuthData(ServerBridgeHandlerConfiguration config) {
        AuthData.@Nullable LocationAuthData locationAuthData;
        if (config.getServerAccessId() != null && config.getServerAccessIdPassword() != null) {
            locationAuthData = new AuthData.LocationAuthData(
                    config.getServerAccessId().intValue(), config.getServerAccessIdPassword());
        } else {
            locationAuthData = null;
        }
        AuthData.@Nullable EmailAuthData emailAuthData;
        if (config.getEmail() != null) {
            emailAuthData = new AuthData.EmailAuthData(config.getEmail(), config.getAuthKey());
        } else {
            emailAuthData = null;
        }
        return new AuthData(locationAuthData, emailAuthData);
    }

    static TimeoutConfiguration buildTimeoutConfiguration(ServerBridgeHandlerConfiguration config) {
        return new TimeoutConfiguration(
                parseDuration(config.getTimeout()),
                parseDuration(config.getTimeoutMin()),
                parseDuration(config.getTimeoutMax()));
    }

    static Duration parseDuration(@Nullable String timeout) {
        return tryParseDuration(timeout)
                .orElseThrow(() -> new IllegalArgumentException("Cannot parse duration from [%s]".formatted(timeout)));
    }

    static Optional<Duration> tryParseDuration(@Nullable String timeout) {
        if (timeout == null) {
            return Optional.empty();
        }
        try {
            var i = Integer.parseInt(timeout);
            return Optional.of(Duration.ofSeconds(i));
        } catch (NumberFormatException __) {
            try {
                return Optional.of(Duration.parse(timeout));
            } catch (DateTimeException ___) {
                return Optional.empty();
            }
        }
    }
}
