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
    private static final byte[] CALL_ID_100_VALUE = new byte[] {0, 7, 47, -37, 2, 0, 0, 0, 0};
    private static final byte[] CALL_ID_105_VALUE = new byte[] {
        0, 10, -83, 1, 0, 0, -25, 27, 14, 11, 0, 0, 0, 0, 71, 13, 4, 0, 0, 0, 0, 0, -98, 8, 22, 0, 0, 0, 0, 0, -58, 17,
        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, -79, 39, -77, 5, 0, 0, 0, 0, -121, -99, 0, 0,
        0, 0, 0, 0, -47, -67, 4, 0, 0, 0, 0, 0, -54, -24, 29, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, -50, -118, 8, 0, 0,
        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, -1, -1, 0, 0, 1, 0, 0, 0, 5, 0, 0, 0, -124, 19, -108, 94, -94, 94, 70,
        94, -46, 0, 0, 0, 16, 0, 76, -123, 55, 0, 120, 21, 0, 0, -90, 9, 0, 0, -26, -126, 3, 0, 34, 3, 0, 0, -88, -57,
        -4, -1, -8, 78, 76, 0, 102, 38, 1, 0, 102, 102, 2, 0, -11, 2, -24, 3, -24, 3, 1, -7, -59, 4, 52, 1, -124, 19,
        -104, 94, -91, 94, 112, 94, -44, 0, 16, 0, 16, 0, -104, -113, 55, 0, -104, 24, 0, 0, -120, -56, -4, -1, 18, 111,
        3, 0, -2, -52, -4, -1, 24, -5, -1, -1, -26, 39, 77, 0, 0, 0, 0, 0, 0, 0, 0, 0, -6, 2, -24, 3, -24, 3, -5, -8,
        -55, 5, -8, -8, -124, 19, -88, 94, -81, 94, 126, 94, -48, 0, 16, 0, 0, 0, 2, 87, 55, 0, -40, 23, 0, 0, -32, 13,
        0, 0, -96, 6, 3, 0, -60, -53, -4, -1, -100, 6, 0, 0, 86, -78, 75, 0, 50, -77, 0, 0, 0, -128, 0, 0, -7, 2, -24,
        3, -24, 3, 3, -7, -8, -8, 125, -6, -123, 19, -110, 94, -65, 94, 86, 94, -47, 0, 16, 0, 16, 0, -110, 80, 55, 0,
        60, 19, 0, 0, 28, 4, 0, 0, 18, 16, 3, 0, 96, -54, -4, -1, -102, -57, -4, -1, -10, -15, 75, 0, 50, -13, 0, 0, 50,
        -13, 2, 0, -6, 2, -24, 3, -24, 3, -5, -8, -75, -4, -8, -8, -123, 19, -83, 94, -66, 94, 93, 94, -43, 0, 16, 0, 0,
        0, -32, 106, 55, 0, 114, 10, 0, 0, -16, 5, 0, 0, 12, 64, 3, 0, 126, -54, -4, -1, -56, 3, 0, 0, -106, 100, 77, 0,
        0, -64, 2, 0, 102, -26, 1, 0, -6, 2, -24, 3, -24, 3, -4, -8, -83, -7, -8, -8
    };

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

    public void meterValueUpdatedCall100() throws IOException {
        sendPacket(100, CALL_ID_100_VALUE);
    }

    public void meterValueUpdatedCall105() throws IOException {
        sendPacket(105, CALL_ID_105_VALUE);
    }

    @Override
    protected void updateChannel(short channelNumber, byte[] value) {
        throw new UnsupportedOperationException("There are no channels that OH can update in MEW-01!");
    }
}
