package org.ace.util;

import java.io.*;
import java.util.ArrayList;

public class DataTypeConverter {

    public static byte[] ArrayListToByte(ArrayList<byte[]> list) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try{
            ObjectOutputStream oos = new ObjectOutputStream(baos);
            oos.writeObject(list);
            oos.close();
            return baos.toByteArray();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static ArrayList<byte[]> ByteToArrayList(byte[] bytes) {

        ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
        try{
            ObjectInputStream ois = new ObjectInputStream(bais);
            ois.close();
            return (ArrayList<byte[]>) ois.readObject();
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }

        return null;
    }



}
