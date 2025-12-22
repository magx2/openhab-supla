package pl.grzeslowski.openhab.supla.internal.device;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static pl.grzeslowski.jsupla.protocol.api.consts.ProtoConsts.SUPLA_LOCATION_PWD_MAXSIZE;
import static pl.grzeslowski.openhab.supla.internal.server.ByteArrayToHex.hexToBytes;

import java.io.IOException;
import java.util.Arrays;
import lombok.Getter;
import lombok.Setter;
import org.checkerframework.checker.nullness.qual.NonNull;
import pl.grzeslowski.jsupla.protocol.api.structs.ds.SuplaDeviceChannelB;
import pl.grzeslowski.jsupla.protocol.api.structs.ds.SuplaDeviceChannelValueA;
import pl.grzeslowski.jsupla.protocol.api.structs.ds.SuplaRegisterDeviceB;
import pl.grzeslowski.jsupla.protocol.api.structs.sd.SuplaRegisterDeviceResultA;
import pl.grzeslowski.openhab.supla.internal.random.RandomExtension;

public class ZamelRow01 extends Device {
    private final int locationId;
    private final byte[] locationPwd;

    @Getter
    @Setter
    private boolean state = RandomExtension.INSTANCE.randomBool();

    public ZamelRow01(String guid, int locationId, String locationPwd) {
        super((short) 5, guid);
        this.locationId = locationId;
        assertThat(locationPwd.getBytes(UTF_8)).hasSizeLessThanOrEqualTo(SUPLA_LOCATION_PWD_MAXSIZE);
        this.locationPwd = Arrays.copyOf(locationPwd.getBytes(UTF_8), SUPLA_LOCATION_PWD_MAXSIZE);
    }

    public void register() throws IOException {
        log.info("Registering Zamel ROW-01 device");
        var channel = new SuplaDeviceChannelB((short) 0, 2900, 96, 140, findValue());
        var proto = new SuplaRegisterDeviceB(
                locationId,
                locationPwd,
                hexToBytes(guid),
                new byte[] {
                    90, 65, 77, 69, 76, 32, 82, 79, 87, 45, 48, 49, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                    0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                    0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                    0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                    0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                    0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                    0, 0, 0, 0, 0, 0, 0
                },
                new byte[] {50, 46, 48, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0},
                (short) 1,
                new SuplaDeviceChannelB[] {channel});
        send(proto);
    }

    public SuplaRegisterDeviceResultA readRegisterDeviceResultA() throws IOException {
        var read = super.read();
        assertThat(read).isInstanceOf(SuplaRegisterDeviceResultA.class);
        return (SuplaRegisterDeviceResultA) read;
    }

    public synchronized void toggleSwitch() throws IOException {
        state = !state;
        var value = findValue();
        var proto = new SuplaDeviceChannelValueA((short) 0, value);
        send(proto);
    }

    private byte @NonNull [] findValue() {
        var byteState = (byte) (state ? 1 : 0);
        return new byte[] {byteState, 0, 0, 0, 0, 0, 0, 0};
    }

    @Override
    protected void updateChannel(short channelNumber, byte[] value) {
        assertThat(channelNumber).isZero();
        state = value[0] == 1;
    }
}
