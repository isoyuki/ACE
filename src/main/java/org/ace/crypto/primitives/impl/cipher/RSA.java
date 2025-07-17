package org.ace.crypto.primitives.impl.cipher;

import org.bouncycastle.crypto.AsymmetricCipherKeyPair;
import org.bouncycastle.crypto.engines.RSAEngine;
import org.bouncycastle.crypto.generators.RSAKeyPairGenerator;
import org.bouncycastle.crypto.params.RSAKeyGenerationParameters;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.ace.core.util.SecureParam;
import org.ace.crypto.primitives.AsymmetricCipher;

import javax.crypto.*;
import java.math.BigInteger;
import java.security.*;

public class RSA implements AsymmetricCipher {

    protected static final String ALGORITHM = "RSA";
    protected static final int KEY_SIZE = 2048;

    protected static AsymmetricCipherKeyPair keyPair;

    static {
        Security.addProvider(new BouncyCastleProvider());

        try {
            keyPair = GenerateKeys();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
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

    public byte[] encrypt(byte[] content, byte[] password) {
        try{

            KeyPairGenerator keyGen = KeyPairGenerator.getInstance(ALGORITHM);
            SecureRandom random = SecureRandom.getInstance("SHA1PRNG");
            random.setSeed(password);
            keyGen.initialize(KEY_SIZE, random);
            KeyPair sk = keyGen.generateKeyPair();
            Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
            cipher.init(Cipher.ENCRYPT_MODE, sk.getPrivate());

            byte[] cipherText = cipher.doFinal(content);

            return cipherText;
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

            byte[] plainText = cipher.doFinal(content);

            return plainText;
        } catch (BadPaddingException
                | IllegalBlockSizeException
                | InvalidKeyException
                | NoSuchAlgorithmException
                | NoSuchPaddingException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public byte[] encrypt(byte[] content) {

        RSAEngine engine = new RSAEngine();
        engine.init(true, keyPair.getPublic()); //true if encrypt

        byte[] cipherText = engine.processBlock(content, 0, content.length);

        return cipherText;
    }

    @Override
    public byte[] decrypt(byte[] content) {

        RSAEngine engine = new RSAEngine();
        engine.init(false, keyPair.getPrivate()); //true if encrypt

        byte[] plainText = engine.processBlock(content, 0, content.length);
        return plainText;

    }

    public static AsymmetricCipherKeyPair GenerateKeys() throws NoSuchAlgorithmException{
        RSAKeyPairGenerator generator = new RSAKeyPairGenerator();

        SecureRandom random = SecureRandom.getInstance("SHA1PRNG");
        random.setSeed(SecureParam.rsa_seed);

        generator.init(new RSAKeyGenerationParameters
                (
//                        new BigInteger("10001", 16),
                        new BigInteger("17"),
                        random,
                        KEY_SIZE,//strength
                        80//certainty
                ));

        return generator.generateKeyPair();
    }
}

