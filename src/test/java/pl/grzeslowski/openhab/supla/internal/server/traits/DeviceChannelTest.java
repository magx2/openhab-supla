package pl.grzeslowski.openhab.supla.internal.server.traits;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;
import static pl.grzeslowski.jsupla.protocol.api.ChannelFunction.SUPLA_CHANNELFNC_NONE;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pl.grzeslowski.jsupla.protocol.api.channeltype.value.HvacValue;
import pl.grzeslowski.jsupla.protocol.api.structs.ds.SuplaDeviceChannelA;
import pl.grzeslowski.jsupla.protocol.api.structs.ds.SuplaDeviceChannelB;
import pl.grzeslowski.jsupla.protocol.api.structs.ds.SuplaDeviceChannelE;

@ExtendWith(MockitoExtension.class)
class DeviceChannelTest {
    @Mock
    private SuplaDeviceChannelA channelA;

    @Mock
    private SuplaDeviceChannelB channelB;

    @Mock
    private SuplaDeviceChannelE channelE;

    @Mock
    private HvacValue hvacValue;

    @Test
    void shouldRejectMissingValues() {
        assertThatThrownBy(() -> new DeviceChannel(1, 2, SUPLA_CHANNELFNC_NONE, null, null, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("value and hvacValue must not be null!");
    }

    @Test
    void shouldBuildFromProtoA() {
        byte[] value = new byte[] {0x01, 0x02};
        when(channelA.number()).thenReturn((short) 5);
        when(channelA.type()).thenReturn(7);
        when(channelA.value()).thenReturn(value);

        DeviceChannel deviceChannel = DeviceChannel.fromProto(channelA);

        assertThat(deviceChannel.number()).isEqualTo(5);
        assertThat(deviceChannel.type()).isEqualTo(7);
        assertThat(deviceChannel.channelFunction()).isEqualTo(SUPLA_CHANNELFNC_NONE);
        assertThat(deviceChannel.value()).containsExactly(value);
        assertThat(deviceChannel.hvacValue()).isNull();
        assertThat(deviceChannel.subDeviceId()).isNull();
    }

    @Test
    void shouldBuildFromProtoBWithChannelFunction() {
        byte[] value = new byte[] {0x0A};
        when(channelB.number()).thenReturn((short) 10);
        when(channelB.type()).thenReturn(20);
        when(channelB.funcList()).thenReturn(SUPLA_CHANNELFNC_NONE.getValue());
        when(channelB.value()).thenReturn(value);

        DeviceChannel deviceChannel = DeviceChannel.fromProto(channelB);

        assertThat(deviceChannel.number()).isEqualTo(10);
        assertThat(deviceChannel.type()).isEqualTo(20);
        assertThat(deviceChannel.channelFunction()).isEqualTo(SUPLA_CHANNELFNC_NONE);
        assertThat(deviceChannel.value()).containsExactly(value);
        assertThat(deviceChannel.hvacValue()).isNull();
        assertThat(deviceChannel.subDeviceId()).isNull();
    }

    @Test
    void shouldBuildFromProtoEWithSubDeviceId() {
        byte[] value = new byte[] {0x0B, 0x0C};
        when(channelE.number()).thenReturn((short) 11);
        when(channelE.type()).thenReturn(21);
        when(channelE.funcList()).thenReturn(SUPLA_CHANNELFNC_NONE.getValue());
        when(channelE.value()).thenReturn(value);
        when(channelE.hvacValue()).thenReturn(null);
        when(channelE.subDeviceId()).thenReturn((short) 3);

        DeviceChannel deviceChannel = DeviceChannel.fromProto(channelE);

        assertThat(deviceChannel.number()).isEqualTo(11);
        assertThat(deviceChannel.type()).isEqualTo(21);
        assertThat(deviceChannel.channelFunction()).isEqualTo(SUPLA_CHANNELFNC_NONE);
        assertThat(deviceChannel.value()).containsExactly(value);
        assertThat(deviceChannel.hvacValue()).isNull();
        assertThat(deviceChannel.subDeviceId()).isEqualTo(3);
    }

    @Test
    void shouldAcceptHvacWithoutValue() {
        DeviceChannel deviceChannel = new DeviceChannel(1, 2, SUPLA_CHANNELFNC_NONE, null, hvacValue, null);

        assertThat(deviceChannel.value()).isNull();
        assertThat(deviceChannel.hvacValue()).isEqualTo(hvacValue);
    }
}
