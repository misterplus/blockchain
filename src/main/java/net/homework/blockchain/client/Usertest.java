package net.homework.blockchain.client;

import io.leonard.Base58;
import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;
import org.bouncycastle.jce.ECNamedCurveTable;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.jce.spec.ECParameterSpec;
import org.bouncycastle.math.ec.ECPoint;


import java.io.*;
import java.math.BigInteger;
import java.security.*;
import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.ECPublicKey;
import java.security.spec.ECGenParameterSpec;
import java.security.spec.InvalidKeySpecException;

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
        // 1 - Public ECDSA Key
        byte[] b1 = new byte[65];
        b1[0] = 4;
        System.arraycopy(removeLeadingZero(pub.getW().getAffineX().toByteArray()), 0, b1, 1, 32);
        System.arraycopy(removeLeadingZero(pub.getW().getAffineY().toByteArray()), 0, b1, 33, 32);
        System.out.print("1: ");
        System.out.println(Hex.encodeHex(b1, false));
        System.out.println(Base58.encode(Hex.decodeHex(Hex.encodeHex(b1,false))));
        char[] localPriKey = Hex.encodeHex(b0, false);
        FileWriter prifw = new FileWriter("E:/privateKey.key");
        BufferedWriter pribw = new BufferedWriter(prifw);
        pribw.write(String.valueOf(localPriKey));
        pribw.flush();
        pribw.close();
        prifw.close();
        FileReader prifr = new FileReader("E:/privateKey.key");
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
        byte[] b2 = new byte[65];
        b2[0] = 4;
        System.arraycopy(removeLeadingZero(publicKeyGenerated.getW().getAffineX().toByteArray()), 0, b2, 1, 32);
        System.arraycopy(removeLeadingZero(publicKeyGenerated.getW().getAffineY().toByteArray()), 0, b2, 33, 32);
        System.out.print("1: ");
        System.out.println(Hex.encodeHex(b2, false));
        System.out.println(Base58.encode(Hex.decodeHex(Hex.encodeHex(b2,false))));

    }
}
