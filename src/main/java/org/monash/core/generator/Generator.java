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
import org.monash.core.dao.DataSource;
import org.monash.core.dao.impl.RedisDataSource;
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
import org.monash.util.DataTypeConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scala.Tuple2;

import javax.xml.crypto.Data;
import java.io.*;
import java.math.BigInteger;
import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;
import java.util.*;

public class Generator {

    private static final Logger LOGGER = LoggerFactory.getLogger(Generator.class);

    static SparkConf conf;
    static JavaSparkContext sc;

    static Element g;
    static ElementPow preG;
    static Broadcast<ElementPow> broadcastPow;

    private static final Map<ByteArrayKey, ArrayList<byte[]>> FSet = new HashMap<>(); // rID1 | gST c 1 ·tagw1 /tagID1 , gSTc 2 ·tagw2 /tagID1
    private static final Map<byte[], byte[]> ISet = new HashMap<>();
    private static final Map<String, Tuple2<byte[], Integer>> W = new HashMap<>(); // key is the keyword, value is the tuple of (ST_c,c)

    public static void main(String[] args) {

        conf = new SparkConf().setAppName("Generator");

        conf.setMaster("local[2]");

        // Serialise the element for pre-processing
        conf.set("spark.serializer", "org.apache.spark.serializer.KryoSerializer")
                .set("spark.kryoserializer.buffer.max", "512")
                .registerKryoClasses(new Class[]{Element.class});

        sc = new JavaSparkContext(conf);


        sc.setLogLevel(Level.ERROR.toString());

        // Try to load existing group
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
        preG = g.getElementPowPreProcessing();
        // Create broadcast variables
        broadcastPow = sc.broadcast(preG);


        // File path is passed as argument
        if(args.length > 1) {

            if(args[0].equals("update")){
                update(args[1]);
            } else if (args[0].equals("delete")) {
                delete(args[1]);
            } else {
                LOGGER.error("Invalid argument");
            }
        } else {
            LOGGER.error("Invalid argument");
        }
    }

    public static void update(String file){

        // Redis is going to overwrite the existing data
        updateFSet();
        updateISet();

        // Generate inverted index
        JavaPairRDD<String, String> invertedIndex = sc.textFile(file)
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

                String keyword = entry._1;
                String[] ids = entry._2.split(",");

                byte[] tag_w = hmac.encode(keyword.getBytes(), SecureParam.K_T);
                byte[] k_w = cmac.encode(keyword.getBytes(), SecureParam.K_S);

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

                    byte[] enc_id  = aescbc.encrypt(id.getBytes(), k_w);

                    c++;

                    ST = rsa.encrypt(ST);

                    byte[] generator = broadcastPow.getValue()
                            .powZn(PairingUtil.getZrElementForHash(ST)
                                    .mul(PairingUtil.getZrElementForHash(tag_w)))
                            .getImmutable()
                            .toBytes();


                    byte[] l = hmac.encode(generator, SecureParam.K_h);

                    // Append enc_id to ISet[l]
                    if(!ISet.containsKey(l)) {
                        ISet.put(l, enc_id);
                    }

                    // delta = g ^ ( (ST-c * tag_w) / (tag_id))

                    byte[] delta = broadcastPow.getValue()
                            .powZn(PairingUtil.getZrElementForHash(ST)
                                    .mul(PairingUtil.getZrElementForHash(tag_w))
                                    .div(PairingUtil.getZrElementForHash(tag_id)))
                            .getImmutable()
                            .toBytes();

                    // Append delta into FSet[r_ID]

                    ByteArrayKey byteArrayKey = new ByteArrayKey(r);

                    if(!FSet.containsKey(byteArrayKey)) {
                        ArrayList<byte[]> keywords = new ArrayList<>();
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
                    byte[] l2 = hmac.encode(tk_prime, SecureParam.K_h);

                }

                // Update W
                // W[w] = (ST, c)
                W.put(keyword, new Tuple2<>(ST, c));
            });
        });

        DataSource redis = new RedisDataSource();
        // Redis supports Map<byte[], byte[]> hence FSet needs to be converted
        Map<byte[], byte[]> FSet_ByteMap = new HashMap<>();
        for (Map.Entry<ByteArrayKey, ArrayList<byte[]>> entry : FSet.entrySet()) {
            FSet_ByteMap.put(entry.getKey().getData(), DataTypeConverter.ArrayListToByte(entry.getValue()));
        }

        redis.mset("FSet".getBytes(),FSet_ByteMap);
        redis.mset("ISet".getBytes(), ISet);

        // Print out the current FSet and ISet
        System.out.println("FSet: ");
        redis.hget_all("FSet".getBytes()).forEach((k, v) -> {
            System.out.println(StringByteConverter.byteToHex(k) + " : ");
            Objects.requireNonNull(DataTypeConverter.ByteToArrayList(v)).forEach(value ->{
                System.out.println(StringByteConverter.byteToHex(value));
            });
        });

        System.out.println("ISet: ");
        redis.hget_all("ISet".getBytes()).forEach((k, v) -> {
            System.out.println(StringByteConverter.byteToHex(k) + " : " + StringByteConverter.byteToHex(v));
        });

        redis.close();
    }

    public static void delete(String id){

        // Compute tag id and r
        Hash cmac = new AESCMAC();
        Hash hmac = new HMACSHA();

        byte[] tag_id = hmac.encode(id.getBytes(), SecureParam.K_2);
        byte[] r = cmac.encode(id.getBytes(), SecureParam.K_1);

        // Search in FSet for r
        DataSource redis = new RedisDataSource();
        byte[] delta = redis.hget("FSet".getBytes(), r);

        // Disassemble delta into ArrayList<byte[]>
        ArrayList<byte[]> deltaList = DataTypeConverter.ByteToArrayList(delta);

        assert deltaList != null;
        for(byte[] delta_elements : deltaList){

            Element deltaElement = PairingUtil.getGTElementFromByte(delta_elements);

            byte[] delta_tag_id = deltaElement.powZn(PairingUtil.getZrElementForHash(tag_id))
                    .getImmutable()
                    .toBytes();

            // Compute the index l
            byte[] l = hmac.encode(delta_tag_id, SecureParam.K_h);

            // Delete the entry in ISet[l]
            if(l != null){
                redis.hdel("ISet".getBytes(), l);
            }
        }

    }

    public static void updateFSet(){

        DataSource redis = new RedisDataSource();

        // Get the current FSet
        Map<byte[], byte[]> FSet_redis = redis.hget_all("FSet".getBytes());

        // Disassemble FSet into HashMap<ByteArrayKey, ArrayList<byte[]>>
        FSet_redis.forEach((k, v) -> {
            FSet.put(new ByteArrayKey(k), DataTypeConverter.ByteToArrayList(v));
        });
    }

    public static void updateISet(){

        DataSource redis = new RedisDataSource();

        // Get the current ISet
        Map<byte[], byte[]> ISet_redis = redis.hget_all("ISet".getBytes());
        ISet.putAll(ISet_redis);
    }
}