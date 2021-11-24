package net.homework.blockchain.client;

import cn.hutool.http.HttpUtil;
import io.leonard.Base58;
import net.homework.blockchain.util.ByteUtils;
import net.homework.blockchain.util.CryptoUtils;
import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;
import org.bouncycastle.jce.ECNamedCurveTable;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.jce.spec.ECParameterSpec;
import org.bouncycastle.math.ec.ECPoint;


import java.io.*;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.ECPublicKey;
import java.security.spec.ECGenParameterSpec;
import java.security.spec.InvalidKeySpecException;
import java.util.*;
import java.util.stream.Collectors;

import static net.homework.blockchain.util.ByteUtils.removeLeadingZero;

public class Usertest {
    public static void main(String args[]) throws IOException, NoSuchAlgorithmException, InvalidAlgorithmParameterException, DecoderException, NoSuchProviderException, InvalidKeySpecException {
        Security.addProvider(new BouncyCastleProvider());
        KeyPairGenerator kpg = KeyPairGenerator.getInstance("EC");
        ECGenParameterSpec curve = new ECGenParameterSpec("secp256k1");
        kpg.initialize(curve);
        KeyPair kp = kpg.genKeyPair();
        ECPrivateKey pri = (ECPrivateKey) kp.getPrivate();
        ECPublicKey pub = (ECPublicKey) kp.getPublic();
        // 0 - Private ECDSA Key
        byte[] b0 = removeLeadingZero(pri.getS().toByteArray());
        System.out.print("0: ");
        System.out.println(Hex.encodeHex(b0, false));
        //// 1 - Public ECDSA Key
        //byte[] b1 = new byte[65];
        //b1[0] = 4;
        //System.arraycopy(removeLeadingZero(pub.getW().getAffineX().toByteArray()), 0, b1, 1, 32);
        //System.arraycopy(removeLeadingZero(pub.getW().getAffineY().toByteArray()), 0, b1, 33, 32);
        //System.out.print("1: ");
        //System.out.println(Hex.encodeHex(b1, false));
        //System.out.println(Base58.encode(Hex.decodeHex(Hex.encodeHex(b1,false))));
        char[] localPriKey = Hex.encodeHex(b0, false);
        FileWriter prifw = new FileWriter("./privateKey.key");
        BufferedWriter pribw = new BufferedWriter(prifw);
        pribw.write(String.valueOf(localPriKey));
        pribw.flush();
        pribw.close();
        prifw.close();
        FileReader prifr = new FileReader("./privateKey.key");
        BufferedReader pribr = new BufferedReader(prifr);
        byte[] LocalKeyByte =Hex.decodeHex(pribr.readLine());
        System.out.println("Localread"+LocalKeyByte);
        BigInteger LocalKeyBig = new BigInteger(LocalKeyByte);
        KeyFactory keyFactory = KeyFactory.getInstance("ECDSA","BC");
        ECParameterSpec eCParameterSpec = ECNamedCurveTable.getParameterSpec("secp256k1");
        ECPoint Q = eCParameterSpec.getG().multiply(LocalKeyBig);
        org.bouncycastle.jce.spec.ECPublicKeySpec pubSpec = new org.bouncycastle.jce.spec.ECPublicKeySpec(Q, eCParameterSpec);
        ECPublicKey publicKeyGenerated = (ECPublicKey) keyFactory.generatePublic(pubSpec);
        System.out.println("publicG"+publicKeyGenerated);
        //byte[] b2 = new byte[65];
        //b2[0] = 4;
        //System.arraycopy(removeLeadingZero(publicKeyGenerated.getW().getAffineX().toByteArray()), 0, b2, 1, 32);
        //System.arraycopy(removeLeadingZero(publicKeyGenerated.getW().getAffineY().toByteArray()), 0, b2, 33, 32);
        //System.out.print("1: ");
        //System.out.println(Hex.encodeHex(b2, false));
        //System.out.println(Base58.encode(Hex.decodeHex(Hex.encodeHex(b2,false))));
        // 1 - Public ECDSA Key
        byte[] b1 = new byte[65];
        b1[0] = 4;
        System.arraycopy(removeLeadingZero(pub.getW().getAffineX().toByteArray()), 0, b1, 1, 32);
        System.arraycopy(removeLeadingZero(pub.getW().getAffineY().toByteArray()), 0, b1, 33, 32);
        System.out.print("1: ");
        System.out.println(Hex.encodeHex(b1, false));

        // 2 - SHA-256 hash of 1
        byte[] b2 = CryptoUtils.sha256(b1);
        System.out.print("2: ");
        System.out.println(Hex.encodeHex(b2, false));

        // 3 - RIPEMD-160 Hash of 2
        byte[] b3 = CryptoUtils.ripmd160(b2);
        System.out.print("3: ");
        System.out.println(Hex.encodeHex(b3, false));

        // 4 - Adding network bytes to 3
        byte[] b4 = new byte[21];
        System.arraycopy(b3, 0, b4, 1, 20);
        System.out.print("4: ");
        System.out.println(Hex.encodeHex(b4, false));

        // 6 - double SHA-256 hash of 4
        byte[] b6 = CryptoUtils.sha256Twice(b4);
        System.out.print("6: ");
        System.out.println(Hex.encodeHex(b6, false));

        // 7 - First four bytes of 6
        byte[] b7 = Arrays.copyOf(b6, 4);
        System.out.print("7: ");
        System.out.println(Hex.encodeHex(b7, false));

        // 8 - Adding 7 at the end of 4
        byte[] b8 = new byte[25];
        System.arraycopy(b4, 0, b8, 0, 21);
        System.arraycopy(b7, 0, b8, 21, 4);
        System.out.print("8: ");
        System.out.println(Hex.encodeHex(b8, false));

        // 9 - Base58 encoding of 8
        System.out.print("9: ");
        System.out.println(Base58.encode(b8));
        String result = "{\"OEF31673217A8D528707EC44AE168182A8BE928178C5769EDD8DC427A4274990\": \"0,1\","+
"\"7471AC72E90A1CFB208435599A2BCF0E8179F84C86AFCC86EB7992EE1D57711B\": \"0,1\","+
"\"720B6FE83D297518368146E6E00ABF74D72F1F79FA723BE57C8635C04038E381\": \"0,1\","+
"\"93DC260FABC439E65C29A788CA1C632EA7724BE05937274593E9D5B1B8F13B05\": \"0\","+
"\"ADA29188EFAE19F58572B22CDE317590453362AOA29E8026CF3DB78D15DE1EA3\":\"0\","+
"\"F2B2DA9BBFA930AB1F90DBFD2F626249A78744F6B9103B19D08B1635536191B1\": \"0\","+
"\"6FF6B7BEBEF1AE8A95611F069E0A80C5142E1ABC8E63275994324555AF546D00\": \"0\","+
"\"15A1A230EE39C6F6FC869709D57FC965EA7407ED1A922CEE762A263FEE7F0BOE\":\"0\","+
"\"4F99D1FC109C6CE1C2BDB3F42437062977B98EF7BC2E2B67B8B2EEF897FCD494\": \"0\","+
"\"76C2637618591A729D0E559EADECF8D6656EF6E35BDC7E6FBEA2640BC4705CE8\":\"0\"}";
        Map<String, String> map = new HashMap<>();
        map = ByteUtils.fromJson(result,map);
        //System.out.println(map);
        Map<ByteBuffer, List<Integer>> map1 = new HashMap<>();
        for (Map.Entry<String, String> entry : map.entrySet()) {
            String mapkey = entry.getKey();
            ByteBuffer mapKey = ByteBuffer.wrap(mapkey.getBytes(StandardCharsets.UTF_8));
            String mapvalue = entry.getValue();
            String[] mapvalueString = mapvalue.split(",");
            int[] mapvalueint = Arrays.stream(mapvalueString).mapToInt(Integer::parseInt).toArray();
            List<Integer> mapValue = Arrays.stream(mapvalueint).boxed().collect(Collectors.toList());
            map1.put(mapKey,mapValue);
        }
    }
}
