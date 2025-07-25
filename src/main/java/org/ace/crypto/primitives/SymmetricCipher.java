package org.ace.crypto.primitives;

public interface SymmetricCipher {
    byte[] encrypt(byte[] content, byte[] password);
    byte[] decrypt(byte[] content, byte[] password);
}
