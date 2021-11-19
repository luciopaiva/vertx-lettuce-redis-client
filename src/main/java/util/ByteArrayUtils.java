package util;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

public class ByteArrayUtils {

    public static byte[] strToBytes(String str) {
        return str.getBytes(StandardCharsets.UTF_8);
    }

    public static String bytesToStr(byte[] bytes) {
        return new String(bytes, StandardCharsets.UTF_8);
    }

    public static byte[] intToBytes(int value) {
        return ByteBuffer.allocate(Integer.BYTES).putInt(value).array();
    }

    public static int bytesToInt(byte[] bytes) {
        return ByteBuffer.wrap(bytes).getInt();
    }
}
