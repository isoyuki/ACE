import org.junit.Test;
import org.monash.crypto.primitives.AsymmetricCipher;
import org.monash.crypto.primitives.Hash;
import org.monash.crypto.primitives.impl.cipher.RSA;
import org.monash.crypto.primitives.impl.mac.HMACSHA;
import org.monash.crypto.util.StringByteConverter;

import java.util.Arrays;

public class TestGenerator {

    @Test
    public void testRSACipher() {
        byte[] content = "Hello World".getBytes();
        byte[] password = "password".getBytes();

        AsymmetricCipher rsa = new RSA();

        byte[] encrypted = rsa.encrypt(content);
        byte[] decrypted = rsa.decrypt(encrypted);

        AsymmetricCipher rsa2 = new RSA();

        byte[] decrypted2 = rsa2.decrypt(encrypted);

        assert Arrays.equals(content, decrypted);
        assert Arrays.equals(content, decrypted2);
    }

    @Test
    public void testHMACSHA() {
        byte[] content = "The quick brown fox jumps over the lazy dog".getBytes();
        byte[] password = "key".getBytes();

        String answer = "b42af09057bac1e2d41708e48a902e09b5ff7f12ab428a4fe86653c73dd248fb82f948a549f7b791a5b41915ee4d1ec3935357e4e2317250d0372afa2ebeeb3a";

        Hash hash = new HMACSHA();
        byte[] hashed = hash.encode(content, password);

        String hexHash = StringByteConverter.byteToHex(hashed).toLowerCase();

        assert hexHash.equals(answer);
    }
}
