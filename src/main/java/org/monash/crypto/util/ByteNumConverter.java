package org.monash.crypto.util;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.Conversion;

import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.util.Arrays;

public class ByteNumConverter {

    public static byte[] intToBytes(long value) {
        return BigInteger.valueOf(value).toByteArray();
    }

    public static long bytesToInt(byte[] bytes) {
        return new BigInteger(bytes).longValue();
    }
}
