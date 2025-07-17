package edu.monash.crypto.primitives.impl.mac;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.monash.crypto.primitives.Hash;

import javax.crypto.KeyGenerator;
import javax.crypto.Mac;
import javax.crypto.SecretKey;
import java.security.*;

public final class AESCMAC implements Hash {

    static {
        Security.addProvider(new BouncyCastleProvider());
    }

    public byte[] encode(byte[] content, byte[] password) {
        try {
            Mac mac = Mac.getInstance("AESCMAC", "BC");
            KeyGenerator keyGenerator = KeyGenerator.getInstance("AES");
            SecureRandom random = SecureRandom.getInstance("SHA1PRNG");
            random.setSeed(password);
            keyGenerator.init(128, random);
            SecretKey secretKey = keyGenerator.generateKey();
            mac.init(secretKey);
            mac.update(content, 0, content.length);
            return mac.doFinal();
        } catch (InvalidKeyException
                 | NoSuchAlgorithmException
                 | NoSuchProviderException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public byte[] encode(byte[] content) {
        throw new RuntimeException("AES-CMAC requires a key");
    }
}
