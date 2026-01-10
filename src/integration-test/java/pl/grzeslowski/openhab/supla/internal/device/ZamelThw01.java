package pl.grzeslowski.openhab.supla.internal.device;

import static java.math.RoundingMode.HALF_EVEN;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static pl.grzeslowski.jsupla.protocol.api.consts.ProtoConsts.SUPLA_CHANNEL_OFFLINE_FLAG_ONLINE;
import static pl.grzeslowski.jsupla.protocol.api.consts.ProtoConsts.SUPLA_EMAIL_MAXSIZE;
import static pl.grzeslowski.openhab.supla.internal.server.ByteArrayToHex.hexToBytes;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.Arrays;
import lombok.Getter;
import org.eclipse.jdt.annotation.Nullable;
import pl.grzeslowski.jsupla.protocol.api.channeltype.encoders.ChannelTypeEncoder;
import pl.grzeslowski.jsupla.protocol.api.channeltype.value.TemperatureAndHumidityValue;
import pl.grzeslowski.jsupla.protocol.api.structs.ds.*;
import pl.grzeslowski.openhab.supla.internal.extension.random.RandomExtension;

public class ZamelThw01 extends Device {
    private final ChannelTypeEncoder encoder = ChannelTypeEncoder.INSTANCE;

    @Getter
    private HvacChannel hvac = RandomExtension.INSTANCE.randomHvac();

    @Getter
    private BigDecimal temperature = BigDecimal.valueOf(22.075);

    @Getter
    private BigDecimal humidity = BigDecimal.valueOf(33.837);

    private final byte[] email;
    private final byte[] authKey;

    public ZamelThw01(String guid, String email, String authKey) {
        super((short) 18, guid);
        assertThat(email.getBytes(UTF_8)).hasSizeLessThanOrEqualTo(SUPLA_EMAIL_MAXSIZE);
        this.email = Arrays.copyOf(email.getBytes(UTF_8), SUPLA_EMAIL_MAXSIZE);
        this.authKey = hexToBytes(authKey);
    }

    @Override
    public void register() throws IOException {
        log.info("Registering Zamel GKW-02 device");
        var channels = new SuplaDeviceChannelC[] {
            new SuplaDeviceChannelC(
                    (short) 0,
                    3038, // SUPLA_CHANNELTYPE_HUMIDITYANDTEMPSENSOR
                    0,
                    null,
                    45, // SUPLA_CHANNELFNC_HUMIDITYANDTEMPERATURE
                    65536, // SUPLA_CHANNEL_FLAG_CHANNELSTATE
                    // TemperatureAndHumidityValue[temperature=22.075,
                    // humidity=Optional[HumidityValue[humidity=33.837]]]
                    new byte[] {59, 86, 0, 0, 45, -124, 0, 0},
                    null,
                    null),
            new SuplaDeviceChannelC(
                    (short) 1,
                    3034, // SUPLA_CHANNELTYPE_THERMOMETER
                    0,
                    null,
                    0, // SUPLA_CHANNELFNC_NONE
                    65536, // SUPLA_CHANNEL_FLAG_CHANNELSTATE
                    // TemperatureDoubleValue[temperature=-275.0]
                    new byte[] {0, 0, 0, 0, 0, 48, 113, -64},
                    null,
                    null),
            new SuplaDeviceChannelC(
                    (short) 2,
                    3034, // SUPLA_CHANNELTYPE_THERMOMETER
                    0,
                    null,
                    0, // SUPLA_CHANNELFNC_NONE
                    65536, // SUPLA_CHANNEL_FLAG_CHANNELSTATE
                    // TemperatureDoubleValue[temperature=-275.0]
                    new byte[] {0, 0, 0, 0, 0, 48, 113, -64},
                    null,
                    null),
            new SuplaDeviceChannelC(
                    (short) 3,
                    3034, // SUPLA_CHANNELTYPE_THERMOMETER
                    0,
                    null,
                    0, // SUPLA_CHANNELFNC_NONE
                    65536, // SUPLA_CHANNEL_FLAG_CHANNELSTATE
                    // TemperatureDoubleValue[temperature=-275.0]
                    new byte[] {0, 0, 0, 0, 0, 48, 113, -64},
                    null,
                    null),
            new SuplaDeviceChannelC(
                    (short) 4,
                    3034, // SUPLA_CHANNELTYPE_THERMOMETER
                    0,
                    null,
                    0, // SUPLA_CHANNELFNC_NONE
                    65536, // SUPLA_CHANNEL_FLAG_CHANNELSTATE
                    // TemperatureDoubleValue[temperature=-275.0]
                    new byte[] {0, 0, 0, 0, 0, 48, 113, -64},
                    null,
                    null),
        };
        var proto = new SuplaRegisterDeviceE(
                email,
                authKey,
                hexToBytes(guid),
                // name = ZAMEL THW-01
                new byte[] {
                    90, 65, 77, 69, 76, 32, 84, 72, 87, 45, 48, 49, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                    0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                    0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                    0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                    0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                    0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                    0, 0, 0, 0, 0, 0, 0
                },
                // softVer = 22.11.03
                new byte[] {50, 50, 46, 49, 49, 46, 48, 51, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0},
                // serverName = 192.168.1.29
                new byte[] {
                    49, 57, 50, 46, 49, 54, 56, 46, 49, 46, 50, 57, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                    0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                    0, 0, 0
                },
                // flags = [SUPLA_DEVICE_FLAG_CALCFG_ENTER_CFG_MODE, SUPLA_DEVICE_FLAG_SLEEP_MODE_ENABLED]
                48,
                (short) 4,
                (short) 6000,
                (short) channels.length,
                channels);
        send(proto);
    }

    @Override
    protected void updateChannel(short channelNumber, byte[] value) {
        throw new UnsupportedOperationException("There are no channels that OH can update in THW-01!");
    }

    private void temperatureAndHumidityUpdated(@Nullable BigDecimal temperature, @Nullable BigDecimal humidity)
            throws IOException {
        if (temperature != null) {
            log.info("Updating temperature {}->{}", this.temperature, temperature);
            this.temperature = temperature;
        }
        if (humidity != null) {
            log.info("Updating humidity {}->{}", this.humidity, humidity);
            this.humidity = humidity;
        }
        var proto = new SuplaDeviceChannelValueC(
                (short) 0,
                (short) SUPLA_CHANNEL_OFFLINE_FLAG_ONLINE,
                60, // important - there is 60 secs validity!
                encoder.encode(new TemperatureAndHumidityValue(this.temperature, this.humidity)));
        send(proto);
    }

    public void temperatureAndHumidityUpdated() throws IOException {
        temperatureAndHumidityUpdated(randomTemperature(), randomHumidity());
    }

    public void temperatureUpdated() throws IOException {
        temperatureAndHumidityUpdated(randomTemperature(), null);
    }

    public void humidityUpdated() throws IOException {
        temperatureAndHumidityUpdated(null, randomHumidity());
    }

    private BigDecimal randomTemperature() {
        return RandomExtension.INSTANCE.randomTemperature(temperature).setScale(3, HALF_EVEN);
    }

    private BigDecimal randomHumidity() {
        return RandomExtension.INSTANCE.randomPercentage(humidity).setScale(3, HALF_EVEN);
    }
}
