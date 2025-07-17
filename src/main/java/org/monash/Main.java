package org.monash;

import org.monash.crypto.primitives.Hash;
import org.monash.crypto.primitives.impl.cipher.RSA;
import org.monash.crypto.primitives.impl.mac.HMACSHA;

import java.util.Arrays;

public class Main {
    public static void main(String[] args) {

//        RSA rsa = new RSA();
//
//        // Construct byte array with 16 bytes
//        byte[] content = new byte[16];
//        for (int i = 0; i < content.length; i++) {
//            content[i] = (byte) i;
//        }
//
//        byte[] pass = new byte[16];
//
//        try{
//            byte[] encrypted = rsa.encrypt(content, pass);
//            byte[] decrypted = rsa.decrypt(encrypted, pass);
//        } catch (Exception e) {
//            e.printStackTrace();
//        }

        Hash hash = new HMACSHA();
        byte[] content = "Hello World".getBytes();
        byte[] pass = "password".getBytes();

        byte[] hashed = hash.encode(content, pass);

        // Print result has hex
        for (byte b : hashed) {
            System.out.printf("%02x", b);
        }
        System.out.println();

    }
}