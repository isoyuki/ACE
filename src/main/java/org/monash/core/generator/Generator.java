package org.monash.core.generator;

import com.sun.tools.jconsole.JConsoleContext;
import edu.monash.crypto.primitives.impl.mac.AESCMAC;
import it.unisa.dia.gas.jpbc.Element;
import it.unisa.dia.gas.jpbc.ElementPow;
import it.unisa.dia.gas.jpbc.Field;
import it.unisa.dia.gas.plaf.jpbc.field.z.ZrElement;
import org.apache.log4j.Level;
import org.apache.spark.SparkConf;
import org.apache.spark.api.java.JavaPairRDD;
import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.api.java.JavaSparkContext;
import org.apache.spark.broadcast.Broadcast;
import org.monash.core.util.SecureParam;
import org.monash.crypto.primitives.AsymmetricCipher;
import org.monash.crypto.primitives.Hash;
import org.monash.crypto.primitives.SymmetricCipher;
import org.monash.crypto.primitives.impl.cipher.AESCBC;
import org.monash.crypto.primitives.impl.cipher.RSA;
import org.monash.crypto.primitives.impl.mac.HMACSHA;
import org.monash.crypto.util.ByteArrayKey;
import org.monash.crypto.util.ByteNumConverter;
import org.monash.crypto.util.PairingUtil;
import org.monash.crypto.util.StringByteConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scala.Tuple2;

import java.io.Console;
import java.math.BigInteger;
import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;
import java.util.*;

public class Generator {

    private static final Logger LOGGER = LoggerFactory.getLogger(Generator.class);
    private static Map<ByteArrayKey, Vector<byte[]>> FSet = new HashMap<>(); // rID1 | gST c 1 ·tagw1 /tagID1 , gSTc 2 ·tagw2 /tagID1
    private static Map<byte[], byte[]> ISet = new HashMap<>();

    private static Map<String, Tuple2<byte[], Integer>> W = new HashMap<>(); // key is the keyword, value is the tuple of (ST_c,c)



    public static void main(String[] args) {

        SparkConf conf = new SparkConf().setAppName("Generator");

        // Serialise the element for pre-processing
        conf.set("spark.serializer", "org.apache.spark.serializer.KryoSerializer")
                .set("spark.kryoserializer.buffer.max", "512")
                .registerKryoClasses(new Class[]{Element.class});


        conf.setMaster("local[2]");
        JavaSparkContext sc = new JavaSparkContext(conf);
        sc.setLogLevel(Level.ERROR.toString());

        // Initialise empty maps W[w], FSet and empty dictionary Iset
        // EGDB1 = FSet, EGDB2 = Iset

        Map<byte[], byte[]> EGDB1 = new HashMap<>();


        // Try to load existing group
        Element g;
        try {
            g = PairingUtil.loadGTElementFromFile("elliptical_g");
        } catch (RuntimeException e) {
            // g doesn't not exist
            // Initialise elliptical curve element
            g = PairingUtil.newRandomGTElement();
            // Save the group element on file
            PairingUtil.saveElement(g, "elliptical_g");
        }

        // Create preprocessed element
        ElementPow preG = g.getElementPowPreProcessing();
        // Create broadcast variables
        Broadcast<ElementPow> broadcastPow = sc.broadcast(preG);


        // File path is passed as argument
        if(args.length > 0) {

            // Generate inverted Index

            // id1 key1,key2,key3
            // id2 key1,key3

            // to

            // key1 id1,id2
            // key2 id1
            // key3 id1,id2

            JavaPairRDD<String, String> invertedIndex = sc.textFile(args[0])
                    .mapToPair(line -> {
                        String[] split = line.split(" ");
                        String id = split[0];
                        String[] keywords = split[1].split(",");
                        return new Tuple2<>(id, keywords);
                    })
                    .flatMapToPair(tuple -> {
                        List<Tuple2<String, String>> list = new ArrayList<>();
                        for (String keyword : tuple._2) {
                            list.add(new Tuple2<>(keyword, tuple._1));
                        }
                        return list.iterator();
                    })
                    .reduceByKey((v1, v2) -> v1 + "," + v2);

            LOGGER.debug("Inverted index is generated.");



            invertedIndex.foreachPartition(entries -> {

                Hash cmac = new AESCMAC();
                Hash hmac = new HMACSHA();
                AsymmetricCipher rsa = new RSA();
                SymmetricCipher aescbc = new AESCBC();

                entries.forEachRemaining(entry -> { // for each w

//                    System.out.println("Processing keyword: " + entry._1);

                    String keyword = entry._1;
                    String[] ids = entry._2.split(",");

                    byte[] tag_w = hmac.encode(keyword.getBytes(), SecureParam.K_T);
                    byte[] k_w = cmac.encode(keyword.getBytes(), SecureParam.K_S);

                    // Find w in W

                    int c;
                    byte[] ST;

                    if(!W.containsKey(keyword)) {
                        c = 0;
                        ST = rsa.encrypt(SecureParam.sample_array);
                    }else{
                        ST = W.get(keyword)._1;
                        c = W.get(keyword)._2;
                    }

                    for (String id : ids) { // for IDi ∈ GDB(w) do

                        byte[] r = cmac.encode(id.getBytes(), SecureParam.K_1);
                        byte[] tag_id;
                        // if there is no index r_ID in FSet for IDi then
                        if (!FSet.containsKey(r)) {
                            tag_id = hmac.encode(id.getBytes(), SecureParam.K_2);
                        }
                        tag_id = hmac.encode(id.getBytes(), SecureParam.K_2);

//                        tag_id = hmac.encode(id.getBytes(), SecureParam.K_2);

                        byte[] enc_id  = aescbc.encrypt(id.getBytes(), k_w);

                        c++;

                        System.out.println("c: " + c);

                        ST = rsa.encrypt(ST);

                        byte[] group = broadcastPow.getValue()
                                .powZn(PairingUtil.getZrElementForHash(ST)
                                        .mul(PairingUtil.getZrElementForHash(tag_w)))
                                .getImmutable()
                                .toBytes();

                        byte[] l = hmac.encode(group, SecureParam.K_h);

//                        System.out.println(StringByteConverter.byteToHex(enc_id));\
                        // Append enc_id to ISet[l]
                        if(!ISet.containsKey(l)) {
                            ISet.put(l, enc_id);
                        }

                        // delta = g ^ ST-c * tag_w / tag_id

                        byte[] delta = broadcastPow.getValue()
                                .powZn(PairingUtil.getZrElementForHash(ST)
                                        .mul(PairingUtil.getZrElementForHash(tag_w))
                                        .div(PairingUtil.getZrElementForHash(tag_id)))
                                .getImmutable()
                                .toBytes();

                        // Append delta into FSet[r_ID]

                        ByteArrayKey byteArrayKey = new ByteArrayKey(r);

                        if(!FSet.containsKey(byteArrayKey)) {
                            Vector<byte[]> keywords = new Vector<>();
                            keywords.add(delta);
                            FSet.put(byteArrayKey, keywords);
                        }else{
                            FSet.get(byteArrayKey).add(delta);
                        }


                        // --- Verify

                        // tk <- g ^ tag_w

                        // l <- H(tk^ST, k_h)

                        Element tk = broadcastPow.getValue()
                                .powZn(PairingUtil.getZrElementForHash(tag_w))
                                .getImmutable();

                        byte[] tk_prime = tk.powZn(PairingUtil.getZrElementForHash(ST))
                                .getImmutable()
                                .toBytes();
//
                        byte[] l2 = hmac.encode(tk_prime, SecureParam.K_h);

                    }

                    // Update W
                    // W[w] = (ST, c)
                    W.put(keyword, new Tuple2<>(ST, c));
                });
            });

        } else{
            LOGGER.info("No file path provided");
        }

        // Print W

        for (Map.Entry<String, Tuple2<byte[], Integer>> entry : W.entrySet()) {
            System.out.println(entry.getKey() + " " + StringByteConverter.byteToHex(entry.getValue()._1) + " " + entry.getValue()._2);
        }

    }
}