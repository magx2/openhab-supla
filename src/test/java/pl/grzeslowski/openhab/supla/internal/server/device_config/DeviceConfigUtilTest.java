package pl.grzeslowski.openhab.supla.internal.server.device_config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static pl.grzeslowski.openhab.supla.internal.server.device_config.DeviceConfigField.AUTOMATIC_TIME_SYNC;
import static pl.grzeslowski.openhab.supla.internal.server.device_config.DeviceConfigField.STATUS_LED;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Map;
import org.junit.jupiter.api.Test;

class DeviceConfigUtilTest {
    @Test
    void shouldBuildDeviceConfigMapFromFieldsAndBytes() throws IOException {
        var statusLed = new DeviceConfig.StatusLedConfig(DeviceConfig.StatusLedConfig.StatusLed.OFF_WHEN_CONNECTED);
        var automaticTimeSync = new DeviceConfig.AutomaticTimeSyncConfig(true);
        long fields = STATUS_LED.getMask() | AUTOMATIC_TIME_SYNC.getMask();

        var output = new ByteArrayOutputStream();
        output.write(statusLed.encode());
        output.write(automaticTimeSync.encode());

        Map<String, String> configMap = DeviceConfigUtil.buildDeviceConfig(fields, output.toByteArray());

        assertThat(configMap)
                .containsEntry("DEVICE_CONFIG_STATUS_LED", "OFF_WHEN_CONNECTED")
                .containsEntry("DEVICE_CONFIG_AUTOMATIC_TIME_SYNC", "enabled");
    }

    @Test
    void shouldParseDeviceConfigByName() {
        var config = DeviceConfigUtil.parseDeviceConfig("ButtonVolumeConfig:55");

        assertThat(config).isInstanceOf(DeviceConfig.ButtonVolumeConfig.class);
        assertThat(((DeviceConfig.ButtonVolumeConfig) config).volume()).isEqualTo(55);
    }

    @Test
    void shouldRejectMissingSeparatorWhenParsingDeviceConfig() {
        assertThatThrownBy(() -> DeviceConfigUtil.parseDeviceConfig("InvalidString"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("at least one `:`");
    }

    @Test
    void shouldRejectUnknownDeviceConfigName() {
        assertThatThrownBy(() -> DeviceConfigUtil.parseDeviceConfig("UnknownConfig:true"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("UnknownConfig");
    }

    @Test
    void shouldRejectInvalidEnumValue() {
        assertThatThrownBy(() -> DeviceConfigUtil.parseEnum("INVALID", DeviceConfig.StatusLedConfig.StatusLed.class))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Cannot parse enum");
    }

    @Test
    void shouldRejectInvalidBooleanValue() {
        assertThatThrownBy(() -> DeviceConfigUtil.parseBoolean("not-a-bool"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Cannot parse boolean");
    }

    @Test
    void shouldRejectInvalidDuration() {
        assertThatThrownBy(() -> DeviceConfigUtil.parseDuration("INVALID"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Cannot parse duration");
    }

    @Test
    void shouldRejectInvalidInteger() {
        assertThatThrownBy(() -> DeviceConfigUtil.parseInt("not-int"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Cannot parse int");
    }
}
