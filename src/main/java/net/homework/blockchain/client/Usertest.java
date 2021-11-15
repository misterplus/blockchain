package net.homework.blockchain.client;

import io.leonard.Base58;
import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;
import org.bouncycastle.jce.ECNamedCurveTable;
import org.bouncycastle.jce.spec.ECNamedCurveParameterSpec;
import org.bouncycastle.jce.spec.ECParameterSpec;


import java.io.*;
import java.math.BigInteger;
import java.security.*;
import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.ECPublicKey;
import java.security.spec.ECGenParameterSpec;
import java.security.spec.ECPoint;

import static net.homework.blockchain.util.ByteUtils.removeLeadingZero;

public class Usertest {
    public static void main(String args[]) throws IOException, NoSuchAlgorithmException, InvalidAlgorithmParameterException, DecoderException {
        KeyPairGenerator kpg = KeyPairGenerator.getInstance("EC");
        ECGenParameterSpec curve = new ECGenParameterSpec("secp256k1");
        kpg.initialize(curve);
        KeyPair kp = kpg.genKeyPair();
        ECPrivateKey pri = (ECPrivateKey) kp.getPrivate();
        System.out.println("pri:"+pri);
        System.out.println(pri.getS());
        ECPublicKey pub = (ECPublicKey) kp.getPublic();
        System.out.println("pub:"+pub);
        // 0 - Private ECDSA Key
        byte[] b0 = removeLeadingZero(pri.getS().toByteArray());
        System.out.print("0: ");
        System.out.println(Base58.encode(pri.getS().toByteArray()));
        System.out.println(Hex.encodeHex(b0, false));
        // 1 - Public ECDSA Key
        byte[] b1 = new byte[65];
        b1[0] = 4;
        System.arraycopy(removeLeadingZero(pub.getW().getAffineX().toByteArray()), 0, b1, 1, 32);
        System.arraycopy(removeLeadingZero(pub.getW().getAffineY().toByteArray()), 0, b1, 33, 32);
        System.out.print("1: ");
        System.out.println(Hex.encodeHex(b1, false));
        try {
            char[] Localpri = Hex.encodeHex(removeLeadingZero(pri.getS().toByteArray()),false);
            System.out.println(Localpri);
            FileWriter prifw = new FileWriter("E:/privateKey.key");
            BufferedWriter pribw = new BufferedWriter(prifw);
            pribw.write(Localpri);
            pribw.flush();
            pribw.close();
            prifw.close();
        } catch (Exception e) {
            throw e;
        }
        FileReader prifr = new FileReader("E:/privateKey.key");
        BufferedReader pribr = new BufferedReader(prifr);
        byte[] Localprir =Hex.decodeHex(pribr.readLine());
        BigInteger result = 0 ;
        ByteArrayInputStream bis=new ByteArrayInputStream(Localprir);
        DataInputStream dis=new DataInputStream(bis);
        try {
            result= dis.readInt();
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println(result);

        try {
            KeyFactory keyFactory = KeyFactory.getInstance("ECDSA","BC");
            ECParameterSpec ecpc = ECNamedCurveTable.getParameterSpec("secp256k1");
            //ECPoint Q = ecpc.getG().multiply(Localprir);
        } catch (NoSuchProviderException e) {
            e.printStackTrace();
        }


    }
}
