package net.homework.blockchain.client;

import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;
import org.bouncycastle.jce.ECNamedCurveTable;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.jce.spec.ECParameterSpec;
import org.bouncycastle.math.ec.ECPoint;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.math.BigInteger;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.Security;
import java.security.interfaces.ECPublicKey;
import java.security.spec.InvalidKeySpecException;

import static net.homework.blockchain.util.ByteUtils.removeLeadingZero;

public class GenPubKey {
    public static void main(String args[]) throws IOException, DecoderException, NoSuchAlgorithmException, NoSuchProviderException, InvalidKeySpecException {
        FileReader fileReader = new FileReader("./privateKey.key");
        BufferedReader bufferedReader = new BufferedReader(fileReader);
        byte[] localKeyByte = Hex.decodeHex(bufferedReader.readLine());
        BigInteger localKeyBig = new BigInteger(localKeyByte);
        Security.addProvider(new BouncyCastleProvider());
        KeyFactory keyFactory = KeyFactory.getInstance("ECDSA","BC");
        ECParameterSpec eCParameterSpec = ECNamedCurveTable.getParameterSpec("secp256k1");
        ECPoint Q = eCParameterSpec.getG().multiply(localKeyBig);
        org.bouncycastle.jce.spec.ECPublicKeySpec pubSpec = new org.bouncycastle.jce.spec.ECPublicKeySpec(Q, eCParameterSpec);
        ECPublicKey publicKeyGenerated = (ECPublicKey) keyFactory.generatePublic(pubSpec);
        byte[] bytes = new byte[65];
        bytes[0] = 4;
        System.arraycopy(removeLeadingZero(publicKeyGenerated.getW().getAffineX().toByteArray()), 0, bytes, 1, 32);
        System.arraycopy(removeLeadingZero(publicKeyGenerated.getW().getAffineY().toByteArray()), 0, bytes, 33, 32);
        System.out.println(Hex.encodeHex(bytes, false));
    }
}
