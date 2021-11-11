package net.homework.blockchain;

import org.apache.commons.codec.binary.Hex;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

public class Test {
    public static void main(String[] args) throws NoSuchAlgorithmException, UnsupportedEncodingException {
        KeyGenerator keyGen = KeyGenerator.getInstance("AES");
        keyGen.init(256);
        // 256-bit private key, identification for a wallet
        SecretKey privateKey = keyGen.generateKey();
        // 32-byte representation
        byte[] privateBytes = privateKey.getEncoded();
        byte[] withPrefix = new byte[37];
        withPrefix[0] = (byte) 0x80;
        System.arraycopy(privateBytes, 0, withPrefix, 1, 32);
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hash = digest.digest(digest.digest(Arrays.copyOf(privateBytes, 33)));
        System.arraycopy(hash, 0, withPrefix, 33, 4);
    }
}
