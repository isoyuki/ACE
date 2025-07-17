package org.ace.crypto.util;

import org.ace.util.FolderUtil;
import it.unisa.dia.gas.jpbc.Element;
import it.unisa.dia.gas.jpbc.Field;
import it.unisa.dia.gas.jpbc.Pairing;
import it.unisa.dia.gas.plaf.jpbc.pairing.PairingFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.math.BigInteger;

public final class PairingUtil {

    // Logger Class
    private static final Logger LOGGER = LoggerFactory.getLogger(PairingUtil.class);
    // Load pairing information
    private static final Pairing pairing = PairingFactory.getPairing("params/curves/a.properties");
    private static final Field gt = pairing.getGT();
    private static final Field zr = pairing.getZr();

    public static Element newRandomGTElement() {
        return gt.newRandomElement()
                .getImmutable();
    }

    public static Element newRandomZrElement() {
        return zr.newRandomElement()
                .getImmutable();
    }

    public static Element ZeroZrElement() {
        return zr.newZeroElement()
                .getImmutable();
    }

    public static Element OneZrElement() {
        return zr.newOneElement()
                .getImmutable();
    }

    public static Element ZeroGTElement() {
        return gt.newZeroElement()
                .getImmutable();
    }

    public static Element OneGTElement() {
        return gt.newOneElement()
                .getImmutable();
    }

    

    public static Element getZrElementFromBigInt(BigInteger src) {
        return zr.newElement(src).getImmutable();
    }

    public static Element getZrElementForHash(byte[] src) {
        return zr.newElementFromHash(src, 0, src.length).getImmutable();
    }

    public static Element getZrElementFromByte(byte[] src) {
        return zr.newElementFromBytes(src).getImmutable();
    }

    public static Element getGTElementForHash(byte[] src) {
        return gt.newElementFromHash(src, 0, src.length).getImmutable();
    }

    public static Element getGTElementFromByte(byte[] src) {
        return gt.newElementFromBytes(src).getImmutable();
    }

    public static Element loadZrElementFromFile(String fileName) {
        // only return if the file size is correct
        return getZrElementFromByte(loadElementFromFile(fileName));
    }

    public static Element loadGTElementFromFile(String fileName) {
        // only return if the file size is correct
        return getGTElementFromByte(loadElementFromFile(fileName));
    }

    private static byte[] loadElementFromFile(String fileName) {
        try {
            // load the pairing element from given file
            File gFile = new File(fileName);
            FileInputStream loadStream = new FileInputStream(gFile);
            int size = loadStream.read();
            byte[] gInBytes = new byte[size];
            int readSize = loadStream.read(gInBytes);
            if(readSize != size) {
                throw new IOException("Element file is corrupted");
            }
            loadStream.close();
            // only return if the file size is correct
            return gInBytes;
        } catch (IOException e) {
            // Fail to read the element
            LOGGER.error("Unable to read pairing element from file");
            throw new RuntimeException(e);
        }
    }

    public static void saveElement(Element g, String fileName) {

        if(g == null) { // ignore the null pointer
            LOGGER.error("Null element is provided");
            throw new NullPointerException("The input is a null pointer");
        }

        try {
            FileOutputStream saveStream = new FileOutputStream(new File(FolderUtil.getUserDir(), fileName));
            saveStream.write(g.getLengthInBytes());
            saveStream.write(g.toBytes());
            saveStream.close();
        } catch (IOException e){
            // Failed to save the element
            LOGGER.error("Unable to save the pairing element to file");
            throw new RuntimeException(e);
        }
    }
}
