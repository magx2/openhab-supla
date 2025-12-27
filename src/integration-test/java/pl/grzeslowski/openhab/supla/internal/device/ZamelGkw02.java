package pl.grzeslowski.openhab.supla.internal.device;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static pl.grzeslowski.jsupla.protocol.api.channeltype.value.ActionTrigger.Capabilities.HOLD;
import static pl.grzeslowski.jsupla.protocol.api.channeltype.value.ActionTrigger.Capabilities.SHORT_PRESS_x1;
import static pl.grzeslowski.jsupla.protocol.api.consts.ProtoConsts.*;
import static pl.grzeslowski.openhab.supla.internal.server.ByteArrayToHex.hexToBytes;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.Arrays;
import lombok.Getter;
import pl.grzeslowski.jsupla.protocol.api.ChannelType;
import pl.grzeslowski.jsupla.protocol.api.channeltype.decoders.ChannelTypeDecoder;
import pl.grzeslowski.jsupla.protocol.api.channeltype.encoders.ThermometerTypeDoubleChannelEncoderImpl;
import pl.grzeslowski.jsupla.protocol.api.channeltype.value.TemperatureValue;
import pl.grzeslowski.jsupla.protocol.api.structs.ActionTriggerProperties;
import pl.grzeslowski.jsupla.protocol.api.structs.ds.ActionTrigger;
import pl.grzeslowski.jsupla.protocol.api.structs.ds.SuplaDeviceChannelD;
import pl.grzeslowski.jsupla.protocol.api.structs.ds.SuplaDeviceChannelValueC;
import pl.grzeslowski.jsupla.protocol.api.structs.ds.SuplaRegisterDeviceF;
import pl.grzeslowski.openhab.supla.internal.extension.random.RandomExtension;

public class ZamelGkw02 extends Device {
    private final ThermometerTypeDoubleChannelEncoderImpl thermometerTypeDoubleChannelEncoder =
            new ThermometerTypeDoubleChannelEncoderImpl();
    private final HvacChannel hvac = RandomExtension.INSTANCE.randomHvac();

    @Getter
    private BigDecimal temperature = RandomExtension.INSTANCE.randomTemperature();

    private final byte[] email;
    private final byte[] authKey;

    public ZamelGkw02(String guid, String email, String authKey) {
        super((short) 23, guid);
        assertThat(email.getBytes(UTF_8)).hasSizeLessThanOrEqualTo(SUPLA_EMAIL_MAXSIZE);
        this.email = Arrays.copyOf(email.getBytes(UTF_8), SUPLA_EMAIL_MAXSIZE);
        this.authKey = hexToBytes(authKey);
    }

    @Override
    public void register() throws IOException {
        log.info("Registering Zamel GKW-02 device");
        var channels = new SuplaDeviceChannelD[] {
            new SuplaDeviceChannelD(
                    (short) 0,
                    6100,
                    131072,
                    null,
                    420,
                    419495936L,
                    (short) 0,
                    0L,
                    null,
                    null,
                    hvac.toHvacValue(),
                    (short) 0),
            new SuplaDeviceChannelD(
                    (short) 1,
                    3034,
                    0,
                    null,
                    40,
                    134283264L,
                    (short) 0,
                    0L,
                    thermometerTypeDoubleChannelEncoder.encode(new TemperatureValue(temperature)),
                    null,
                    null,
                    (short) 0),
            new SuplaDeviceChannelD(
                    (short) 2,
                    11000,
                    null,
                    54515L,
                    700,
                    65536L,
                    (short) 0,
                    0L,
                    null,
                    new ActionTriggerProperties((short) 0, 0),
                    null,
                    (short) 0),
            new SuplaDeviceChannelD(
                    (short) 3,
                    11000,
                    null,
                    54515L,
                    700,
                    65536L,
                    (short) 0,
                    0L,
                    null,
                    new ActionTriggerProperties((short) 0, 0),
                    null,
                    (short) 0),
            new SuplaDeviceChannelD(
                    (short) 4,
                    11000,
                    null,
                    54515L,
                    700,
                    65536L,
                    (short) 0,
                    0L,
                    null,
                    new ActionTriggerProperties((short) 0, 0),
                    null,
                    (short) 0)
        };
        var proto = new SuplaRegisterDeviceF(
                email,
                authKey,
                hexToBytes(guid),
                // name = ZAMEL GKW-02
                new byte[] {
                    90, 65, 77, 69, 76, 32, 71, 75, 87, 45, 48, 50, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                    0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                    0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                    0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                    0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                    0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                    0, 0, 0, 0, 0, 0, 0
                },
                // softVer = 25.03
                new byte[] {50, 53, 46, 48, 51, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0},
                // serverName = 192.168.1.29
                new byte[] {
                    49, 57, 50, 46, 49, 54, 56, 46, 49, 46, 50, 57, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                    0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                    0, 0, 0
                },
                3216,
                (short) 4,
                (short) 7010,
                (short) channels.length,
                channels);
        send(proto);
    }

    public void temperatureUpdated() throws IOException {
        temperature = RandomExtension.INSTANCE.randomUpdateTemperature(temperature);
        var proto = new SuplaDeviceChannelValueC(
                (short) 1,
                (short) SUPLA_CHANNEL_OFFLINE_FLAG_ONLINE,
                0,
                thermometerTypeDoubleChannelEncoder.encode(new TemperatureValue(temperature)));
        send(proto);
    }

    @Override
    protected void updateChannel(short channelNumber, byte[] value) {
        switch (channelNumber) {
            case 0:
                updateHvac(value);
            case 1:
                updateTemperature(value);
            default:
                throw new IllegalArgumentException("channelNumber has to be less than 2! Was " + channelNumber);
        }
    }

    private void updateHvac(byte[] value) {}

    private void updateTemperature(byte[] value) {
        var decode = ChannelTypeDecoder.INSTANCE.decode(ChannelType.SUPLA_CHANNELTYPE_THERMOMETER, value);
        switch (decode) {
            case TemperatureValue(var temp) -> temperature = temp;
            case null, default ->
                throw new IllegalArgumentException(
                        "Value is not a temperature! value=%s, decode=%s".formatted(Arrays.toString(value), decode));
        }
    }

    private void trigger(
            int triggerChannel,
            pl.grzeslowski.jsupla.protocol.api.channeltype.value.ActionTrigger.Capabilities capabilities)
            throws IOException {
        send(new ActionTrigger((short) (2 + triggerChannel), capabilities.toMask(), new short[10]));
    }

    public void shortPress(int triggerChannel) throws IOException {
        trigger(triggerChannel, SHORT_PRESS_x1);
    }

    public void hold(int triggerChannel) throws IOException {
        trigger(triggerChannel, HOLD);
    }
}
