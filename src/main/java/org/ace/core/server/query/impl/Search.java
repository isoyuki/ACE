package org.ace.core.server.query.impl;

import it.unisa.dia.gas.jpbc.Element;
import org.ace.core.dao.DataSource;
import org.ace.core.dao.impl.RedisDataSource;
import org.ace.core.server.query.Query;
import org.ace.core.util.SecureParam;
import org.ace.crypto.primitives.AsymmetricCipher;
import org.ace.crypto.primitives.Hash;
import org.ace.crypto.primitives.impl.cipher.RSA;
import org.ace.crypto.primitives.impl.mac.HMACSHA;
import org.ace.crypto.util.PairingUtil;
import org.ace.crypto.util.StringByteConverter;
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

    private static final ArrayList<byte[]> RSet = new ArrayList<>();

    @Override
    public void execute() {

        LOGGER.debug("Start Search");

        // Check if tk, ST_c, c are defined

        if (tk == null || ST_c == null) {
            LOGGER.error("tk, ST_c, c are not defined");
        }else{

            Hash hmac = new HMACSHA();
            AsymmetricCipher RSA = new RSA();
            DataSource redis = new RedisDataSource();

            for (int i = c; i > 0; i--) {

                byte[] tk_ST = tk.powZn(PairingUtil.getZrElementForHash(ST_c))
                        .getImmutable()
                        .toBytes();

                byte[] l = hmac.encode(tk_ST, SecureParam.K_h);

                // Find in ISet
                byte[] enc_id = redis.hget("ISet".getBytes(), l);

                if (enc_id != null){
                    LOGGER.debug("Found in ISet: " + StringByteConverter.byteToHex(enc_id));
                    RSet.add(enc_id);
                } else {
//                    System.out.println("Not found in ISet: " + StringByteConverter.byteToHex(ST_c));
                }

                ST_c = RSA.encrypt(ST_c);
            }

            redis.close();
        }
    }

    @Override
    public int getResultSize() {
        return RSet.size();
    }

    @Override
    public ArrayList<byte[]> getResultList() {
        return RSet;
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
