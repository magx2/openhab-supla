package pl.grzeslowski.openhab.supla.internal;

import static java.util.Arrays.stream;
import static org.assertj.core.api.Assertions.assertThat;
import static pl.grzeslowski.jsupla.protocol.api.ChannelFunction.SUPLA_CHANNELFNC_NONE;
import static pl.grzeslowski.jsupla.protocol.api.ProtocolHelpers.toSignedInt;
import static pl.grzeslowski.openhab.supla.internal.DecipherProtocol.Type.*;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import pl.grzeslowski.jsupla.protocol.api.*;
import pl.grzeslowski.jsupla.protocol.api.calltypes.CallTypeParser;
import pl.grzeslowski.jsupla.protocol.api.channeltype.decoders.ChannelTypeDecoder;
import pl.grzeslowski.jsupla.protocol.api.channeltype.decoders.HvacTypeDecoder;
import pl.grzeslowski.jsupla.protocol.api.channeltype.value.ChannelValue;
import pl.grzeslowski.jsupla.protocol.api.channeltype.value.HvacValue;
import pl.grzeslowski.jsupla.protocol.api.decoders.DecoderFactoryImpl;
import pl.grzeslowski.jsupla.protocol.api.structs.ds.*;
import pl.grzeslowski.openhab.supla.internal.server.ByteArrayToHex;

public class DecipherProtocol {

    private static final Pattern PACKET =
            Pattern.compile("(?s)\\s*SuplaDataPacket\\{\\s*" + "version=(?<version>-?\\d+)\\s*,\\s*"
                    + "rrId=(?<rrId>-?\\d+)\\s*,\\s*"
                    + "callId=(?<callId>-?\\d+)\\s*,\\s*"
                    + "dataSize=(?<dataSize>-?\\d+)\\s*,\\s*"
                    + "data=\\[(?<data>-?\\d+(?:\\s*,\\s*-?\\d+)*)]"
                    + "\\s*}");
    final CallTypeParser callTypeParser = CallTypeParser.INSTANCE;
    final DecoderFactoryImpl decoderFactory = DecoderFactoryImpl.INSTANCE;

    @SuppressWarnings("SwitchStatementWithTooFewBranches")
    @ParameterizedTest(name = "{index}: should decode `SuplaDataPacket` for {0}")
    @MethodSource
    void decodeSuplaDataPacket(String description, String text) {
        // when
        Matcher matcher = PACKET.matcher(text);
        assertThat(matcher.find()).isTrue();

        int version = Integer.parseInt(matcher.group("version"));
        int rrId = Integer.parseInt(matcher.group("rrId"));
        int callId = Integer.parseInt(matcher.group("callId"));
        int dataSize = Integer.parseInt(matcher.group("dataSize"));

        String[] parts = matcher.group("data").split("\\s*,\\s*");
        byte[] data = new byte[parts.length];

        for (int i = 0; i < parts.length; i++) {
            int v = Integer.parseInt(parts[i]);
            if (v < Byte.MIN_VALUE || v > Byte.MAX_VALUE) {
                throw new IllegalArgumentException("Value out of byte range: " + v);
            }
            data[i] = (byte) v;
        }

        // then
        System.out.println("                          ♠️♣️♥️♦️ " + description + " ♦️♥️♣️♠️");
        System.out.println("✅ version=" + version);
        System.out.println("✅ rrId=" + rrId);
        System.out.println("✅ callId=" + callId);
        System.out.println("✅ dataSize=" + dataSize);
        System.out.println("✅ data=" + Arrays.toString(data));

        var decoder = decoderFactory.getDecoder(callTypeParser.parse(callId).orElseThrow());
        var decode = decoder.decode(data);
        switch (decode) {
            case SuplaRegisterDevice register -> print(register);
            default -> System.out.println(decode);
        }
    }

    static Stream<Arguments> decodeSuplaDataPacket() {
        return Stream.of(
                Arguments.of(
                        "DIW-01",
                        "SuplaDataPacket{" //
                                + "version=12, "
                                + "rrId=1, "
                                + "callId=69, "
                                + "dataSize=609, "
                                + "data=[122, 97, 109, 101, 108, 64, 122, 97, 109, 101, 108, 46, 112, 108, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 87, 126, 41, -46, 64, 87, -126, 38, -43, 70, -50, -71, -117, -40, 95, 113, -36, 43, 104, -3, -97, 62, 77, -64, 71, 97, 45, -72, 91, 74, -51, -98, 90, 65, 77, 69, 76, 32, 68, 73, 87, 45, 48, 49, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 50, 46, 56, 46, 51, 49, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 49, 57, 50, 46, 49, 54, 56, 46, 49, 46, 50, 57, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 4, 0, -48, 7, 1, 0, -96, 15, 0, 0, 0, 0, 0, 0, -76, 0, 0, 0, 0, 0, 1, 0, 100, 0, 0, 0, 0, 0, 0, 0]}"),
                Arguments.of(
                        "mSLW-01",
                        "SuplaDataPacket{" //
                                + "version=18, "
                                + "rrId=1, "
                                + "callId=69, "
                                + "dataSize=634, "
                                + "data=[122, 97, 109, 101, 108, 64, 122, 97, 109, 101, 108, 46, 112, 108, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, -80, -67, 115, -119, -124, -98, -102, -21, -49, -126, -83, -7, -92, 1, 59, -118, -97, 48, -32, 8, -98, -85, -93, 10, 114, 68, -69, 88, -67, 85, 16, 81, 90, 65, 77, 69, 76, 32, 109, 83, 76, 87, 45, 48, 49, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 50, 51, 46, 49, 50, 46, 48, 49, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 49, 57, 50, 46, 49, 54, 56, 46, 49, 46, 50, 57, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 16, 0, 0, 0, 4, 0, 40, 35, 2, 0, -76, 15, 0, 0, 0, 0, 0, 0, -56, 0, 0, 0, 0, 1, 1, 0, 0, 85, 0, -39, 43, 0, 0, 0, 1, -8, 42, 0, 0, 3, -4, 0, 0, -68, 2, 0, 0, 0, 0, 1, 0, 1, 0, 12, 0, 0, 0, 0, 0]}"),
                Arguments.of(
                        "GKW-02",
                        "SuplaDataPacket{" //
                                + "version=23, "
                                + "rrId=1, "
                                + "callId=75, "
                                + "dataSize=759, "
                                + "data=[122, 97, 109, 101, 108, 64, 122, 97, 109, 101, 108, 46, 112, 108, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, -76, -103, 1, -29, -118, 11, 40, 79, -47, -53, 125, 58, 19, 67, 9, 96, -16, -53, -72, -75, -97, -69, 23, -42, -7, -77, 95, 121, -47, 77, -27, 104, 90, 65, 77, 69, 76, 32, 71, 75, 87, 45, 48, 50, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 50, 53, 46, 48, 51, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 49, 57, 50, 46, 49, 54, 56, 46, 49, 46, 50, 57, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, -112, 12, 0, 0, 4, 0, 98, 27, 5, 0, -44, 23, 0, 0, 0, 0, 2, 0, -92, 1, 0, 0, 0, 0, 1, 25, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 2, -116, 10, 0, 0, 5, 0, 0, 1, -38, 11, 0, 0, 0, 0, 0, 0, 40, 0, 0, 0, 0, 0, 1, 8, 0, 0, 0, 0, 0, 0, 0, 0, 0, -72, 30, -123, -21, 81, 56, 50, 64, 0, 2, -8, 42, 0, 0, 3, -4, 0, 0, -68, 2, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 3, -8, 42, 0, 0, 3, -4, 0, 0, -68, 2, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 4, -8, 42, 0, 0, 3, -4, 0, 0, -68, 2, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0]}"),
                Arguments.of(
                        "THW-01",
                        "SuplaDataPacket{" //
                                + "version=18, "
                                + "rrId=1, "
                                + "callId=69, "
                                + "dataSize=709, "
                                + "data=[122, 97, 109, 101, 108, 64, 122, 97, 109, 101, 108, 46, 112, 108, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, -16, 0, 107, -8, 46, 95, -32, 101, 116, -11, -86, -30, 102, -47, 79, -11, 22, -35, 108, -125, 103, -71, 60, -103, -114, 20, 6, -9, -65, 52, -6, 127, 90, 65, 77, 69, 76, 32, 84, 72, 87, 45, 48, 49, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 50, 50, 46, 49, 49, 46, 48, 51, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 49, 57, 50, 46, 49, 54, 56, 46, 49, 46, 50, 57, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 48, 0, 0, 0, 4, 0, 112, 23, 5, 0, -34, 11, 0, 0, 0, 0, 0, 0, 45, 0, 0, 0, 0, 0, 1, 0, 59, 86, 0, 0, 45, -124, 0, 0, 1, -38, 11, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 48, 113, -64, 2, -38, 11, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 48, 113, -64, 3, -38, 11, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 48, 113, -64, 4, -38, 11, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 48, 113, -64]}"));
    }

    private void print(SuplaRegisterDevice register) {
        System.out.println(findRegister(register));
        System.out.println("Channels:");
        var channels = findChannels(register);
        if (channels.isEmpty()) {
            System.out.println("<none>");
        } else {
            channels.stream().map(Object::toString).map(s -> "🔵 " + s).forEach(System.out::println);
        }
    }

    enum Type {
        A,
        B,
        C,
        D,
        E,
        F,
        G,
        H,
        I,
        J,
        K,
        L,
        M,
        N,
        O,
        P,
        R,
        S,
        T,
        U,
        V,
        X,
        Y,
        Z
    }

    private Register findRegister(SuplaRegisterDevice register) {
        return switch (register) {
            case SuplaRegisterDeviceA rd ->
                new Register(A, rd.locationId(), rd.locationPwd(), rd.guid(), rd.name(), rd.softVer(), null);
            case SuplaRegisterDeviceB rd ->
                new Register(B, rd.locationId(), rd.locationPwd(), rd.guid(), rd.name(), rd.softVer(), null);
            case SuplaRegisterDeviceC rd ->
                new Register(C, rd.locationId(), rd.locationPwd(), rd.guid(), rd.name(), rd.softVer(), rd.serverName());
            case SuplaRegisterDeviceD rd ->
                new Register(
                        D,
                        rd.email(),
                        rd.authKey(),
                        rd.guid(),
                        rd.name(),
                        rd.softVer(),
                        rd.serverName(),
                        0,
                        null,
                        null);
            case SuplaRegisterDeviceE rd ->
                new Register(
                        E,
                        rd.email(),
                        rd.authKey(),
                        rd.guid(),
                        rd.name(),
                        rd.softVer(),
                        rd.serverName(),
                        rd.flags(),
                        rd.manufacturerId(),
                        rd.productId());
            case SuplaRegisterDeviceF rd ->
                new Register(
                        F,
                        rd.email(),
                        rd.authKey(),
                        rd.guid(),
                        rd.name(),
                        rd.softVer(),
                        rd.serverName(),
                        rd.flags(),
                        rd.manufacturerId(),
                        rd.productId());
            case SuplaRegisterDeviceG rd ->
                new Register(
                        G,
                        rd.email(),
                        rd.authKey(),
                        rd.guid(),
                        rd.name(),
                        rd.softVer(),
                        rd.serverName(),
                        rd.flags(),
                        rd.manufacturerId(),
                        rd.productId());
        };
    }

    record Register(
            Type type,
            Integer locationId,
            String locationPwd,
            byte[] locationPwdNative,
            String email,
            byte[] emailNative,
            String authKey,
            byte[] authKeyNative,
            String guid,
            byte[] guidNative,
            String name,
            byte[] nameNative,
            String softVer,
            byte[] softVerNative,
            String serverName,
            byte[] serverNameNative,
            Set<DeviceFlag> flags,
            int flagsNative,
            Short manufacturerId,
            Short productId) {
        @Override
        public String toString() {
            return "Register" + type + "{" + " \n\tlocationId="
                    + locationId + ",\n\tlocationPwd='"
                    + locationPwd + '\'' + ", locationPwdNative="
                    + Arrays.toString(locationPwdNative) + ",\n\temail='"
                    + email + '\'' + ", emailNative="
                    + Arrays.toString(emailNative) + ",\n\tauthKey='"
                    + authKey + '\'' + ", authKeyNative="
                    + Arrays.toString(authKeyNative) + ",\n\tguid='"
                    + guid + '\'' + ", guidNative="
                    + Arrays.toString(guidNative) + ",\n\tname='"
                    + name + '\'' + ", nameNative="
                    + Arrays.toString(nameNative) + ",\n\tsoftVer='"
                    + softVer + '\'' + ", softVerNative="
                    + Arrays.toString(softVerNative) + ",\n\tserverName='"
                    + serverName + '\'' + ", serverNameNative="
                    + Arrays.toString(serverNameNative) + ",\n\tflags="
                    + flags + ", flagsNative="
                    + flagsNative + ",\n\tmanufacturerId="
                    + manufacturerId + ",\n\tproductId="
                    + productId + "\n}";
        }

        Register(
                Type type,
                byte[] email,
                byte[] authKey,
                byte[] guid,
                byte[] name,
                byte[] softVer,
                byte[] serverName,
                int flags,
                Short manufacturerId,
                Short productId) {
            this(
                    type,
                    null,
                    null,
                    null,
                    parse(email, ProtocolHelpers::parseString),
                    email,
                    parse(authKey, ByteArrayToHex::bytesToHex),
                    authKey,
                    parse(guid, ProtocolHelpers::parseHexString),
                    guid,
                    parse(name, ProtocolHelpers::parseString),
                    name,
                    parse(softVer, ProtocolHelpers::parseString),
                    softVer,
                    parse(serverName, ProtocolHelpers::parseString),
                    serverName,
                    DeviceFlag.findByMask(flags),
                    flags,
                    manufacturerId,
                    productId);
        }

        Register(
                Type type,
                int locationId,
                byte[] locationPwd,
                byte[] guid,
                byte[] name,
                byte[] softVer,
                byte[] serverName) {
            this(
                    type,
                    locationId,
                    parse(locationPwd, ByteArrayToHex::bytesToHex),
                    locationPwd,
                    null, // email
                    null, // email
                    null, // auth key
                    null, // auth key
                    parse(guid, ProtocolHelpers::parseHexString),
                    guid,
                    parse(name, ProtocolHelpers::parseString),
                    name,
                    parse(softVer, ProtocolHelpers::parseString),
                    softVer,
                    parse(serverName, ProtocolHelpers::parseString),
                    serverName,
                    Set.of(),
                    0,
                    null,
                    null);
        }
    }

    private List<Channel> findChannels(SuplaRegisterDevice register) {
        return switch (register) {
            case SuplaRegisterDeviceA rd ->
                stream(rd.channels()).map(Channel::new).toList();
            case SuplaRegisterDeviceB rd ->
                stream(rd.channels()).map(Channel::new).toList();
            case SuplaRegisterDeviceC rd ->
                stream(rd.channels()).map(Channel::new).toList();
            case SuplaRegisterDeviceD rd ->
                stream(rd.channels()).map(Channel::new).toList();
            case SuplaRegisterDeviceE rd ->
                stream(rd.channels()).map(Channel::new).toList();
            case SuplaRegisterDeviceF rd ->
                stream(rd.channels()).map(Channel::new).toList();
            case SuplaRegisterDeviceG rd ->
                stream(rd.channels()).map(Channel::new).toList();
        };
    }

    record Channel(
            Type protoType,
            short number,
            ChannelType type,
            Integer typeNative,
            Set<BitFunction> funcList,
            Integer funcListNative,
            Long actionTriggerCaps,
            Set<RgbwBitFunction> rGBWFuncList,
            Long rGBWFuncListNative,
            ChannelFunction defaultValue,
            Integer defaultValueNative,
            Set<ChannelFlag> flags,
            Long flagsNative,
            Short offline,
            Long valueValidityTimeSec,
            ChannelValue value,
            byte[] valueNative,
            pl.grzeslowski.jsupla.protocol.api.channeltype.value.ActionTrigger actionTrigger,
            pl.grzeslowski.jsupla.protocol.api.structs.ActionTriggerProperties actionTriggerProperties,
            HvacValue hvacValue,
            pl.grzeslowski.jsupla.protocol.api.structs.HVACValue hvacValueNative,
            Short defaultIcon,
            Short subDeviceId) {
        @Override
        public String toString() {
            return "Channel" + protoType + "{" + " \n\tnumber="
                    + number + ",\n\ttype="
                    + type + ", typeNative="
                    + typeNative + ",\n\tfuncList="
                    + funcList + ", funcListNative="
                    + funcListNative + ",\n\tactionTriggerCaps="
                    + actionTriggerCaps + ",\n\trGBWFuncList="
                    + rGBWFuncList + ", rGBWFuncListNative="
                    + rGBWFuncListNative + ",\n\tdefaultValue="
                    + defaultValue + ", defaultValueNative="
                    + defaultValueNative + ",\n\tflags="
                    + flags + ", flagsNative="
                    + flagsNative + ",\n\toffline="
                    + offline + ",\n\tvalueValidityTimeSec="
                    + valueValidityTimeSec + ",\n\tvalue="
                    + value + ", valueNative="
                    + Arrays.toString(valueNative) + ",\n\tactionTrigger="
                    + actionTrigger + ",\n\tactionTriggerProperties="
                    + actionTriggerProperties + ",\n\thvacValue="
                    + hvacValue + ", hvacValueNative="
                    + hvacValueNative + ",\n\tdefaultIcon="
                    + defaultIcon + ",\n\tsubDeviceId="
                    + subDeviceId + "\n}";
        }

        public Channel(SuplaDeviceChannelA channel) {
            this(
                    A,
                    channel.number(),
                    ChannelType.findByValue(channel.type()).orElse(null),
                    channel.type(),
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    ChannelTypeDecoder.INSTANCE.decode(channel.type(), channel.value()),
                    channel.value(),
                    null,
                    null,
                    null,
                    null,
                    null,
                    null);
        }

        public Channel(SuplaDeviceChannelB channel) {
            this(
                    B,
                    channel.number(),
                    ChannelType.findByValue(channel.type()).orElse(null),
                    channel.type(),
                    Optional.of(channel.funcList()).map(BitFunction::findByMask).orElse(Set.of()),
                    channel.funcList(),
                    null,
                    null,
                    null,
                    Optional.of(channel.defaultValue())
                            .flatMap(ChannelFunction::findByValue)
                            .orElse(SUPLA_CHANNELFNC_NONE),
                    channel.defaultValue(),
                    null,
                    null,
                    null,
                    null,
                    ChannelTypeDecoder.INSTANCE.decode(channel.type(), channel.value()),
                    channel.value(),
                    null,
                    null,
                    null,
                    null,
                    null,
                    null);
        }

        public Channel(SuplaDeviceChannelC channel) {
            this(
                    C,
                    channel.number(),
                    ChannelType.findByValue(channel.type()).orElse(null),
                    channel.type(),
                    Optional.ofNullable(channel.funcList())
                            .map(BitFunction::findByMask)
                            .orElse(Set.of()),
                    channel.funcList(),
                    channel.actionTriggerCaps(),
                    null,
                    null,
                    Optional.of(channel.defaultValue())
                            .flatMap(ChannelFunction::findByValue)
                            .orElse(SUPLA_CHANNELFNC_NONE),
                    channel.defaultValue(),
                    ChannelFlag.findByMask(channel.flags()),
                    (long) channel.flags(),
                    null,
                    null,
                    ChannelTypeDecoder.INSTANCE.decode(channel.type(), channel.value()),
                    channel.value(),
                    (channel.actionTriggerCaps() == null || channel.actionTriggerProperties() == null)
                            ? null
                            : new pl.grzeslowski.jsupla.protocol.api.channeltype.value.ActionTrigger(
                                    toSignedInt(channel.actionTriggerCaps())),
                    channel.actionTriggerProperties(),
                    Optional.ofNullable(channel.hvacValue())
                            .map(HvacTypeDecoder.INSTANCE::decode)
                            .orElse(null),
                    channel.hvacValue(),
                    null,
                    null);
        }

        public Channel(SuplaDeviceChannelD channel) {
            this(
                    D,
                    channel.number(),
                    ChannelType.findByValue(channel.type()).orElse(null),
                    channel.type(),
                    Optional.ofNullable(channel.funcList())
                            .map(BitFunction::findByMask)
                            .orElse(Set.of()),
                    channel.funcList(),
                    channel.actionTriggerCaps(),
                    null,
                    null,
                    Optional.of(channel.defaultValue())
                            .flatMap(ChannelFunction::findByValue)
                            .orElse(SUPLA_CHANNELFNC_NONE),
                    channel.defaultValue(),
                    ChannelFlag.findByMask(channel.flags()),
                    channel.flags(),
                    channel.offline(),
                    channel.valueValidityTimeSec(),
                    Optional.ofNullable(channel.value())
                            .filter(c -> c.length > 0)
                            .map(c -> ChannelTypeDecoder.INSTANCE.decode(channel.type(), c))
                            .orElse(null),
                    channel.value(),
                    (channel.actionTriggerCaps() == null || channel.actionTriggerProperties() == null)
                            ? null
                            : new pl.grzeslowski.jsupla.protocol.api.channeltype.value.ActionTrigger(
                                    toSignedInt(channel.actionTriggerCaps())),
                    channel.actionTriggerProperties(),
                    Optional.ofNullable(channel.hvacValue())
                            .map(HvacTypeDecoder.INSTANCE::decode)
                            .orElse(null),
                    channel.hvacValue(),
                    channel.defaultIcon(),
                    null);
        }

        public Channel(SuplaDeviceChannelE channel) {
            this(
                    E,
                    channel.number(),
                    ChannelType.findByValue(channel.type()).orElse(null),
                    channel.type(),
                    Optional.ofNullable(channel.funcList())
                            .map(BitFunction::findByMask)
                            .orElse(Set.of()),
                    channel.funcList(),
                    channel.actionTriggerCaps(),
                    Optional.ofNullable(channel.rGBWFuncList())
                            .map(RgbwBitFunction::findByMask)
                            .orElse(null),
                    channel.rGBWFuncList(),
                    Optional.of(channel.defaultValue())
                            .flatMap(ChannelFunction::findByValue)
                            .orElse(SUPLA_CHANNELFNC_NONE),
                    channel.defaultValue(),
                    ChannelFlag.findByMask(channel.flags()),
                    channel.flags(),
                    channel.offline(),
                    channel.valueValidityTimeSec(),
                    ChannelTypeDecoder.INSTANCE.decode(channel.type(), channel.value()),
                    channel.value(),
                    (channel.actionTriggerCaps() == null || channel.actionTriggerProperties() == null)
                            ? null
                            : new pl.grzeslowski.jsupla.protocol.api.channeltype.value.ActionTrigger(
                                    toSignedInt(channel.actionTriggerCaps())),
                    channel.actionTriggerProperties(),
                    Optional.ofNullable(channel.hvacValue())
                            .map(HvacTypeDecoder.INSTANCE::decode)
                            .orElse(null),
                    channel.hvacValue(),
                    channel.defaultIcon(),
                    channel.subDeviceId());
        }
    }

    @SuppressWarnings("MethodOnlyUsedFromInnerClass")
    private static <InT, OutT> OutT parse(InT in, Function<InT, OutT> f) {
        if (in == null) {
            return null;
        }
        return f.apply(in);
    }
}
