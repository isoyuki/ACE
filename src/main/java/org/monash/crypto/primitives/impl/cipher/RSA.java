package org.monash.crypto.primitives.impl.cipher;

import org.bouncycastle.crypto.AsymmetricCipherKeyPair;
import org.bouncycastle.crypto.generators.RSAKeyPairGenerator;
import org.bouncycastle.crypto.params.RSAKeyGenerationParameters;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.monash.crypto.primitives.AsymmetricCipher;
import org.monash.crypto.primitives.SymmetricCipher;

import javax.crypto.*;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigInteger;
import java.security.*;

public class RSA implements AsymmetricCipher {

    protected static final String ALGORITHM = "RSA";
    protected static final int KEY_SIZE = 2048;

    static {
        Security.addProvider(new BouncyCastleProvider());
    }

    public static String getHexString(byte[] b) {
        StringBuilder result = new StringBuilder();
        for (byte value : b) {
            result.append(Integer.toString((value & 0xff) + 0x100, 16)
                    .substring(1));
        }
        return result.toString();
    }
    public static byte[] hexStringToByteArray(String s) {
        int len = s.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4) + Character
                    .digit(s.charAt(i + 1), 16));
        }
        return data;
    }

    /**
     * Generate RSA Key pair
     * @return key pair
     */
    public static KeyPair generateKey() throws NoSuchAlgorithmException
    {
        KeyPairGenerator keyGen = KeyPairGenerator.getInstance(ALGORITHM);
        keyGen.initialize(KEY_SIZE);
        return keyGen.generateKeyPair();
    }

    public byte[] encrypt(byte[] content, byte[] password) {
        try{
            KeyPairGenerator keyGen = KeyPairGenerator.getInstance(ALGORITHM);
            SecureRandom random = SecureRandom.getInstance("SHA1PRNG");
            random.setSeed(password);
            keyGen.initialize(KEY_SIZE, random);
            KeyPair sk = keyGen.generateKeyPair();
            Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
            cipher.init(Cipher.ENCRYPT_MODE, sk.getPrivate());
            return cipher.doFinal(content);
        } catch (BadPaddingException
                 | IllegalBlockSizeException
                 | InvalidKeyException
                 | NoSuchAlgorithmException
                 | NoSuchPaddingException e) {
            throw new RuntimeException(e);
        }
    }

    public byte[] decrypt(byte[] content, byte[] password) {
        try{
            KeyPairGenerator keyGen = KeyPairGenerator.getInstance(ALGORITHM);
            SecureRandom random = SecureRandom.getInstance("SHA1PRNG");
            random.setSeed(password);
            keyGen.initialize(KEY_SIZE, random);
            KeyPair sk = keyGen.generateKeyPair();
            Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
            cipher.init(Cipher.DECRYPT_MODE, sk.getPublic());
            return cipher.doFinal(content);
        } catch (BadPaddingException
                | IllegalBlockSizeException
                | InvalidKeyException
                | NoSuchAlgorithmException
                | NoSuchPaddingException e) {
            throw new RuntimeException(e);
        }
    }
}

