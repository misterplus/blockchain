package net.homework.blockchain.util;

import io.leonard.Base58;
import lombok.SneakyThrows;
import org.apache.commons.codec.binary.Hex;
import org.bouncycastle.jce.ECNamedCurveTable;
import org.bouncycastle.jce.interfaces.ECPrivateKey;
import org.bouncycastle.jce.interfaces.ECPublicKey;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.jce.spec.ECParameterSpec;
import org.bouncycastle.jce.spec.ECPrivateKeySpec;
import org.bouncycastle.jce.spec.ECPublicKeySpec;
import org.bouncycastle.math.ec.ECPoint;

import java.math.BigInteger;
import java.security.*;
import java.util.Arrays;

public class CryptoUtils {
    private static final ECParameterSpec ecSpec = ECNamedCurveTable.getParameterSpec("secp256k1");
    private static MessageDigest sha256;
    private static MessageDigest ripemd160;
    private static KeyFactory keyFactory;
    private static Signature signer;

    static {
        Security.addProvider(new BouncyCastleProvider());
        try {
            sha256 = MessageDigest.getInstance("SHA-256");
            ripemd160 = MessageDigest.getInstance("RIPEMD160");
        } catch (NoSuchAlgorithmException ignored) {
        }
    }

    static {
        try {
            Security.addProvider(new BouncyCastleProvider());
            keyFactory = KeyFactory.getInstance("ECDSA", "BC");
            Security.addProvider(new BouncyCastleProvider());
            signer = Signature.getInstance("ECDSA", "BC");
        } catch (NoSuchAlgorithmException | NoSuchProviderException e) {
            e.printStackTrace();
        }
    }

    public static byte[] sha256Twice(byte[] input) {
        return sha256(sha256(input));
    }

    public static byte[] sha256(byte[] input) {
        return sha256.digest(input);
    }

    public static byte[] ripmd160(byte[] input) {
        return ripemd160.digest(input);
    }

    @SneakyThrows
    public static ECPrivateKey assemblePrivateKey(byte[] privateKeyBytes) {
        ECPrivateKeySpec privateKeySpec = new ECPrivateKeySpec(new BigInteger(privateKeyBytes), ecSpec);
        return (ECPrivateKey) keyFactory.generatePrivate(privateKeySpec);
    }

    @SneakyThrows
    public static ECPublicKey assemblePublicKey(byte[] publicKeyBytes) {
        ECPublicKeySpec publicKeySpec = new ECPublicKeySpec(ecSpec.getCurve().decodePoint(publicKeyBytes), ecSpec);
        return (ECPublicKey) keyFactory.generatePublic(publicKeySpec);
    }

    @SneakyThrows
    public static ECPublicKey generatePublicKey(ECPrivateKey privateKey) {
        ECPoint Q = ecSpec.getG().multiply(privateKey.getD());
        ECPublicKeySpec publicKeySpec = new ECPublicKeySpec(Q, ecSpec);
        return (ECPublicKey) keyFactory.generatePublic(publicKeySpec);
    }

    @SneakyThrows
    public static byte[] signTransaction(byte[] transactionHash, ECPrivateKey privateKey) {
        signer.initSign(privateKey);
        signer.update(transactionHash);
        return signer.sign();
    }

    @SneakyThrows
    public static boolean verifyTransaction(byte[] transactionHash, byte[] signedTransactionHash, ECPublicKey publicKey) {
        signer.initVerify(publicKey);
        signer.update(transactionHash);
        return signer.verify(signedTransactionHash);
    }

    public static byte[] getPublicKeyHashFromAddress(String address) {
        byte[] decoded = Base58.decode(address);
        return Arrays.copyOfRange(decoded, 1, decoded.length - 4);
    }

    public static byte[] hashPublicKeyBytes(byte[] publicKeyBytes) {
        return ripmd160(sha256(publicKeyBytes));
    }
    public static String getAddressFromPublicKey(ECPublicKey publicKey){
        byte[] b4 = new byte[21];
        System.arraycopy(CryptoUtils.ripmd160(CryptoUtils.sha256(publicKey.getQ().getEncoded())), 0, b4, 1, 20);
        byte[] b7 = Arrays.copyOf(CryptoUtils.sha256Twice(b4), 4);
        byte[] b8 = new byte[25];
        System.arraycopy(b4, 0, b8, 0, 21);
        System.arraycopy(b7, 0, b8, 21, 4);
        return Base58.encode(b8);
    }
}
