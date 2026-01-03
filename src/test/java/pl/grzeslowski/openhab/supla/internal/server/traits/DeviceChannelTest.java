package pl.grzeslowski.openhab.supla.internal.server.traits;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static pl.grzeslowski.jsupla.protocol.api.ChannelFunction.SUPLA_CHANNELFNC_NONE;

import java.util.Set;
import org.junit.jupiter.api.Test;
import pl.grzeslowski.jsupla.protocol.api.ChannelType;
import pl.grzeslowski.jsupla.protocol.api.channeltype.value.HvacValue;
import pl.grzeslowski.jsupla.protocol.api.structs.ds.SuplaDeviceChannelA;
import pl.grzeslowski.jsupla.protocol.api.structs.ds.SuplaDeviceChannelB;
import pl.grzeslowski.jsupla.protocol.api.structs.ds.SuplaDeviceChannelE;

@SuppressWarnings("deprecation")
class DeviceChannelTest {
    @Test
    void shouldRejectMissingValues() {
        assertThatThrownBy(() -> new DeviceChannel(
                        1,
                        ChannelType.CALCFG_TYPE_THERMOSTAT_DETAILS_V1,
                        Set.of(),
                        SUPLA_CHANNELFNC_NONE,
                        Set.of(),
                        null,
                        null,
                        null,
                        null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("value or hvacValue or action must not be null!");
    }

    @Test
    void shouldBuildFromProtoA() {
        byte[] value = new byte[] {0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08};
        SuplaDeviceChannelA channelA =
                new SuplaDeviceChannelA((short) 5, ChannelType.SUPLA_CHANNELTYPE_RELAYHFD4.getValue(), value);

        DeviceChannel deviceChannel = DeviceChannel.fromProto(channelA);

        assertThat(deviceChannel.number()).isEqualTo(5);
        assertThat(deviceChannel.type()).isEqualTo(ChannelType.SUPLA_CHANNELTYPE_RELAYHFD4);
        assertThat(deviceChannel.channelFunction()).isEqualTo(null);
        assertThat(deviceChannel.value()).containsExactly(value);
        assertThat(deviceChannel.hvacValue()).isNull();
        assertThat(deviceChannel.subDeviceId()).isNull();
    }

    @Test
    void shouldBuildFromProtoBWithChannelFunction() {
        byte[] value = new byte[] {0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x0A};
        SuplaDeviceChannelB channelB = new SuplaDeviceChannelB(
                (short) 10,
                ChannelType.SUPLA_CHANNELTYPE_ACTIONTRIGGER.getValue(),
                SUPLA_CHANNELFNC_NONE.getValue(),
                0,
                value);

        DeviceChannel deviceChannel = DeviceChannel.fromProto(channelB);

        assertThat(deviceChannel.number()).isEqualTo(10);
        assertThat(deviceChannel.type()).isEqualTo(ChannelType.SUPLA_CHANNELTYPE_ACTIONTRIGGER);
        assertThat(deviceChannel.channelFunction()).isEqualTo(SUPLA_CHANNELFNC_NONE);
        assertThat(deviceChannel.value()).containsExactly(value);
        assertThat(deviceChannel.hvacValue()).isNull();
        assertThat(deviceChannel.subDeviceId()).isNull();
    }

    @Test
    void shouldBuildFromProtoEWithSubDeviceId() {
        byte[] value = new byte[] {0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x0B, 0x0C};
        SuplaDeviceChannelE channelE = new SuplaDeviceChannelE(
                (short) 11,
                ChannelType.SUPLA_CHANNELTYPE_AM2302.getValue(),
                SUPLA_CHANNELFNC_NONE.getValue(),
                null,
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
        assertThat(deviceChannel.type()).isEqualTo(ChannelType.SUPLA_CHANNELTYPE_AM2302);
        assertThat(deviceChannel.channelFunction()).isEqualTo(SUPLA_CHANNELFNC_NONE);
        assertThat(deviceChannel.value()).containsExactly(value);
        assertThat(deviceChannel.hvacValue()).isNull();
        assertThat(deviceChannel.subDeviceId()).isEqualTo(3);
    }

    @Test
    void shouldAcceptHvacWithoutValue() {
        HvacValue hvacValue = new HvacValue(
                true,
                HvacValue.Mode.NOT_SET,
                null,
                null,
                new HvacValue.Flags(
                        false, false, false, false, false, false, false, false, false, false, false, false, false));
        DeviceChannel deviceChannel = new DeviceChannel(
                1,
                ChannelType.EV_TYPE_ELECTRICITY_METER_MEASUREMENT_V1,
                Set.of(),
                SUPLA_CHANNELFNC_NONE,
                Set.of(),
                null,
                null,
                hvacValue,
                null);

        assertThat(deviceChannel.value()).isNull();
        assertThat(deviceChannel.hvacValue()).isEqualTo(hvacValue);
    }
}
