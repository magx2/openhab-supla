package pl.grzeslowski.openhab.supla.internal.server.handler.trait;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import pl.grzeslowski.openhab.supla.internal.server.oh_config.AuthData;
import pl.grzeslowski.openhab.supla.internal.server.oh_config.ServerBridgeHandlerConfiguration;
import pl.grzeslowski.openhab.supla.internal.server.oh_config.TimeoutConfiguration;

class ServerBridgeTest {

    @Test
    void shouldBuildAuthDataWithLocationAndEmail() {
        var config = new ServerBridgeHandlerConfiguration();
        config.setServerAccessId(BigDecimal.valueOf(123));
        config.setServerAccessIdPassword("secret");
        config.setEmail("user@example.com");
        config.setAuthKey("key");

        AuthData authData = ServerBridge.buildAuthData(config);

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

        AuthData authData = ServerBridge.buildAuthData(config);

        assertThat(authData.locationAuthData()).isNull();
        assertThat(authData.emailAuthData()).isNotNull();
        assertThat(authData.emailAuthData().email()).isEqualTo("user@example.com");
        assertThat(authData.emailAuthData().authKey()).isEqualTo("key");
    }

    @Test
    void shouldBuildTimeoutConfiguration() {
        var config = new ServerBridgeHandlerConfiguration();
        config.setTimeout("5");
        config.setTimeoutMin("3");
        config.setTimeoutMax("7");

        TimeoutConfiguration timeoutConfiguration = ServerBridge.buildTimeoutConfiguration(config);

        assertThat(timeoutConfiguration.timeout()).isEqualTo(Duration.ofSeconds(5));
        assertThat(timeoutConfiguration.min()).isEqualTo(Duration.ofSeconds(3));
        assertThat(timeoutConfiguration.max()).isEqualTo(Duration.ofSeconds(7));
    }

    @Test
    void shouldBuildTimeoutConfigurationPt() {
        var config = new ServerBridgeHandlerConfiguration();
        config.setTimeout("PT5S");
        config.setTimeoutMin("PT3S");
        config.setTimeoutMax("PT7S");

        TimeoutConfiguration timeoutConfiguration = ServerBridge.buildTimeoutConfiguration(config);

        assertThat(timeoutConfiguration.timeout()).isEqualTo(Duration.ofSeconds(5));
        assertThat(timeoutConfiguration.min()).isEqualTo(Duration.ofSeconds(3));
        assertThat(timeoutConfiguration.max()).isEqualTo(Duration.ofSeconds(7));
    }
}
