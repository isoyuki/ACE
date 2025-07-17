package org.monash.core.client;

import edu.monash.crypto.primitives.impl.mac.AESCMAC;
import it.unisa.dia.gas.jpbc.Element;
import it.unisa.dia.gas.jpbc.ElementPow;
import org.monash.core.server.query.impl.Search;
import org.monash.core.util.SecureParam;
import org.monash.crypto.primitives.Hash;
import org.monash.crypto.primitives.impl.mac.HMACSHA;
import org.monash.crypto.util.PairingUtil;
import org.monash.crypto.util.StringByteConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scala.Tuple2;

import java.io.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

public class Client {

    private static final Logger LOGGER = LoggerFactory.getLogger("Client");

    // Required Primitives
    private final Hash cmac;
    private final Hash hmac;
    private final ElementPow preG;

    private static final Map<String, Tuple2<byte[], Integer>> W = new HashMap<>(); // key is the keyword, value is the tuple of (ST_c,c)

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

    public static void main(String[] args){
        Client client = new Client();
        Search query = new Search();

        if (args.length > 0){

            String keyword = args[0];

            byte[] tag_w = client.hmac.encode(keyword.getBytes(), SecureParam.K_T);

            System.out.println("tag_w: " + StringByteConverter.byteToHex(tag_w));

            Element tk = client.preG
                    .powZn(PairingUtil.getZrElementForHash(tag_w))
                    .getImmutable();

            query.setTk(tk);

            // Print W
            if (W.containsKey(keyword)) {

                System.out.println("Keyword found in W");

                query.setST_c(W.get(keyword)._1);
                query.setC(W.get(keyword)._2);
            }else{
                return;
            }

            query.execute();

        }

    }


}
