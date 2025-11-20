package pl.grzeslowski.openhab.supla.internal.server.device_config;

import static org.assertj.core.api.Assertions.assertThat;
import static pl.grzeslowski.jsupla.protocol.api.consts.ProtoConsts.SUPLA_CONFIG_RESULT_DEVICE_NOT_FOUND;
import static pl.grzeslowski.jsupla.protocol.api.consts.ProtoConsts.SUPLA_CONFIG_RESULT_TRUE;

import org.junit.jupiter.api.Test;

class DeviceConfigResultTest {
    @Test
    void shouldFindKnownResult() {
        var result = DeviceConfigResult.findConfigResult(SUPLA_CONFIG_RESULT_TRUE);

        assertThat(result).isEqualTo(DeviceConfigResult.TRUE);
        assertThat(result.isSuccess()).isTrue();
    }

    @Test
    void shouldFindSpecificFailure() {
        var result = DeviceConfigResult.findConfigResult(SUPLA_CONFIG_RESULT_DEVICE_NOT_FOUND);

        assertThat(result).isEqualTo(DeviceConfigResult.DEVICE_NOT_FOUND);
        assertThat(result.isSuccess()).isFalse();
    }

    @Test
    void shouldReturnUnknownForUnexpectedValue() {
        assertThat(DeviceConfigResult.findConfigResult(999)).isEqualTo(DeviceConfigResult.UNKNOWN);
    }
}
