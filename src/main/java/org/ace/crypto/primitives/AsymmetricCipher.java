package org.ace.crypto.primitives;

public interface AsymmetricCipher {
    byte[] encrypt(byte[] content, byte[] password);
    byte[] decrypt(byte[] content, byte[] password);

    byte[] encrypt(byte[] content);

    byte[] decrypt(byte[] content);
}