package org.ace.core.client;

import org.ace.crypto.primitives.impl.mac.AESCMAC;
import it.unisa.dia.gas.jpbc.Element;
import it.unisa.dia.gas.jpbc.ElementPow;
import org.ace.core.server.query.impl.Search;
import org.ace.core.util.SecureParam;
import org.ace.crypto.primitives.Hash;
import org.ace.crypto.primitives.SymmetricCipher;
import org.ace.crypto.primitives.impl.cipher.AESCBC;
import org.ace.crypto.primitives.impl.mac.HMACSHA;
import org.ace.crypto.util.PairingUtil;
import org.ace.crypto.util.StringByteConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scala.Tuple2;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

public class Client {

    private static final Logger LOGGER = LoggerFactory.getLogger("Client");

    // Required Primitives
    private final Hash cmac;
    private final Hash hmac;
    private final ElementPow preG;

    private static final Map<String, Tuple2<byte[], Integer>> W = new HashMap<>();

    private static final Properties properties = new Properties();
    private static final String buildDir = "target/";

    public Client() {

        try{
            properties.load(new FileInputStream(buildDir + "config.properties"));
        } catch (IOException e) {
            e.printStackTrace();
        }

        cmac = new AESCMAC();
        hmac = new HMACSHA();
        preG = PairingUtil.loadGTElementFromFile("elliptical_g")
                .getElementPowPreProcessing();

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

    // Act as vetter
    public ArrayList<String> retrieve(ArrayList<byte[]> enc_ids, byte[] K_w){
        SymmetricCipher aescbc = new AESCBC();
        ArrayList<String> IDSet = new ArrayList<>(enc_ids.size());

        enc_ids.parallelStream().map(id -> aescbc.decrypt(id, K_w)).forEach(dec_id -> IDSet.add(new String(dec_id)));
        return IDSet;
    }

    public static void main(String[] args){
        Client client = new Client();
        Search query = new Search();


        if (args.length > 0){

            String keyword = args[0];

            long start = System.currentTimeMillis();

            byte[] tag_w = client.hmac.encode(keyword.getBytes(), SecureParam.K_T);

            Element tk = client.preG
                    .powZn(PairingUtil.getZrElementForHash(tag_w))
                    .getImmutable();

            query.setTk(tk);

            if (W.containsKey(keyword)) {
                query.setST_c(W.get(keyword)._1);
                query.setC(W.get(keyword)._2);
            }else{
                return;
            }

            query.execute();


            byte[] K_w = client.cmac.encode(keyword.getBytes(), SecureParam.K_S);

            ArrayList<String> decrypted = client.retrieve(query.getResultList(), K_w);

            System.out.println(query.getResultSize() + " IDs with keyword " + keyword + ": ");
            decrypted.forEach(id -> {
                System.out.print("|" + id);
            });
            System.out.println();

            System.out.println("Execution Time: " + (System.currentTimeMillis() - start) + " ms");

        }

    }



}
