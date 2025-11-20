package pl.grzeslowski.openhab.supla.internal.server;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class ByteArrayToHexTest {
    @Test
    void shouldConvertBytesToHexAndBack() {
        byte[] input = new byte[] {0x00, 0x0A, (byte) 0xFF};

        String hex = ByteArrayToHex.bytesToHex(input);
        byte[] result = ByteArrayToHex.hexToBytes(hex);

        assertThat(hex).isEqualTo("000AFF");
        assertThat(result).containsExactly(input);
    }

    @Test
    void shouldHandleEmptyArray() {
        String hex = ByteArrayToHex.bytesToHex(new byte[0]);
        byte[] result = ByteArrayToHex.hexToBytes(hex);

        assertThat(hex).isEmpty();
        assertThat(result).isEmpty();
    }
}
