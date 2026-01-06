package pl.grzeslowski.openhab.supla.internal.server.device_config;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.Test;
import pl.grzeslowski.jsupla.protocol.api.StatusLed;

class DeviceConfigTest {
    @Test
    void shouldParseDisableUserInterfaceWithoutRange() {
        var config = DeviceConfig.DisableUserInterfaceConfig.parse(List.of("ENABLED"));

        assertThat(config.userInterface()).isEqualTo(DeviceConfig.DisableUserInterfaceConfig.UserInterface.ENABLED);
        assertThat(config.minTemp()).isNull();
        assertThat(config.maxTemp()).isNull();
    }

    @Test
    void shouldParseDisableUserInterfaceWithRange() {
        var config = DeviceConfig.DisableUserInterfaceConfig.parse(List.of("PARTIAL", "18", "25"));

        assertThat(config.userInterface()).isEqualTo(DeviceConfig.DisableUserInterfaceConfig.UserInterface.PARTIAL);
        assertThat(config.minTemp()).isEqualTo(18);
        assertThat(config.maxTemp()).isEqualTo(25);
    }

    @Test
    void shouldParseHomeScreenContentConfig() {
        var config = DeviceConfig.HomeScreenContentConfig.parse(List.of("TEMPERATURE", "TIME_DATE"));

        assertThat(config.contents())
                .containsExactly(
                        DeviceConfig.HomeScreenContentConfig.HomeScreenContent.TEMPERATURE,
                        DeviceConfig.HomeScreenContentConfig.HomeScreenContent.TIME_DATE);
    }

    @Test
    void shouldParseHomeScreenOffDelayConfig() {
        var config = DeviceConfig.HomeScreenOffDelayConfig.parse(List.of("true", "PT30S"));

        assertThat(config.enabled()).isTrue();
        assertThat(config.duration()).isEqualTo(Duration.ofSeconds(30));
    }

    @Test
    void shouldParseStatusLedConfig() {
        var config = DeviceConfig.StatusLedConfig.parse(List.of("SUPLA_DEVCFG_STATUS_LED_OFF_WHEN_CONNECTED"));

        assertThat(config.statusLed()).isEqualTo(StatusLed.SUPLA_DEVCFG_STATUS_LED_OFF_WHEN_CONNECTED);
    }
}
