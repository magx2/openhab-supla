package pl.grzeslowski.openhab.supla.internal.server.traits;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pl.grzeslowski.jsupla.protocol.api.structs.ds.SuplaDeviceChannelValueA;
import pl.grzeslowski.jsupla.protocol.api.structs.ds.SuplaDeviceChannelValueB;
import pl.grzeslowski.jsupla.protocol.api.structs.ds.SuplaDeviceChannelValueC;

@ExtendWith(MockitoExtension.class)
class DeviceChannelValueTest {
    @Mock
    private SuplaDeviceChannelValueA valueA;

    @Mock
    private SuplaDeviceChannelValueB valueB;

    @Mock
    private SuplaDeviceChannelValueC valueC;

    @Test
    void shouldBuildFromProtoA() {
        byte[] bytes = new byte[] {0x01};
        when(valueA.channelNumber()).thenReturn((short) 2);
        when(valueA.value()).thenReturn(bytes);

        DeviceChannelValue deviceChannelValue = DeviceChannelValue.fromProto(valueA);

        assertThat(deviceChannelValue.channelNumber()).isEqualTo(2);
        assertThat(deviceChannelValue.value()).containsExactly(bytes);
        assertThat(deviceChannelValue.offline()).isFalse();
        assertThat(deviceChannelValue.validityTimeSec()).isNull();
    }

    @Test
    void shouldBuildFromProtoB() {
        byte[] bytes = new byte[] {0x02};
        when(valueB.channelNumber()).thenReturn((short) 3);
        when(valueB.value()).thenReturn(bytes);
        when(valueB.offline()).thenReturn((short) 1);

        DeviceChannelValue deviceChannelValue = DeviceChannelValue.fromProto(valueB);

        assertThat(deviceChannelValue.channelNumber()).isEqualTo(3);
        assertThat(deviceChannelValue.value()).containsExactly(bytes);
        assertThat(deviceChannelValue.offline()).isTrue();
        assertThat(deviceChannelValue.validityTimeSec()).isNull();
    }

    @Test
    void shouldBuildFromProtoC() {
        byte[] bytes = new byte[] {0x03};
        when(valueC.channelNumber()).thenReturn((short) 4);
        when(valueC.value()).thenReturn(bytes);
        when(valueC.offline()).thenReturn((short) 0);
        when(valueC.validityTimeSec()).thenReturn(99L);

        DeviceChannelValue deviceChannelValue = DeviceChannelValue.fromProto(valueC);

        assertThat(deviceChannelValue.channelNumber()).isEqualTo(4);
        assertThat(deviceChannelValue.value()).containsExactly(bytes);
        assertThat(deviceChannelValue.offline()).isFalse();
        assertThat(deviceChannelValue.validityTimeSec()).isEqualTo(99L);
    }
}
