package org.ace.crypto.util;

import java.math.BigInteger;

public class ByteNumConverter {

    public static byte[] intToBytes(long value) {
        return BigInteger.valueOf(value).toByteArray();
    }

    public static long bytesToInt(byte[] bytes) {
        return new BigInteger(bytes).longValue();
    }
}
