package net.homework.blockchain.client;

import io.leonard.Base58;
import lombok.SneakyThrows;
import net.homework.blockchain.bean.Transaction;
import net.homework.blockchain.util.CryptoUtils;
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
import java.util.Arrays;
import java.util.Map;

import static net.homework.blockchain.util.ByteUtils.removeLeadingZero;

public class UserImpl implements User {
    @Override
    public String generatePrivateKey() throws NoSuchAlgorithmException, InvalidAlgorithmParameterException, IOException {
        KeyPairGenerator kpg = KeyPairGenerator.getInstance("EC");
        ECGenParameterSpec curve = new ECGenParameterSpec("secp256k1");
        kpg.initialize(curve);
        KeyPair kp = kpg.genKeyPair();
        ECPrivateKey pri = (ECPrivateKey) kp.getPrivate();
        System.out.println(pri);
        ECPublicKey pub = (ECPublicKey) kp.getPublic();
        System.out.println(pub);
        // 0 - Private ECDSA Key
        byte[] b0 = removeLeadingZero(pri.getS().toByteArray());
        System.out.print("0: ");
        System.out.println(Hex.encodeHex(b0, false));
        char[] localPriKey = Hex.encodeHex(b0, false);
        FileWriter fileWriter = new FileWriter("E:/privateKey.key");
        BufferedWriter bufferedWriter = new BufferedWriter(fileWriter);
        bufferedWriter.write(String.valueOf(localPriKey));
        bufferedWriter.flush();
        bufferedWriter.close();
        fileWriter.close();
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
        return null;
    }


    @Override
    public BigInteger loadPrivateKey() throws IOException, DecoderException {
        FileReader fileReader = new FileReader("E:/privateKey.key");
        BufferedReader bufferedReader = new BufferedReader(fileReader);
        byte[] localKeyByte = Hex.decodeHex(bufferedReader.readLine());
        System.out.println("Localread" + localKeyByte);
        BigInteger localKeyBig = new BigInteger(localKeyByte);
        return localKeyBig;
    }


    @Override
    public char[] getPublicKey(BigInteger privateKey) throws NoSuchAlgorithmException, NoSuchProviderException, InvalidKeySpecException {
        Security.addProvider(new BouncyCastleProvider());
        KeyFactory keyFactory = KeyFactory.getInstance("ECDSA","BC");
        ECParameterSpec eCParameterSpec = ECNamedCurveTable.getParameterSpec("secp256k1");
        ECPoint Q = eCParameterSpec.getG().multiply(privateKey);
        org.bouncycastle.jce.spec.ECPublicKeySpec pubSpec = new org.bouncycastle.jce.spec.ECPublicKeySpec(Q, eCParameterSpec);
        ECPublicKey publicKeyGenerated = (ECPublicKey) keyFactory.generatePublic(pubSpec);
        System.out.println("publicG"+publicKeyGenerated);
        byte[] bytes = new byte[65];
        bytes[0] = 4;
        System.arraycopy(removeLeadingZero(publicKeyGenerated.getW().getAffineX().toByteArray()), 0, bytes, 1, 32);
        System.arraycopy(removeLeadingZero(publicKeyGenerated.getW().getAffineY().toByteArray()), 0, bytes, 33, 32);
        System.out.print("1: ");
        System.out.println(Hex.encodeHex(bytes, false));
        return Hex.encodeHex(bytes, false);
    }

    @Override
    public String getAddress(char[] publicKey) throws DecoderException {
        return Base58.encode(Hex.decodeHex(publicKey));
    }

    @Override
    public Transaction assembleTx(Map<byte[], byte[]> recipientsWithAmount) {
        return null;
    }

    @Override
    public void broadcastTx(Transaction tx) {

    }

    @Override
    public Map<Transaction, Integer> getUTXOs() {
        return null;
    }

}
