package org.monash.crypto.util;

import java.util.concurrent.atomic.AtomicReferenceArray;

public class AtomicByteArray {

    private final AtomicReferenceArray<Byte> array;

    public AtomicByteArray(byte[] initialArray) {
        this.array = new AtomicReferenceArray<>(initialArray.length);

        // Initialize the array with the values from the initialArray
        for (int i = 0; i < initialArray.length; i++) {
            array.set(i, initialArray[i]);
        }
    }

    public int length() {
        return array.length();
    }

    public byte get(int index) {
        return array.get(index);
    }

    public void set(int index, byte value) {
        array.set(index, value);
    }
}
