package pl.grzeslowski.openhab.supla.internal.server.oh_config;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import org.junit.jupiter.api.Test;

class ServerDeviceHandlerConfigurationTest {
    @Test
    void shouldUse30SecondsAsDefaultActionTimeout() {
        var configuration = new ServerDeviceHandlerConfiguration();

        assertThat(configuration.getActionTimeout()).isEqualTo(Duration.ofSeconds(30));
        assertThat(configuration.getSetDeviceConfigActionTimeout()).isEqualTo(Duration.ofSeconds(30));
        assertThat(configuration.getResetElectricMeterCountersActionTimeout()).isEqualTo(Duration.ofSeconds(30));
        assertThat(configuration.getEnterConfigModeActionTimeout()).isEqualTo(Duration.ofSeconds(30));
        assertThat(configuration.getCheckFirmwareUpdateActionTimeout()).isEqualTo(Duration.ofSeconds(30));
        assertThat(configuration.getStartFirmwareUpdateActionTimeout()).isEqualTo(Duration.ofSeconds(30));
        assertThat(configuration.getStartSecurityUpdateActionTimeout()).isEqualTo(Duration.ofSeconds(30));
    }

    @Test
    void shouldUseMainActionTimeoutWhenSpecificOneIsNotSet() {
        var configuration = new ServerDeviceHandlerConfiguration();
        configuration.setActionTimeout("PT42S");

        assertThat(configuration.getActionTimeout()).isEqualTo(Duration.ofSeconds(42));
        assertThat(configuration.getSetDeviceConfigActionTimeout()).isEqualTo(Duration.ofSeconds(42));
        assertThat(configuration.getResetElectricMeterCountersActionTimeout()).isEqualTo(Duration.ofSeconds(42));
        assertThat(configuration.getEnterConfigModeActionTimeout()).isEqualTo(Duration.ofSeconds(42));
        assertThat(configuration.getCheckFirmwareUpdateActionTimeout()).isEqualTo(Duration.ofSeconds(42));
        assertThat(configuration.getStartFirmwareUpdateActionTimeout()).isEqualTo(Duration.ofSeconds(42));
        assertThat(configuration.getStartSecurityUpdateActionTimeout()).isEqualTo(Duration.ofSeconds(42));
    }

    @Test
    void shouldUseSpecificActionTimeoutWhenSet() {
        var configuration = new ServerDeviceHandlerConfiguration();
        configuration.setActionTimeout("PT42S");
        configuration.setSetDeviceConfigActionTimeout("PT4S");

        assertThat(configuration.getSetDeviceConfigActionTimeout()).isEqualTo(Duration.ofSeconds(4));
    }
}
