package pl.grzeslowski.openhab.supla.internal.device;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static pl.grzeslowski.jsupla.protocol.api.ChannelType.SUPLA_CHANNELTYPE_DIMMER;
import static pl.grzeslowski.jsupla.protocol.api.consts.ProtoConsts.SUPLA_EMAIL_MAXSIZE;
import static pl.grzeslowski.openhab.supla.internal.server.ByteArrayToHex.hexToBytes;

import java.io.IOException;
import java.util.Arrays;
import lombok.Getter;
import pl.grzeslowski.jsupla.protocol.api.channeltype.decoders.ChannelTypeDecoder;
import pl.grzeslowski.jsupla.protocol.api.channeltype.encoders.ChannelTypeEncoder;
import pl.grzeslowski.jsupla.protocol.api.channeltype.value.PercentValue;
import pl.grzeslowski.jsupla.protocol.api.structs.ds.SuplaDeviceChannelC;
import pl.grzeslowski.jsupla.protocol.api.structs.ds.SuplaDeviceChannelValueA;
import pl.grzeslowski.jsupla.protocol.api.structs.ds.SuplaRegisterDeviceE;
import pl.grzeslowski.openhab.supla.internal.extension.random.RandomExtension;

public class ZamelDiw01 extends Device {
    private final ChannelTypeEncoder encoder = ChannelTypeEncoder.INSTANCE;
    private final ChannelTypeDecoder decoder = ChannelTypeDecoder.INSTANCE;

    private final byte[] email;
    private final byte[] authKey;

    @Getter
    private PercentValue value = RandomExtension.INSTANCE.randomPercentage();

    public ZamelDiw01(String guid, String email, String authKey) {
        super((short) 12, guid);
        assertThat(email.getBytes(UTF_8)).hasSizeLessThanOrEqualTo(SUPLA_EMAIL_MAXSIZE);
        this.email = Arrays.copyOf(email.getBytes(UTF_8), SUPLA_EMAIL_MAXSIZE);
        this.authKey = hexToBytes(authKey);
    }

    @Override
    public void register() throws IOException {
        var channel = new SuplaDeviceChannelC((short) 0, 4000, 0, null, 180, 65536, encoder.encode(value), null, null);
        var proto = new SuplaRegisterDeviceE(
                email,
                authKey,
                hexToBytes(guid),
                new byte[] {
                    90, 65, 77, 69, 76, 32, 68, 73, 87, 45, 48, 49, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                    0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                    0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                    0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                    0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                    0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                    0, 0, 0, 0, 0, 0, 0
                },
                new byte[] {50, 46, 56, 46, 51, 49, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0},
                new byte[] {
                    49, 57, 50, 46, 49, 54, 56, 46, 49, 46, 50, 57, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                    0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                    0, 0, 0
                },
                0,
                (short) 4,
                (short) 2000,
                (short) 1,
                new SuplaDeviceChannelC[] {channel});
        send(proto);
    }

    @Override
    protected void updateChannel(short channelNumber, byte[] value) {
        assertThat(channelNumber).isZero();
        this.value = (PercentValue) decoder.decode(SUPLA_CHANNELTYPE_DIMMER, value);
    }

    public void dim() throws IOException {
        this.value = RandomExtension.INSTANCE.randomPercentage(this.value);
        var proto = new SuplaDeviceChannelValueA((short) 0, encoder.encode(this.value));
        send(proto);
    }
}
