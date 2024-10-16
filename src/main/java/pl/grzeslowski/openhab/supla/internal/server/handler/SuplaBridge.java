package pl.grzeslowski.openhab.supla.internal.server.handler;

import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.thing.binding.BridgeHandler;

public interface SuplaBridge extends BridgeHandler {
    TimeoutConfiguration getTimeoutConfiguration();

    AuthData getAuthData();

    void deviceDisconnected();

    void deviceConnected();

    static AuthData buildAuthData(ServerBridgeHandlerConfig config) {
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

    static TimeoutConfiguration buildTimeoutConfiguration(ServerBridgeHandlerConfig config) {
        return new TimeoutConfiguration(
                config.getTimeout().intValue(),
                config.getTimeoutMin().intValue(),
                config.getTimeoutMax().intValue());
    }
}
