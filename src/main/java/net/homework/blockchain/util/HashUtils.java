package net.homework.blockchain.util;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class HashUtils {
    private static MessageDigest sha256;

    static {
        try { sha256 = MessageDigest.getInstance("SHA-256"); } catch (NoSuchAlgorithmException ignored) {}
    }

    public static byte[] sha256Twice(byte[] input) {
        return sha256(sha256(input));
    }

    public static byte[] sha256(byte[] input) {
        return sha256.digest(input);
    }
}
