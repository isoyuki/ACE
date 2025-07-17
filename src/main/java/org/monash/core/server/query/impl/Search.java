package org.monash.core.server.query.impl;

import it.unisa.dia.gas.jpbc.Element;
import it.unisa.dia.gas.jpbc.ElementPow;
import org.apache.spark.broadcast.Broadcast;
import org.monash.core.dao.DataSource;
import org.monash.core.dao.impl.RedisDataSource;
import org.monash.core.server.query.Query;
import org.monash.core.util.SecureParam;
import org.monash.crypto.primitives.AsymmetricCipher;
import org.monash.crypto.primitives.Hash;
import org.monash.crypto.primitives.impl.cipher.RSA;
import org.monash.crypto.primitives.impl.mac.HMACSHA;
import org.monash.crypto.util.PairingUtil;
import org.monash.crypto.util.StringByteConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scala.Tuple2;

import java.io.*;
import java.util.*;

public class Search implements Query {

    private static final Logger LOGGER = LoggerFactory.getLogger("Server Search");
    private static final Properties properties = new Properties();
    private static final String buildDir = "target/";

    private static final Map<String, Tuple2<byte[], Integer>> W = new HashMap<>(); // key is the keyword, value is the tuple of (ST_c,c)

    static{

        try{
            properties.load(new FileInputStream(buildDir+ "config.properties"));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static Element tk;
    private static byte[] ST_c;
    private static int c;

    private static ArrayList<byte[]> RSet = new ArrayList<>();

    @Override
    public void execute() {

        System.out.println("Search");

        // Check if tk, ST_c, c are defined

        if (tk == null || ST_c == null || c == 0) {
            LOGGER.error("tk, ST_c, c are not defined");
        }else{

            // For i = c to 1

            Hash hmac = new HMACSHA();
            AsymmetricCipher RSA = new RSA();
            DataSource redis = new RedisDataSource();

            for (int i = c; i > 0; i--) {

                // Compute l

                byte[] tk_ST = tk.powZn(PairingUtil.getZrElementForHash(ST_c))
                        .getImmutable()
                        .toBytes();

                byte[] l = hmac.encode(tk_ST, SecureParam.K_h);


                // Find in ISet

                byte[] enc_id = redis.hget("ISet".getBytes(), l);

                if (enc_id != null){
                    RSet.add(enc_id);

                    System.out.println("enc_id: " + StringByteConverter.byteToHex(enc_id));

                }

                ST_c = RSA.encrypt(ST_c);
            }

        }

    }

    @Override
    public int getResultSize() {
        return 0;
    }

    @Override
    public List<byte[]> getResultList() {
        return null;
    }

    public void setTk(Element tk){
        Search.tk = tk;
    }

    public void setST_c(byte[] ST_c){
        Search.ST_c = ST_c;
    }

    public void setC(int c){
        Search.c = c;
    }
}
