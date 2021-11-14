package net.homework.blockchain.client;

import io.leonard.Base58;
import lombok.SneakyThrows;
import net.homework.blockchain.bean.Transaction;
import org.apache.commons.codec.binary.Hex;
import org.bouncycastle.jce.provider.BouncyCastleProvider;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.MessageDigest;
import java.security.Security;
import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.ECPublicKey;
import java.security.spec.ECGenParameterSpec;
import java.util.Arrays;

import static net.homework.blockchain.util.ByteUtils.removeLeadingZero;

public class UserImpl implements User {
    @SneakyThrows
    @Override
    public String generatePriKey() {
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
        MessageDigest sha256 = MessageDigest.getInstance("SHA-256");
        System.out.print("1: ");
        System.out.println(Hex.encodeHex(b1, false));

        // 2 - SHA-256 hash of 1
        byte[] b2 = sha256.digest(b1);
        System.out.print("2: ");
        System.out.println(Hex.encodeHex(b2, false));

        MessageDigest ripemd160 = MessageDigest.getInstance("RIPEMD160");
        // 3 - RIPEMD-160 Hash of 2
        byte[] b3 = ripemd160.digest(b2);
        System.out.print("3: ");
        System.out.println(Hex.encodeHex(b3, false));

        // 4 - Adding network bytes to 3
        byte[] b4 = new byte[21];
        System.arraycopy(b3, 0, b4, 1, 20);
        System.out.print("4: ");
        System.out.println(Hex.encodeHex(b4, false));

        // 5 - SHA-256 hash of 4
        byte[] b5 = sha256.digest(b4);
        System.out.print("5: ");
        System.out.println(Hex.encodeHex(b5, false));

        // 6 - SHA-256 hash of 5
        byte[] b6 = sha256.digest(b5);
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
    public Transaction assembleTx() {
        return null;
    }

    @Override
    public void broadcastTx(Transaction tx) {

    }
}
