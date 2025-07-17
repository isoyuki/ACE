package org.monash;

import org.monash.crypto.primitives.impl.cipher.RSA;

import java.util.Arrays;

public class Main {
    public static void main(String[] args) {

        RSA rsa = new RSA();

        // Construct byte array with 16 bytes
        byte[] content = new byte[16];
        for (int i = 0; i < content.length; i++) {
            content[i] = (byte) i;
        }

        byte[] pass = new byte[16];

        try{
            byte[] encrypted = rsa.encrypt(content, pass);
            byte[] decrypted = rsa.decrypt(encrypted, pass);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}