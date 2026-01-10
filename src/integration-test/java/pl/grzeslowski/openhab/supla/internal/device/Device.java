package pl.grzeslowski.openhab.supla.internal.device;

import static java.time.Instant.now;
import static java.util.Objects.requireNonNull;
import static org.assertj.core.api.Assertions.assertThat;
import static pl.grzeslowski.jsupla.protocol.api.consts.ProtoConsts.SUPLA_TAG;

import java.io.IOException;
import java.net.Socket;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicLong;
import org.eclipse.jdt.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.grzeslowski.jsupla.protocol.api.calltypes.CallTypeParser;
import pl.grzeslowski.jsupla.protocol.api.decoders.DecoderFactoryImpl;
import pl.grzeslowski.jsupla.protocol.api.decoders.SuplaDataPacketDecoder;
import pl.grzeslowski.jsupla.protocol.api.encoders.EncoderFactoryImpl;
import pl.grzeslowski.jsupla.protocol.api.encoders.SuplaDataPacketEncoder;
import pl.grzeslowski.jsupla.protocol.api.structs.SuplaDataPacket;
import pl.grzeslowski.jsupla.protocol.api.structs.SuplaTimeval;
import pl.grzeslowski.jsupla.protocol.api.structs.dcs.SuplaPingServer;
import pl.grzeslowski.jsupla.protocol.api.structs.sd.SuplaChannelNewValue;
import pl.grzeslowski.jsupla.protocol.api.structs.sd.SuplaRegisterDeviceResultA;
import pl.grzeslowski.jsupla.protocol.api.structs.sdc.SuplaPingServerResult;
import pl.grzeslowski.jsupla.protocol.api.types.FromServerProto;
import pl.grzeslowski.jsupla.protocol.api.types.ToServerProto;

public abstract class Device implements AutoCloseable {
    protected final Logger log;
    protected final String guid;

    private final AtomicLong id = new AtomicLong();
    private final byte[] buffer = new byte[1024];
    private final short version;
    private @Nullable Socket socket;

    public Device(short version, String guid) {
        log = LoggerFactory.getLogger(getClass().getName() + "." + guid);
        this.guid = guid;
        this.version = version;
    }

    public synchronized void initialize(String host, int serverPort) throws IOException {
        if (socket != null) {
            throw new IllegalStateException("Invoke `close` before initialising again!");
        }
        log.info("Connecting to server {}:{}", host, serverPort);
        socket = new Socket(host, serverPort);
    }

    protected synchronized void send(ToServerProto proto) throws IOException {
        requireConnection();
        log.info("Sending proto to server: {}", proto);
        var encoder = EncoderFactoryImpl.INSTANCE.getEncoder(proto);
        var encode = encoder.encode(proto);

        var packet = new SuplaDataPacket(
                version, id.incrementAndGet(), proto.callType().getValue(), encode.length, encode);
        var bytes = SuplaDataPacketEncoder.INSTANCE.encode(packet);

        var out = requireNonNull(socket).getOutputStream();
        out.write(SUPLA_TAG);
        out.write(bytes);
        out.write(SUPLA_TAG);
        out.flush();
    }

    protected synchronized FromServerProto read() throws IOException {
        requireConnection();

        var in = requireNonNull(socket).getInputStream();
        var suplaTagOffset = 0;
        while (suplaTagOffset < SUPLA_TAG.length) {
            var read = in.read();
            if (read == -1) {
                throw new IllegalStateException("Stream is closed! No data!");
            }
            var letter = (byte) read;
            if (letter != SUPLA_TAG[suplaTagOffset]) {
                suplaTagOffset = 0;
            } else {
                suplaTagOffset++;
            }
        }

        var offset = 0;
        var endSuplaTagRead = false;
        while (!endSuplaTagRead) {
            var read = in.read(buffer, offset, buffer.length - offset);
            if (read == -1) {
                Arrays.fill(buffer, (byte) 0); // clear the buffer for next use
                throw new IllegalStateException("No more bytes!");
            }
            offset += read;
            if (offset < SUPLA_TAG.length) {
                // did not read the minimal length for supla tag
                continue;
            }
            var readWholeTag = true;
            for (var idx = 0; idx < SUPLA_TAG.length; idx++) {
                // counts from last byte to first byte in SUPLA_TAG
                if (buffer[offset - SUPLA_TAG.length + idx] != SUPLA_TAG[idx]) {
                    readWholeTag = false;
                    break;
                }
            }
            if (readWholeTag) {
                endSuplaTagRead = true;
            }
        }
        var packet = SuplaDataPacketDecoder.INSTANCE.decode(buffer, 0);
        Arrays.fill(buffer, (byte) 0); // clear the buffer for next use

        var decoder = CallTypeParser.INSTANCE
                .parse(packet.callId())
                .map(DecoderFactoryImpl.INSTANCE::getDecoder)
                .orElseThrow(() -> new IllegalArgumentException("Cannot find call type for value " + packet.callId()));
        var decode = decoder.decode(packet.data());
        if (!(decode instanceof FromServerProto fsp)) {
            throw new IllegalArgumentException(
                    "Decodes proto does not have class %s, it's current type is %s. Decoded proto: %s"
                            .formatted(
                                    FromServerProto.class.getSimpleName(),
                                    decode.getClass().getSimpleName(),
                                    decode));
        }
        return fsp;
    }

    protected void requireConnection() {
        if (socket == null) {
            throw new IllegalStateException("Device is not initialized! Run `initialize(String, int)` before.");
        }
    }

    @Override
    public synchronized void close() throws Exception {
        log.info("Closing {}", guid);
        Arrays.fill(buffer, (byte) 0); // clear the buffer for next use
        if (socket != null) {
            log.info("Closing server socket");
            socket.close();
        }
    }

    public SuplaRegisterDeviceResultA readRegisterDeviceResultA() throws IOException {
        var read = read();
        assertThat(read).isInstanceOf(SuplaRegisterDeviceResultA.class);
        return (SuplaRegisterDeviceResultA) read;
    }

    public void ping() throws IOException {
        sendPing();
        readPing();
    }

    public void sendPing() throws IOException {
        send(new SuplaPingServer(new SuplaTimeval(now().getEpochSecond(), 0)));
    }

    public SuplaPingServerResult readPing() throws IOException {
        var read = read();
        assertThat(read).isInstanceOf(SuplaPingServerResult.class);
        return (SuplaPingServerResult) read;
    }

    protected SuplaChannelNewValue readChannelNewValue() throws IOException {
        var read = read();
        assertThat(read).isInstanceOf(SuplaChannelNewValue.class);
        return (SuplaChannelNewValue) read;
    }

    public void updateChannel() throws IOException {
        var newValue = readChannelNewValue();
        updateChannel(newValue.channelNumber(), newValue.value());
    }

    public abstract void register() throws IOException;

    protected abstract void updateChannel(short channelNumber, byte[] value);
}
