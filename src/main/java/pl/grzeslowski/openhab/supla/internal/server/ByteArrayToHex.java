package pl.grzeslowski.openhab.supla.internal.server;

public interface ByteArrayToHex {
    static String bytesToHex(byte[] bytes) {
        StringBuilder hexString = new StringBuilder(2 * bytes.length);
        for (byte b : bytes) {
            String hex = String.format("%02X", b); // Convert byte to hex, ensuring two digits (e.g., "0A", "FF")
            hexString.append(hex);
        }
        return hexString.toString();
    }

    @SuppressWarnings("StaticMethodOnlyUsedInOneClass")
    static byte[] hexToBytes(String hex) {
        int len = hex.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(hex.charAt(i), 16) << 4) + Character.digit(hex.charAt(i + 1), 16));
        }
        return data;
    }
}
