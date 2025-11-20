package pl.grzeslowski.openhab.supla.internal.server.traits;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static pl.grzeslowski.jsupla.protocol.api.ChannelFunction.SUPLA_CHANNELFNC_NONE;

import org.junit.jupiter.api.Test;
import pl.grzeslowski.jsupla.protocol.api.channeltype.value.HvacValue;
import pl.grzeslowski.jsupla.protocol.api.structs.ds.SuplaDeviceChannelA;
import pl.grzeslowski.jsupla.protocol.api.structs.ds.SuplaDeviceChannelB;
import pl.grzeslowski.jsupla.protocol.api.structs.ds.SuplaDeviceChannelE;

class DeviceChannelTest {
    @Test
    void shouldRejectMissingValues() {
        assertThatThrownBy(() -> new DeviceChannel(1, 2, SUPLA_CHANNELFNC_NONE, null, null, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("value and hvacValue must not be null!");
    }

    @Test
    void shouldBuildFromProtoA() {
        byte[] value = new byte[] {0x01, 0x02};
        SuplaDeviceChannelA channelA = new SuplaDeviceChannelA((short) 5, 7, value);

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
        SuplaDeviceChannelB channelB =
                new SuplaDeviceChannelB((short) 10, 20, SUPLA_CHANNELFNC_NONE.getValue(), 0, value);

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
        SuplaDeviceChannelE channelE = new SuplaDeviceChannelE(
                (short) 11,
                21,
                SUPLA_CHANNELFNC_NONE.getValue(),
                null,
                0,
                0,
                (short) 0,
                0,
                value,
                null,
                null,
                (short) 0,
                (short) 3);

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
        HvacValue hvacValue = mock(HvacValue.class);
        DeviceChannel deviceChannel = new DeviceChannel(1, 2, SUPLA_CHANNELFNC_NONE, null, hvacValue, null);

        assertThat(deviceChannel.value()).isNull();
        assertThat(deviceChannel.hvacValue()).isEqualTo(hvacValue);
    }
}
