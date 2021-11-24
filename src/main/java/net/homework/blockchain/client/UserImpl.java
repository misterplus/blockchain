package net.homework.blockchain.client;

import cn.hutool.http.HttpUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.fasterxml.jackson.databind.util.JSONPObject;
import io.leonard.Base58;
import lombok.SneakyThrows;
import net.homework.blockchain.entity.Transaction;
import net.homework.blockchain.util.ByteUtils;
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
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.ECPublicKey;
import java.security.spec.ECGenParameterSpec;
import java.security.spec.InvalidKeySpecException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import static net.homework.blockchain.util.NetworkUtils.*;
import static net.homework.blockchain.Config.*;
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
        FileWriter fileWriter = new FileWriter("../privateKey.key");
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
    public Transaction assembleTx(Map<byte[], Long> recipientsWithAmount) throws DecoderException, IOException, NoSuchAlgorithmException, InvalidKeySpecException, NoSuchProviderException {
        BigInteger praviteKey = loadPrivateKey();
        char[] publicKey = getPublicKey(praviteKey);
        Transaction transaction = new Transaction();
        List<Transaction.Input> inputs = null;
        List<Transaction.Output> outputs = null;
        Map<ByteBuffer,List<Integer>> byteBufferListMap = null;
        for(Map.Entry<ByteBuffer,List<Integer>> entry:byteBufferListMap.entrySet()){
            for (int i = 0; i < entry.getValue().size(); i++) {
                Transaction.Input input = null;
                entry.getKey().flip();
                int length = entry.getKey().limit() - entry.getKey().position();
                byte [] previousTransactionHash = new byte[length];
                for (int j=0;j<previousTransactionHash.length;j++){
                    previousTransactionHash[i]=entry.getKey().get();
                }
                input.setPreviousTransactionHash(previousTransactionHash);
                input.setOutIndex(entry.getValue().get(i));
                input.setScriptSig(signTransaction(previousTransactionHash, assemblePrivateKey(removeLeadingZero(praviteKey.toByteArray()))));
                input.setScriptPubKey(Hex.decodeHex(publicKey));
                inputs.add(input);
            }
        }
        for(Map.Entry<byte[],Long> entry:recipientsWithAmount.entrySet()){
            Transaction.Output output = null;
            output.setValue(entry.getValue());
            output.setScriptPubKeyHash(entry.getKey());
            outputs.add(output);
        }
        transaction.setInputs(inputs);
        transaction.setOutputs(outputs);
        return transaction;
    }
    @Override
    public void broadcastTx(Transaction tx) {
        new Thread(() ->{
            try {
                DatagramSocket portUserOut =new DatagramSocket(PORT_USER_OUT);
                byte[] transactionHash = tx.getHashTx();
                broadcast(portUserOut,transactionHash,PORT_NODE_IN);
            }catch (IOException e) {
                e.printStackTrace();
            }
        }).start();
        new Thread(() ->{
            try {
                DatagramSocket portUserIn =new DatagramSocket(PORT_USER_IN);
                byte[] receive = new byte[1024];
                DatagramPacket packet = new DatagramPacket(receive,receive.length);
                portUserIn.receive(packet);
            }catch (IOException e) {
                e.printStackTrace();
            }
        }).start();
    }
    @Override
    public Map<ByteBuffer, List<Integer>> getUTXOs() {
        String result = HttpUtil.get(getUrl());
        Map<ByteBuffer, List<Integer>> map = new HashMap<>();
        map = ByteUtils.fromJson(result,map);
        return map;
    }
    public String getUrl(){
        return "";
    }
}
