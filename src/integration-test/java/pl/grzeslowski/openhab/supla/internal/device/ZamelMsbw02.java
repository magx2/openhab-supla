package pl.grzeslowski.openhab.supla.internal.device;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static pl.grzeslowski.jsupla.protocol.api.channeltype.value.ActionTrigger.Capabilities.HOLD;
import static pl.grzeslowski.jsupla.protocol.api.channeltype.value.ActionTrigger.Capabilities.SHORT_PRESS_x1;
import static pl.grzeslowski.jsupla.protocol.api.consts.ProtoConsts.*;
import static pl.grzeslowski.openhab.supla.internal.server.ByteArrayToHex.hexToBytes;

import java.io.IOException;
import java.util.Arrays;
import org.eclipse.jdt.annotation.NonNullByDefault;
import pl.grzeslowski.jsupla.protocol.api.channeltype.value.ActionTrigger.Capabilities;
import pl.grzeslowski.jsupla.protocol.api.structs.ActionTriggerProperties;
import pl.grzeslowski.jsupla.protocol.api.structs.ds.ActionTrigger;
import pl.grzeslowski.jsupla.protocol.api.structs.ds.SuplaDeviceChannelE;
import pl.grzeslowski.jsupla.protocol.api.structs.ds.SuplaDeviceChannelValueC;
import pl.grzeslowski.jsupla.protocol.api.structs.ds.SuplaRegisterDeviceG;

@NonNullByDefault
public class ZamelMsbw02 extends Device {
    private static final int RELAY_FUNCTIONS = 239;
    private static final long RELAY_FLAGS = 151060480L;
    private static final long SENSOR_FLAGS = 134283264L;
    private static final long ACTION_TRIGGER_FLAGS = 65536L;
    private static final long ACTION_TRIGGER_CAPABILITIES = 64515L;

    private final byte[] email;
    private final byte[] authKey;
    private final boolean[] states = new boolean[6];

    public ZamelMsbw02(String guid, String email, String authKey) {
        super((short) 25, guid);
        this.email = fixedSize(email, SUPLA_EMAIL_MAXSIZE);
        this.authKey = hexToBytes(authKey);
    }

    @Override
    public void register() throws IOException {
        log.info("Registering Zamel mSBW-02 device");
        var channels = new SuplaDeviceChannelE[] {
            relay((short) 0, 20),
            relay((short) 1, 10),
            sensor((short) 2, 60),
            sensor((short) 3, 60),
            sensor((short) 4, 50),
            sensor((short) 5, 50),
            actionTrigger((short) 6),
            actionTrigger((short) 7),
            actionTrigger((short) 8),
            actionTrigger((short) 9)
        };
        var proto = new SuplaRegisterDeviceG(
                email,
                authKey,
                hexToBytes(guid),
                fixedSize("ZAMEL mSBW-02", SUPLA_DEVICE_NAME_MAXSIZE),
                fixedSize("25.11.02", SUPLA_SOFTVER_MAXSIZE),
                fixedSize("192.168.1.50", SUPLA_SERVER_NAME_MAXSIZE),
                117968,
                (short) 4,
                (short) 9850,
                (short) channels.length,
                channels);
        send(proto);
    }

    public boolean getState(int channelNumber) {
        assertThat(channelNumber).isBetween(0, states.length - 1);
        return states[channelNumber];
    }

    public void toggleState(int channelNumber) throws IOException {
        assertThat(channelNumber).isBetween(0, states.length - 1);
        states[channelNumber] = !states[channelNumber];
        send(new SuplaDeviceChannelValueC(
                (short) channelNumber, (short) SUPLA_CHANNEL_OFFLINE_FLAG_ONLINE, 0, encodedState(channelNumber)));
    }

    public void shortPress(int channelNumber) throws IOException {
        trigger(channelNumber, SHORT_PRESS_x1);
    }

    public void hold(int channelNumber) throws IOException {
        trigger(channelNumber, HOLD);
    }

    @Override
    protected void updateChannel(short channelNumber, byte[] value) {
        assertThat(channelNumber).isBetween((short) 0, (short) 1);
        states[channelNumber] = value[0] == 1;
    }

    private void trigger(int channelNumber, Capabilities capability) throws IOException {
        assertThat(channelNumber).isBetween(6, 9);
        send(new ActionTrigger((short) channelNumber, capability.toMask(), new short[10]));
    }

    private SuplaDeviceChannelE relay(short number, int defaultValue) {
        return new SuplaDeviceChannelE(
                number,
                2900,
                RELAY_FUNCTIONS,
                null,
                null,
                defaultValue,
                RELAY_FLAGS,
                (short) 0,
                0L,
                encodedState(number),
                null,
                null,
                (short) 0,
                (short) 0);
    }

    private SuplaDeviceChannelE sensor(short number, int defaultValue) {
        return new SuplaDeviceChannelE(
                number,
                1000,
                0,
                null,
                null,
                defaultValue,
                SENSOR_FLAGS,
                (short) 0,
                0L,
                encodedState(number),
                null,
                null,
                (short) 0,
                (short) 0);
    }

    private static SuplaDeviceChannelE actionTrigger(short number) {
        return new SuplaDeviceChannelE(
                number,
                11000,
                null,
                ACTION_TRIGGER_CAPABILITIES,
                null,
                700,
                ACTION_TRIGGER_FLAGS,
                (short) 2,
                0L,
                null,
                new ActionTriggerProperties((short) 0, 2048L, (short) 0, (short) 0),
                null,
                (short) 0,
                (short) 0);
    }

    private byte[] encodedState(int channelNumber) {
        return new byte[] {(byte) (states[channelNumber] ? 1 : 0), 0, 0, 0, 0, 0, 0, 0};
    }

    private static byte[] fixedSize(String value, int size) {
        var bytes = value.getBytes(UTF_8);
        assertThat(bytes).hasSizeLessThanOrEqualTo(size);
        return Arrays.copyOf(bytes, size);
    }
}
