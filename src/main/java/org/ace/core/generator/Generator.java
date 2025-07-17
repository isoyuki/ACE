package org.ace.core.generator;

import org.ace.crypto.primitives.impl.mac.AESCMAC;
import it.unisa.dia.gas.jpbc.Element;
import it.unisa.dia.gas.jpbc.ElementPow;
import org.apache.log4j.Level;
import org.apache.spark.SparkConf;
import org.apache.spark.api.java.JavaPairRDD;
import org.apache.spark.api.java.JavaSparkContext;
import org.apache.spark.broadcast.Broadcast;
import org.ace.core.dao.DataSource;
import org.ace.core.dao.impl.RedisDataSource;
import org.ace.core.util.SecureParam;
import org.ace.crypto.primitives.AsymmetricCipher;
import org.ace.crypto.primitives.Hash;
import org.ace.crypto.primitives.SymmetricCipher;
import org.ace.crypto.primitives.impl.cipher.AESCBC;
import org.ace.crypto.primitives.impl.cipher.RSA;
import org.ace.crypto.primitives.impl.mac.HMACSHA;
import org.ace.crypto.util.ByteArrayKey;
import org.ace.crypto.util.PairingUtil;
import org.ace.crypto.util.StringByteConverter;
import org.ace.util.DataTypeConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scala.Tuple2;

import java.io.*;
import java.util.*;

// Generator acts as Trustee

public class Generator {

    private static final Logger LOGGER = LoggerFactory.getLogger(Generator.class);

    static SparkConf conf;
    static JavaSparkContext sc;
    static Element g;
    static ElementPow preG;
    static Broadcast<ElementPow> broadcastPow;

    private static final Map<String, ArrayList<byte[]>> FSet = new HashMap<>();
    private static final Map<ByteArrayKey, byte[]> ISet = new HashMap<>();
    private static final Map<String, Tuple2<byte[], Integer>> W = new HashMap<>();
    private static final Properties properties = new Properties();
    private static final String buildDir = "target/";

    public static void main(String[] args) {

        // load properties
        try{
            properties.load(new FileInputStream(buildDir+ "config.properties"));
        } catch (IOException e) {
            e.printStackTrace();
        }


        conf = new SparkConf().setAppName("Generator");

        conf.setMaster("local[16]");

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

        preG = g.getElementPowPreProcessing();
        broadcastPow = sc.broadcast(preG);

        // File path is passed as argument
        if(args.length > 1) {

            if(args[0].equals("update")){
                update(args);
            } else if (args[0].equals("delete")) {
                delete(args[1]);
            } else {
                LOGGER.error("Invalid argument");
            }
        } else {
            LOGGER.error("Invalid argument");
        }

        DataSource redis = new RedisDataSource();
        System.out.println("Redis FSet size: " + redis.hget_all("FSet".getBytes()).size());
        System.out.println("Redis ISet size: " + redis.hget_all("ISet".getBytes()).size());
        redis.close();

    }

    public static void update(String[] args){

        String file = args[1];

        if(args.length > 2 && args[2].equals("new")){
            File keywords_file = new File(properties.getProperty("keywords_file"));
            if(keywords_file.exists()){
                if(keywords_file.delete()){
                    LOGGER.info("Deleted existing keywords file");
                } else {
                    LOGGER.error("Failed to delete existing keywords file");
                };
            }

            DataSource redis = new RedisDataSource();
            redis.flushAll();
            redis.close();

        } else{
            updateW();
        }

        // Redis is going to overwrite the existing data

        long totalExecutionTime = System.currentTimeMillis();

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
                .reduceByKey((v1, v2) -> v1 + "," + v2).cache().repartition(16);

        LOGGER.debug("Inverted index is generated.");

        Map<ByteArrayKey, byte[]> tag_ids = new HashMap<>();

        invertedIndex.foreachPartition(partition -> {

            Hash hmac = new HMACSHA();
            Hash cmac = new AESCMAC();
            SymmetricCipher aescbc = new AESCBC();
            AsymmetricCipher rsa = new RSA();

            partition.forEachRemaining(entry -> {

                DataSource redis = new RedisDataSource();

                String keyword = entry._1;
                String[] ids = entry._2.split(",");

                // System.out.println("Keyword: " + keyword + " has " + ids.length + " ids");

                byte[] tag_w = hmac.encode(keyword.getBytes(), SecureParam.K_T);
                byte[] k_w = cmac.encode(keyword.getBytes(), SecureParam.K_S);

                int c;
                byte[] ST;

                if(!W.containsKey(keyword)) {
                    c = 0;
                    ST = rsa.decrypt(SecureParam.sample_array);
                }else{
                    ST = W.get(keyword)._1;
                    c = W.get(keyword)._2;
                }

                redis.initialiseMulti();

                for (String id : ids) { // for IDi ∈ GDB(w) do

                    byte[] r = cmac.encode(id.getBytes(), SecureParam.K_1);

                    ByteArrayKey r_id_key = new ByteArrayKey(r);

                    byte[] tag_id;

                    if(!tag_ids.containsKey(r_id_key)){
                        tag_id = hmac.encode(id.getBytes(), SecureParam.K_2);
                        tag_ids.put(r_id_key, tag_id);
                    }else{
                        tag_id = tag_ids.get(r_id_key);
                    }

                    byte[] enc_id  = aescbc.encrypt(id.getBytes(), k_w);

                    c++;

                    ST = rsa.decrypt(ST);

                    byte[] generator = broadcastPow.getValue()
                            .powZn(PairingUtil.getZrElementForHash(ST)
                                    .mul(PairingUtil.getZrElementForHash(tag_w)))
                            .getImmutable()
                            .toBytes();

                    byte[] l = hmac.encode(generator, SecureParam.K_h);

                    redis.hsetnx_transaction("ISet".getBytes(), l, enc_id);

                    byte[] delta = broadcastPow.getValue()
                            .powZn(PairingUtil.getZrElementForHash(ST)
                                    .mul(PairingUtil.getZrElementForHash(tag_w))
                                    .div(PairingUtil.getZrElementForHash(tag_id)))
                            .getImmutable()
                            .toBytes();

                    byte[] FSet_value = redis.hget("FSet".getBytes(), r);
                    if (FSet_value == null) {
                        ArrayList<byte[]> keywords = new ArrayList<>();
                        keywords.add(delta);
                        byte[] byte_arraylist = DataTypeConverter.ArrayListToByte(keywords);
                        assert byte_arraylist != null;
                        redis.hsetnx("FSet".getBytes(), r, byte_arraylist);
                    } else {
                        ArrayList<byte[]> keywords = DataTypeConverter.ByteToArrayList(FSet_value);
                        assert keywords != null;
                        keywords.add(delta);
                        byte[] byte_arraylist = DataTypeConverter.ArrayListToByte(keywords);
                        assert byte_arraylist != null;
                        redis.hset("FSet".getBytes(), r, byte_arraylist);
                    }
                }

                // Update W
                // W[w] = (ST, c)
                W.put(keyword, new Tuple2<>(ST, c));

                redis.executeMulti();
                redis.close();
            });
        });

        long totalExecutionTimeEnd = System.currentTimeMillis();
        System.out.println("Execution time: " + (totalExecutionTimeEnd - totalExecutionTime) + "ms");

        File keywords_file = new File(properties.getProperty("keywords_file"));
        BufferedWriter writer = null;

        try{
            writer = new BufferedWriter(new FileWriter(keywords_file));
            for (Map.Entry<String, Tuple2<byte[], Integer>> entry : W.entrySet()) {
                writer.write(entry.getKey() + "," + StringByteConverter.byteToHex(entry.getValue()._1) + "," + entry.getValue()._2);
                writer.newLine();
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                assert writer != null;
                writer.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }


//        printFSet();
//        printISet();

    }

    public static void delete(String id){

        long start = System.currentTimeMillis();

        // Compute tag id and r
        Hash cmac = new AESCMAC();
        Hash hmac = new HMACSHA();

        byte[] tag_id = hmac.encode(id.getBytes(), SecureParam.K_2);

        byte[] r = cmac.encode(id.getBytes(), SecureParam.K_1);

        // Search in FSet for r
        DataSource redis = new RedisDataSource();

        byte[] delta = redis.hget("FSet".getBytes(), r);

        if (delta == null) {
            System.out.println("ID not found");
            return;
        }

        System.out.println("Delete entires for ID: " + id);

        // Disassemble delta into ArrayList<byte[]>
        ArrayList<byte[]> deltaList = DataTypeConverter.ByteToArrayList(delta);
        assert deltaList != null;
        System.out.println("No. keywords associated with this ID: " + deltaList.size());

        deltaList.parallelStream().forEach(delta_elements ->{
            Element deltaElement = PairingUtil.getGTElementFromByte(delta_elements);

            byte[] delta_tag_id = deltaElement.powZn(PairingUtil.getZrElementForHash(tag_id))
                    .getImmutable()
                    .toBytes();

            // Compute the index l
            byte[] l = hmac.encode(delta_tag_id, SecureParam.K_h);

            // Delete the entry in ISet[l]
            if(l != null){
                redis.hdel("ISet".getBytes(), l);
            } else{
                System.out.println("l is null");
            }
        });
        // Delete the entry in FSet[r]
        redis.hdel("FSet".getBytes(), r);

        redis.close();

        System.out.println("Execution Time: " + (System.currentTimeMillis() - start) + " ms");

    }

    public static void updateFSet(){

        DataSource redis = new RedisDataSource();

        Map<byte[], byte[]> FSet_redis = redis.hget_all("FSet".getBytes());

//        // Disassemble FSet into HashMap<ByteArrayKey, ArrayList<byte[]>>
        FSet_redis.forEach((k, v) -> {
            FSet.putIfAbsent(StringByteConverter.byteToHex(k), DataTypeConverter.ByteToArrayList(v));
        });

        redis.close();
    }

    public static void updateISet(){

        DataSource redis = new RedisDataSource();
        // Get the current ISet
        Map<byte[], byte[]> ISet_redis = redis.hget_all("ISet".getBytes());
        ISet_redis.forEach((k, v) -> {
            ISet.put(new ByteArrayKey(k), v);
        });

        redis.close();
    }

    public static void updateW(){

        File keywords_file = new File(properties.getProperty("keywords_file"));

        BufferedReader reader = null;
        try{
            reader = new BufferedReader(new FileReader(keywords_file));
            String line;
            while((line = reader.readLine()) != null){
                String[] line_split = line.split(",");
                W.put(line_split[0], new Tuple2<>(StringByteConverter.hexToByte(line_split[1]), Integer.parseInt(line_split[2])));
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                assert reader != null;
                reader.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static void printFSet(){

        DataSource redis = new RedisDataSource();

        System.out.println("FSet: ");
        redis.hget_all("FSet".getBytes()).forEach((k, v) -> {
            Objects.requireNonNull(DataTypeConverter.ByteToArrayList(v)).forEach(value ->{
                System.out.println(StringByteConverter.byteToHex(value));
            });
        });

        redis.close();

    }

    public static void printISet(){

        DataSource redis = new RedisDataSource();

        System.out.println("ISet: ");
        redis.hget_all("ISet".getBytes()).forEach((k, v) -> {
            System.out.println(StringByteConverter.byteToHex(k) + " : " + StringByteConverter.byteToHex(v));
        });

        redis.close();
    }
}