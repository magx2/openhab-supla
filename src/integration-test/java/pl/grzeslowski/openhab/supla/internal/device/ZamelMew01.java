package pl.grzeslowski.openhab.supla.internal.device;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static pl.grzeslowski.jsupla.protocol.api.ChannelType.SUPLA_CHANNELTYPE_ELECTRICITY_METER;
import static pl.grzeslowski.jsupla.protocol.api.consts.ProtoConsts.SUPLA_CHANNEL_OFFLINE_FLAG_ONLINE;
import static pl.grzeslowski.jsupla.protocol.api.consts.ProtoConsts.SUPLA_EMAIL_MAXSIZE;
import static pl.grzeslowski.openhab.supla.internal.server.ByteArrayToHex.hexToBytes;

import java.io.IOException;
import java.util.Arrays;
import pl.grzeslowski.jsupla.protocol.api.structs.ds.SuplaDeviceChannelC;
import pl.grzeslowski.jsupla.protocol.api.structs.ds.SuplaDeviceChannelValueC;
import pl.grzeslowski.jsupla.protocol.api.structs.ds.SuplaRegisterDeviceE;

public class ZamelMew01 extends Device {
    private static final byte[] CHANNEL_VALUE = new byte[] {7, -60, -38, 2, 0, 0, 0, 0};

    private final byte[] email;
    private final byte[] authKey;

    public ZamelMew01(String guid, String email, String authKey) {
        super((short) 10, guid);
        assertThat(email.getBytes(UTF_8)).hasSizeLessThanOrEqualTo(SUPLA_EMAIL_MAXSIZE);
        this.email = Arrays.copyOf(email.getBytes(UTF_8), SUPLA_EMAIL_MAXSIZE);
        this.authKey = hexToBytes(authKey);
    }

    @Override
    public void register() throws IOException {
        log.info("Registering Zamel MEW-01 device");
        var channels = new SuplaDeviceChannelC[] {
            new SuplaDeviceChannelC(
                    (short) 0, SUPLA_CHANNELTYPE_ELECTRICITY_METER.getValue(), 0, null, 0, 0, CHANNEL_VALUE, null, null)
        };
        var proto = new SuplaRegisterDeviceE(
                email,
                authKey,
                hexToBytes(guid),
                Arrays.copyOf("ZAMEL MEW-01".getBytes(UTF_8), 201),
                Arrays.copyOf("2.7.12".getBytes(UTF_8), 21),
                Arrays.copyOf("192.168.1.45".getBytes(UTF_8), 65),
                0,
                (short) 4,
                (short) 1000,
                (short) channels.length,
                channels);
        send(proto);
    }

    public void meterValueUpdated() throws IOException {
        send(new SuplaDeviceChannelValueC((short) 0, (short) SUPLA_CHANNEL_OFFLINE_FLAG_ONLINE, 0, CHANNEL_VALUE));
    }

    @Override
    protected void updateChannel(short channelNumber, byte[] value) {
        throw new UnsupportedOperationException("There are no channels that OH can update in MEW-01!");
    }
}
