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
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.ECPublicKey;
import java.security.spec.ECGenParameterSpec;
import java.security.spec.InvalidKeySpecException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import static net.homework.blockchain.util.ByteUtils.removeLeadingZero;
import static net.homework.blockchain.util.CryptoUtils.*;

public class UserImpl implements User {
    @Override
    public String generatePrivateKey() throws NoSuchAlgorithmException, InvalidAlgorithmParameterException, IOException {
        KeyPairGenerator kpg = KeyPairGenerator.getInstance("EC");
        ECGenParameterSpec curve = new ECGenParameterSpec("secp256k1");
        kpg.initialize(curve);
        KeyPair kp = kpg.genKeyPair();
        ECPrivateKey pri = (ECPrivateKey) kp.getPrivate();
        // 0 - Private ECDSA Key
        byte[] b0 = removeLeadingZero(pri.getS().toByteArray());
        char[] localPriKey = Hex.encodeHex(b0, false);
        FileWriter fileWriter = new FileWriter("E:/privateKey.key");
        BufferedWriter bufferedWriter = new BufferedWriter(fileWriter);
        bufferedWriter.write(String.valueOf(localPriKey));
        bufferedWriter.flush();
        bufferedWriter.close();
        fileWriter.close();
        return null;
    }
    @Override
    public BigInteger loadPrivateKey() throws IOException, DecoderException {
        FileReader fileReader = new FileReader("E:/privateKey.key");
        BufferedReader bufferedReader = new BufferedReader(fileReader);
        byte[] localKeyByte = Hex.decodeHex(bufferedReader.readLine());
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
        byte[] bytes = new byte[65];
        bytes[0] = 4;
        System.arraycopy(removeLeadingZero(publicKeyGenerated.getW().getAffineX().toByteArray()), 0, bytes, 1, 32);
        System.arraycopy(removeLeadingZero(publicKeyGenerated.getW().getAffineY().toByteArray()), 0, bytes, 33, 32);
        return Hex.encodeHex(bytes, false);
    }
    @Override
    public String getAddress(char[] publicKey) throws DecoderException {
        return Base58.encode(Hex.decodeHex(publicKey));
    }
    @Override
    public Transaction assembleTx(Map<byte[], byte[]> recipientsWithAmount) throws DecoderException, IOException, NoSuchAlgorithmException, InvalidKeySpecException, NoSuchProviderException {
        BigInteger praviteKey = loadPrivateKey();
        char[] publicKey = getPublicKey(praviteKey);
        Transaction transaction = new Transaction();
        Transaction.Input input = null ;
        input.setPreviousTransactionHash(getPreviousTransactionHash());
        input.setOutIndex(getOutIndex());
        input.setScriptSig(signTransaction(getPreviousTransactionHash(),assemblePrivateKey(removeLeadingZero(praviteKey.toByteArray()))));
        input.setScriptPubKey(Hex.decodeHex(publicKey));
        List<Transaction.Input> inputs = null;
        inputs.add(input);
        transaction.setInputs(inputs);
        return null;
    }
    @Override
    public void broadcastTx(Transaction tx) {
        new Thread(() ->{
            try {
                InetAddress address = InetAddress.getByName("localhost");
                int port = 6666;
                byte[] transactionHash = tx.getOutputs().get(0).getScriptPubKeyHash();
                long outIndex = tx.getOutputs().get(0).getValue();
                //我貌似没明白怎么同时传这两个数据，那就暂时先传tx的公钥哈希
                DatagramPacket packet = new DatagramPacket(transactionHash, transactionHash.length, address, port);
                DatagramSocket socket = new DatagramSocket();
                socket.send(packet);
            }catch (IOException e) {
                e.printStackTrace();
            }
        }).start();
    }
    @Override
    public Map<Transaction, Integer> getUTXOs() {

        return null;
    }
    public byte[] getPreviousTransactionHash(){
        return null;
    }
    public int getOutIndex(){
        return 0;
    }
    public long getValue(){
        return 0;
    }
    public byte[] getScriptPubKeyHash(){
        return null;
    }

}
