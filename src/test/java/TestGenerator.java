import org.junit.Test;
import org.monash.crypto.primitives.AsymmetricCipher;
import org.monash.crypto.primitives.impl.cipher.RSA;

import java.util.Arrays;

public class TestGenerator {

    @Test
    public void testRSACipher() {
        byte[] content = "Hello World".getBytes();
        byte[] password = "password".getBytes();

        AsymmetricCipher rsa = new RSA();

        byte[] encrypted = rsa.encrypt(content, password);
        byte[] decrypted = rsa.decrypt(encrypted, password);

        AsymmetricCipher rsa2 = new RSA();

        byte[] decrypted2 = rsa2.decrypt(encrypted, password);

        assert Arrays.equals(content, decrypted);
        assert Arrays.equals(content, decrypted2);
    }
}
