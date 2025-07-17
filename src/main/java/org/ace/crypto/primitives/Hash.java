package org.ace.crypto.primitives;

public interface Hash {
    byte[] encode(byte[] content, byte[] password);

    byte[] encode(byte[] content);
}
