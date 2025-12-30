package pl.grzeslowski.openhab.supla.internal.device;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static pl.grzeslowski.jsupla.protocol.api.ChannelType.SUPLA_CHANNELTYPE_DIMMERANDRGBLED;
import static pl.grzeslowski.jsupla.protocol.api.channeltype.value.ActionTrigger.Capabilities.HOLD;
import static pl.grzeslowski.jsupla.protocol.api.channeltype.value.ActionTrigger.Capabilities.SHORT_PRESS_x1;
import static pl.grzeslowski.jsupla.protocol.api.channeltype.value.RgbValue.Command.TURN_OFF_DIMMER;
import static pl.grzeslowski.jsupla.protocol.api.channeltype.value.RgbValue.Command.TURN_ON_DIMMER;
import static pl.grzeslowski.jsupla.protocol.api.consts.ProtoConsts.SUPLA_EMAIL_MAXSIZE;
import static pl.grzeslowski.openhab.supla.internal.server.ByteArrayToHex.hexToBytes;

import java.io.IOException;
import java.util.Arrays;
import lombok.Getter;
import pl.grzeslowski.jsupla.protocol.api.channeltype.decoders.ChannelTypeDecoder;
import pl.grzeslowski.jsupla.protocol.api.channeltype.encoders.ColorTypeChannelEncoderImpl;
import pl.grzeslowski.jsupla.protocol.api.channeltype.value.ActionTrigger.Capabilities;
import pl.grzeslowski.jsupla.protocol.api.channeltype.value.RgbValue;
import pl.grzeslowski.jsupla.protocol.api.structs.ActionTriggerProperties;
import pl.grzeslowski.jsupla.protocol.api.structs.ds.ActionTrigger;
import pl.grzeslowski.jsupla.protocol.api.structs.ds.SuplaDeviceChannelC;
import pl.grzeslowski.jsupla.protocol.api.structs.ds.SuplaDeviceChannelValueA;
import pl.grzeslowski.jsupla.protocol.api.structs.ds.SuplaRegisterDeviceE;
import pl.grzeslowski.openhab.supla.internal.extension.random.RandomExtension;

public class ZamelMslw01 extends Device {
    private final ColorTypeChannelEncoderImpl encoder = new ColorTypeChannelEncoderImpl();
    private final byte[] email;
    private final byte[] authKey;

    @Getter
    private RgbValue rgbwValue = RandomExtension.INSTANCE.randomRgbValue();

    public ZamelMslw01(String guid, String email, String authKey) {
        super((short) 18, guid);
        assertThat(email.getBytes(UTF_8)).hasSizeLessThanOrEqualTo(SUPLA_EMAIL_MAXSIZE);
        this.email = Arrays.copyOf(email.getBytes(UTF_8), SUPLA_EMAIL_MAXSIZE);
        this.authKey = hexToBytes(authKey);
    }

    @Override
    public void register() throws IOException {
        var channels = new SuplaDeviceChannelC[] {
            new SuplaDeviceChannelC((short) 0, 4020, 0, null, 200, 65792, encoder.encode(rgbwValue), null, null),
            new SuplaDeviceChannelC(
                    (short) 1,
                    11000,
                    null,
                    64515L,
                    700,
                    65536,
                    null,
                    new ActionTriggerProperties((short) 1, 3072),
                    null)
        };
        var proto = new SuplaRegisterDeviceE(
                email,
                authKey,
                hexToBytes(guid),
                new byte[] {
                    90, 65, 77, 69, 76, 32, 109, 83, 76, 87, 45, 48, 49, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                    0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                    0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                    0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                    0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                    0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                    0, 0, 0, 0, 0, 0, 0
                },
                new byte[] {50, 51, 46, 49, 50, 46, 48, 49, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0},
                new byte[] {
                    49, 57, 50, 46, 49, 54, 56, 46, 49, 46, 50, 57, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                    0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                    0, 0, 0
                },
                16,
                (short) 4,
                (short) 9000,
                (short) channels.length,
                channels);
        send(proto);
    }

    @Override
    protected void updateChannel(short channelNumber, byte[] value) {
        assertThat(channelNumber).isZero();
        updateRgb(value);
    }

    private void updateRgb(byte[] value) {
        var rgbwValue = (RgbValue) ChannelTypeDecoder.INSTANCE.decode(SUPLA_CHANNELTYPE_DIMMERANDRGBLED, value);
        if (rgbwValue.command().equals(TURN_ON_DIMMER) || rgbwValue.command().equals(TURN_OFF_DIMMER)) {
            this.rgbwValue = new RgbValue(
                    rgbwValue.brightness(),
                    this.rgbwValue.colorBrightness(),
                    this.rgbwValue.red(),
                    this.rgbwValue.green(),
                    this.rgbwValue.blue());
        } else {
            this.rgbwValue = rgbwValue;
        }
        log.info("Updated RGBW value to {} (from {})", rgbwValue, Arrays.toString(value));
    }

    private void trigger(Capabilities capabilities) throws IOException {
        send(new ActionTrigger((short) 1, capabilities.toMask(), new short[10]));
    }

    public void shortPress() throws IOException {
        trigger(SHORT_PRESS_x1);
    }

    public void hold() throws IOException {
        trigger(HOLD);
    }

    public void rgbUpdated() throws IOException {
        rgbwValue = RandomExtension.INSTANCE.randomRgbValue();
        var value = encoder.encode(rgbwValue);
        var proto = new SuplaDeviceChannelValueA((short) 0, value);
        send(proto);
    }
}
