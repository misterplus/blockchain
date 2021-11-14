package net.homework.blockchain.util;

import org.bouncycastle.jce.provider.BouncyCastleProvider;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.Security;

public class CryptoUtils {
    private static MessageDigest sha256;
    private static MessageDigest ripemd160;

    static {
        Security.addProvider(new BouncyCastleProvider());
        try {
            sha256 = MessageDigest.getInstance("SHA-256");
            ripemd160 = MessageDigest.getInstance("RIPEMD160");
        } catch (NoSuchAlgorithmException ignored) {}
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

    public static byte[] signTransaction(byte[] transactionHash, byte[] privateKey) {
        // TODO: return signedTransactionHash
    }

    public static byte[] verifyTransaction(byte[] signedTransactionHash, byte[] publicKey) {
        // TODO: return transactionHash
    }
}
