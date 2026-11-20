package pl.grzeslowski.openhab.supla.internal.server.device_config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static pl.grzeslowski.jsupla.protocol.api.DeviceConfigField.SUPLA_DEVICE_CONFIG_FIELD_AUTOMATIC_TIME_SYNC;
import static pl.grzeslowski.jsupla.protocol.api.DeviceConfigField.SUPLA_DEVICE_CONFIG_FIELD_INPUT_ACTIVATION;
import static pl.grzeslowski.jsupla.protocol.api.DeviceConfigField.SUPLA_DEVICE_CONFIG_FIELD_STATUS_LED;
import static pl.grzeslowski.jsupla.protocol.api.DeviceConfigField.SUPLA_DEVICE_CONFIG_FIELD_THERMAL_PROTECTION;
import static pl.grzeslowski.jsupla.protocol.api.StatusLed.SUPLA_DEVCFG_STATUS_LED_OFF_WHEN_CONNECTED;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Map;
import org.junit.jupiter.api.Test;
import pl.grzeslowski.jsupla.protocol.api.StatusLed;
import pl.grzeslowski.jsupla.protocol.api.encoders.InputActivationEncoder;
import pl.grzeslowski.jsupla.protocol.api.encoders.ThermalProtectionEncoder;
import pl.grzeslowski.jsupla.protocol.api.structs.InputActivation;
import pl.grzeslowski.jsupla.protocol.api.structs.ThermalProtection;

class DeviceConfigUtilTest {
    @Test
    void shouldBuildDeviceConfigMapFromFieldsAndBytes() throws IOException {
        var statusLed = new DeviceConfig.StatusLedConfig(SUPLA_DEVCFG_STATUS_LED_OFF_WHEN_CONNECTED);
        var automaticTimeSync = new DeviceConfig.AutomaticTimeSyncConfig(true);
        long fields = SUPLA_DEVICE_CONFIG_FIELD_STATUS_LED.getValue()
                | SUPLA_DEVICE_CONFIG_FIELD_AUTOMATIC_TIME_SYNC.getValue();

        var output = new ByteArrayOutputStream();
        output.write(statusLed.encode());
        output.write(automaticTimeSync.encode());

        Map<String, String> configMap = DeviceConfigUtil.buildDeviceConfig(fields, output.toByteArray());

        assertThat(configMap)
                .containsEntry("SUPLA_DEVICE_CONFIG_FIELD_STATUS_LED", "SUPLA_DEVCFG_STATUS_LED_OFF_WHEN_CONNECTED")
                .containsEntry("SUPLA_DEVICE_CONFIG_FIELD_AUTOMATIC_TIME_SYNC", "enabled");
    }

    @Test
    void shouldDecodeNewDeviceConfigFieldsInSequence() throws IOException {
        var thermalProtection =
                new ThermalProtection((short) 70, (short) 40, (short) 90, (short) 1, (short) 0, new short[8]);
        var inputActivation = new InputActivation((short) 3, (short) 2, new short[6]);
        var output = new ByteArrayOutputStream();
        output.write(ThermalProtectionEncoder.INSTANCE.encode(thermalProtection));
        output.write(InputActivationEncoder.INSTANCE.encode(inputActivation));

        var fields = SUPLA_DEVICE_CONFIG_FIELD_THERMAL_PROTECTION.getValue()
                | SUPLA_DEVICE_CONFIG_FIELD_INPUT_ACTIVATION.getValue();
        var configMap = DeviceConfigUtil.buildDeviceConfig(fields, output.toByteArray());

        assertThat(configMap)
                .containsEntry("SUPLA_DEVICE_CONFIG_FIELD_THERMAL_PROTECTION_THRESHOLD", "70")
                .containsEntry("SUPLA_DEVICE_CONFIG_FIELD_THERMAL_PROTECTION_MIN_THRESHOLD", "40")
                .containsEntry("SUPLA_DEVICE_CONFIG_FIELD_THERMAL_PROTECTION_MAX_THRESHOLD", "90")
                .containsEntry("SUPLA_DEVICE_CONFIG_FIELD_THERMAL_PROTECTION_ENABLED", "true")
                .containsEntry("SUPLA_DEVICE_CONFIG_FIELD_THERMAL_PROTECTION_DISABLE_ALLOWED", "false")
                .containsEntry("SUPLA_DEVICE_CONFIG_FIELD_INPUT_ACTIVATION_AVAILABLE_MODES", "3")
                .containsEntry("SUPLA_DEVICE_CONFIG_FIELD_INPUT_ACTIVATION_MODE", "2");
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
        assertThatThrownBy(() -> DeviceConfigUtil.parseEnum("INVALID", StatusLed.class))
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
