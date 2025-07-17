package org.monash.crypto.primitives.impl.cipher;

import org.bouncycastle.crypto.AsymmetricBlockCipher;
import org.bouncycastle.crypto.AsymmetricCipherKeyPair;
import org.bouncycastle.crypto.InvalidCipherTextException;
import org.bouncycastle.crypto.engines.RSAEngine;
import org.bouncycastle.crypto.generators.RSAKeyPairGenerator;
import org.bouncycastle.crypto.params.AsymmetricKeyParameter;
import org.bouncycastle.crypto.params.RSAKeyGenerationParameters;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.monash.core.util.SecureParam;
import org.monash.crypto.primitives.AsymmetricCipher;

import javax.crypto.*;
import java.math.BigInteger;
import java.security.*;

public class RSA implements AsymmetricCipher {

    protected static final String ALGORITHM = "RSA";
    protected static final int KEY_SIZE = 2048;

    protected static KeyPair keyPair;

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
        keyPair = keyGen.generateKeyPair();
        return keyGen.generateKeyPair();
    }

    public static void generateSample() {
        try {
            KeyPair keyPair = generateKey();
            System.out.println("Public Key: " + getHexString(keyPair.getPublic().getEncoded()));
            System.out.println("Private Key: " + getHexString(keyPair.getPrivate().getEncoded()));

            String plainText = "Hello World";

            RSA rsa = new RSA();

            // Encrypt the string using the public key
            byte[] cipherText = rsa.encrypt(plainText.getBytes(), keyPair.getPublic().getEncoded());

            // Decrypt the cipher text using the private key.
            byte[] newPlainText = rsa.decrypt(cipherText, keyPair.getPrivate().getEncoded());
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
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

    @Override
    public byte[] encrypt(byte[] content) {
        try{
//            KeyPairGenerator keyGen = KeyPairGenerator.getInstance(ALGORITHM);
//            SecureRandom random = SecureRandom.getInstance("SHA1PRNG");
//            keyGen.initialize(KEY_SIZE, random);
//            KeyPair sk = keyGen.generateKeyPair();
//            Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
//            cipher.init(Cipher.ENCRYPT_MODE, sk.getPrivate());
//            return cipher.doFinal(content);

            AsymmetricCipherKeyPair keyPair = GenerateKeys();
            Security.addProvider(new BouncyCastleProvider());

            RSAEngine engine = new RSAEngine();
            engine.init(true, keyPair.getPublic()); //true if encrypt

            // Output encryption result as BigInteger

            return engine.processBlock(content, 0, content.length);

        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public byte[] decrypt(byte[] content) {
        try{
            KeyPairGenerator keyGen = KeyPairGenerator.getInstance(ALGORITHM);
            SecureRandom random = SecureRandom.getInstance("SHA1PRNG");
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

    public static AsymmetricCipherKeyPair GenerateKeys() throws NoSuchAlgorithmException{
        RSAKeyPairGenerator generator = new RSAKeyPairGenerator();

        SecureRandom random = SecureRandom.getInstance("SHA1PRNG");
        random.setSeed(SecureParam.rsa_seed);

        generator.init(new RSAKeyGenerationParameters
                (
                        new BigInteger("10001", 16),
                        random,
                        KEY_SIZE,//strength
                        80//certainty
                ));

        return generator.generateKeyPair();
    }

    public static String Encrypt(byte[] data, AsymmetricKeyParameter publicKey) throws Exception{
        Security.addProvider(new BouncyCastleProvider());

        RSAEngine engine = new RSAEngine();
        engine.init(true, publicKey); //true if encrypt

        byte[] hexEncodedCipher = engine.processBlock(data, 0, data.length);

        return getHexString(hexEncodedCipher);
    }

    public static String Decrypt(String encrypted, AsymmetricKeyParameter privateKey) throws InvalidCipherTextException {

        Security.addProvider(new BouncyCastleProvider());

        AsymmetricBlockCipher engine = new RSAEngine();
        engine.init(false, privateKey); //false for decryption

        byte[] encryptedBytes = hexStringToByteArray(encrypted);
        byte[] hexEncodedCipher = engine.processBlock(encryptedBytes, 0, encryptedBytes.length);

        return new String (hexEncodedCipher);
    }
}

