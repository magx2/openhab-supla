package pl.grzeslowski.openhab.supla.internal.server.handler.trait;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import pl.grzeslowski.openhab.supla.internal.server.oh_config.AuthData;
import pl.grzeslowski.openhab.supla.internal.server.oh_config.ServerBridgeHandlerConfiguration;
import pl.grzeslowski.openhab.supla.internal.server.oh_config.TimeoutConfiguration;

class SuplaBridgeTest {

    @Test
    void shouldBuildAuthDataWithLocationAndEmail() {
        var config = new ServerBridgeHandlerConfiguration();
        config.setServerAccessId(BigDecimal.valueOf(123));
        config.setServerAccessIdPassword("secret");
        config.setEmail("user@example.com");
        config.setAuthKey("key");

        AuthData authData = SuplaBridge.buildAuthData(config);

        assertThat(authData.locationAuthData()).isNotNull();
        assertThat(authData.locationAuthData().serverAccessId()).isEqualTo(123);
        assertThat(authData.locationAuthData().serverAccessIdPassword()).isEqualTo("secret");
        assertThat(authData.emailAuthData()).isNotNull();
        assertThat(authData.emailAuthData().email()).isEqualTo("user@example.com");
        assertThat(authData.emailAuthData().authKey()).isEqualTo("key");
    }

    @Test
    void shouldBuildAuthDataOnlyWithEmail() {
        var config = new ServerBridgeHandlerConfiguration();
        config.setEmail("user@example.com");
        config.setAuthKey("key");

        AuthData authData = SuplaBridge.buildAuthData(config);

        assertThat(authData.locationAuthData()).isNull();
        assertThat(authData.emailAuthData()).isNotNull();
        assertThat(authData.emailAuthData().email()).isEqualTo("user@example.com");
        assertThat(authData.emailAuthData().authKey()).isEqualTo("key");
    }

    @Test
    void shouldBuildTimeoutConfiguration() {
        var config = new ServerBridgeHandlerConfiguration();
        config.setTimeout(BigDecimal.valueOf(5));
        config.setTimeoutMin(BigDecimal.valueOf(3));
        config.setTimeoutMax(BigDecimal.valueOf(7));

        TimeoutConfiguration timeoutConfiguration = SuplaBridge.buildTimeoutConfiguration(config);

        assertThat(timeoutConfiguration.timeout()).isEqualTo(5);
        assertThat(timeoutConfiguration.min()).isEqualTo(3);
        assertThat(timeoutConfiguration.max()).isEqualTo(7);
    }
}
