package pl.grzeslowski.openhab.supla.internal.server.traits;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import pl.grzeslowski.jsupla.protocol.api.structs.ds.SuplaDeviceChannelValueA;
import pl.grzeslowski.jsupla.protocol.api.structs.ds.SuplaDeviceChannelValueB;
import pl.grzeslowski.jsupla.protocol.api.structs.ds.SuplaDeviceChannelValueC;

class DeviceChannelValueTest {
    @Test
    void shouldBuildFromProtoA() {
        byte[] bytes = new byte[] {0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08};
        SuplaDeviceChannelValueA valueA = new SuplaDeviceChannelValueA((short) 2, bytes);

        DeviceChannelValue deviceChannelValue = DeviceChannelValue.fromProto(valueA);

        assertThat(deviceChannelValue.channelNumber()).isEqualTo(2);
        assertThat(deviceChannelValue.value()).containsExactly(bytes);
        assertThat(deviceChannelValue.offline()).isFalse();
        assertThat(deviceChannelValue.validityTimeSec()).isNull();
    }

    @Test
    void shouldBuildFromProtoB() {
        byte[] bytes = new byte[] {0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x02};
        SuplaDeviceChannelValueB valueB = new SuplaDeviceChannelValueB((short) 3, (short) 1, bytes);

        DeviceChannelValue deviceChannelValue = DeviceChannelValue.fromProto(valueB);

        assertThat(deviceChannelValue.channelNumber()).isEqualTo(3);
        assertThat(deviceChannelValue.value()).containsExactly(bytes);
        assertThat(deviceChannelValue.offline()).isTrue();
        assertThat(deviceChannelValue.validityTimeSec()).isNull();
    }

    @Test
    void shouldBuildFromProtoC() {
        byte[] bytes = new byte[] {0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x03};
        SuplaDeviceChannelValueC valueC = new SuplaDeviceChannelValueC((short) 4, (short) 0, 99L, bytes);

        DeviceChannelValue deviceChannelValue = DeviceChannelValue.fromProto(valueC);

        assertThat(deviceChannelValue.channelNumber()).isEqualTo(4);
        assertThat(deviceChannelValue.value()).containsExactly(bytes);
        assertThat(deviceChannelValue.offline()).isFalse();
        assertThat(deviceChannelValue.validityTimeSec()).isEqualTo(99L);
    }
}
