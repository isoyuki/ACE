package org.ace.crypto.util;

import java.util.Arrays;

public class ByteArrayKey {
    private final byte[] data;

    public ByteArrayKey(byte[] data) {
        this.data = data;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ByteArrayKey that = (ByteArrayKey) o;
        return Arrays.equals(data, that.data);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(data);
    }

    public byte[] getData() {
        return data;
    }
}