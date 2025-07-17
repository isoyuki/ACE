package org.monash.crypto.primitives.impl.blake;
import org.bouncycastle.crypto.digests.Blake2bDigest;
import org.bouncycastle.jcajce.provider.digest.Blake2b;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.monash.crypto.primitives.Hash;

import java.security.SecureRandom;
import java.security.Security;

public class BLAKE2B implements Hash {

    static {
        Security.addProvider(new BouncyCastleProvider());
    }

    public byte[] encode(byte[] content, byte[] password) {
        try {
            SecureRandom random = SecureRandom.getInstance("SHA1PRNG");
            random.setSeed(password);
            Blake2bDigest blake2b = new Blake2bDigest(password);
            blake2b.getAlgorithmName();
            blake2b.update(content, 0, content.length);
            byte[] result = new byte[blake2b.getDigestSize()];
            blake2b.doFinal(result, 0);
            return result;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public byte[] encode(byte[] content) {
        throw new RuntimeException("This implementation of Blake2 requires a key");
    }

}
