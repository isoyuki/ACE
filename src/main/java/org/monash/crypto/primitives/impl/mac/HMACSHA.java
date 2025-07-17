package org.monash.crypto.primitives.impl.mac;

import org.bouncycastle.crypto.macs.HMac;
import org.bouncycastle.crypto.params.KeyParameter;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.jce.provider.JCEKeyGenerator;
import org.monash.crypto.primitives.Hash;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.security.SecureRandom;
import java.security.Security;

public class HMACSHA implements Hash {

    protected static final String ALGORITHM = "SHA512";

    static {
        Security.addProvider(new BouncyCastleProvider());
    }

    public byte[] encode(byte[] content, byte[] password) {
        try{
            SecureRandom random = SecureRandom.getInstance("SHA1PRNG");
            random.setSeed(password);

            HMac mac = new HMac(new org.bouncycastle.crypto.digests.SHA512Digest());
            mac.init(new KeyParameter(password));
            mac.update(content, 0, content.length);
            byte[] result = new byte[mac.getMacSize()];
            mac.doFinal(result, 0);
            return result;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public byte[] encode(byte[] content) {
        throw new RuntimeException("HMAC-SHA requires a key");
    }
}
