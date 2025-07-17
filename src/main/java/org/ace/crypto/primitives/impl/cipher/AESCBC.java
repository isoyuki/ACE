package org.ace.crypto.primitives.impl.cipher;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.ace.crypto.primitives.SymmetricCipher;
import org.ace.crypto.util.StringByteConverter;

import javax.crypto.*;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.*;


public class AESCBC implements SymmetricCipher {

    static {
        Security.addProvider(new BouncyCastleProvider());
    }

    private byte[] N = StringByteConverter.hexToByte("62EC67F9C3A4A407FCB2A8C49031A8B3");

    public void setN(byte[] n) {
        this.N = n;
    }

//    @Override
    public byte[] encrypt(byte[] content, byte[] password) {
        try {
            SecureRandom random = SecureRandom.getInstance("SHA1PRNG");
            random.setSeed(password);
            KeyGenerator keyGenerator = KeyGenerator.getInstance("AES");
            keyGenerator.init(128, random);
            SecretKey secretKey = keyGenerator.generateKey();
            byte[] enCodeFormat = secretKey.getEncoded();
            SecretKeySpec key = new SecretKeySpec(enCodeFormat, "AES");
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5PADDING", "BC");
            cipher.init(Cipher.ENCRYPT_MODE, key, new IvParameterSpec(N));
            byte[] enc = cipher.doFinal(content);

            return enc;

        } catch (BadPaddingException
                 | IllegalBlockSizeException
                 | InvalidAlgorithmParameterException
                 | InvalidKeyException
                 | NoSuchAlgorithmException
                 | NoSuchPaddingException
                 | NoSuchProviderException e) {
            throw new RuntimeException(e);
        }
    }

//    @Override
    public byte[] decrypt(byte[] content, byte[] password) {
        try {

            KeyGenerator keyGenerator = KeyGenerator.getInstance("AES");

            SecureRandom random = SecureRandom.getInstance("SHA1PRNG");
            random.setSeed(password);
            keyGenerator.init(128, random);
            SecretKey secretKey = keyGenerator.generateKey();
            byte[] enCodeFormat = secretKey.getEncoded();
            SecretKeySpec key = new SecretKeySpec(enCodeFormat, "AES");
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5PADDING", "BC");
            cipher.init(Cipher.DECRYPT_MODE, key, new IvParameterSpec(N));
            byte[] dec = cipher.doFinal(content);

            return dec;
        } catch (BadPaddingException
                 | IllegalBlockSizeException
                 | InvalidAlgorithmParameterException
                 | InvalidKeyException
                 | NoSuchAlgorithmException
                 | NoSuchPaddingException
                 | NoSuchProviderException e) {
            throw new RuntimeException(e);
        }}
}

