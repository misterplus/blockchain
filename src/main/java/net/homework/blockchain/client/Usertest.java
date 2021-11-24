package net.homework.blockchain.client;

import cn.hutool.http.HttpRequest;
import net.homework.blockchain.entity.Transaction;
import net.homework.blockchain.util.ByteUtils;
import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;
import org.bouncycastle.jce.ECNamedCurveTable;
import org.bouncycastle.jce.spec.ECParameterSpec;

import java.io.*;
import java.math.BigInteger;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.nio.ByteBuffer;
import java.security.*;
import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.ECPublicKey;
import java.security.spec.ECGenParameterSpec;
import java.security.spec.InvalidKeySpecException;
import java.util.*;
import java.util.stream.Collectors;

import static net.homework.blockchain.Config.*;
import static net.homework.blockchain.util.ByteUtils.removeLeadingZero;
import static net.homework.blockchain.util.CryptoUtils.assemblePrivateKey;
import static net.homework.blockchain.util.CryptoUtils.signTransaction;
import static net.homework.blockchain.util.NetworkUtils.broadcast;

public class Usertest {
    public static void main(String[] args) throws IOException, NoSuchAlgorithmException, InvalidAlgorithmParameterException, DecoderException, NoSuchProviderException, InvalidKeySpecException {
        if(args[0].equals("1")){
            KeyPairGenerator kpg = KeyPairGenerator.getInstance("EC");
            kpg.initialize(new ECGenParameterSpec("secp256k1"));
            ECPrivateKey pri = (ECPrivateKey) kpg.genKeyPair().getPrivate();
            char[] localPriKey = Hex.encodeHex(removeLeadingZero(pri.getS().toByteArray()), false);
            FileWriter fileWriter = new FileWriter("./privateKey.key");
            BufferedWriter bufferedWriter = new BufferedWriter(fileWriter);
            bufferedWriter.write(String.valueOf(localPriKey));
            bufferedWriter.flush();
            bufferedWriter.close();
            fileWriter.close();
        }
        BigInteger localKeyBig = new BigInteger(Hex.decodeHex(new BufferedReader(new FileReader("./privateKey.key")).readLine()));
        ECParameterSpec eCParameterSpec = ECNamedCurveTable.getParameterSpec("secp256k1");
        ECPublicKey publicKeyGenerated = (ECPublicKey) KeyFactory.getInstance("ECDSA","BC").generatePublic(new org.bouncycastle.jce.spec.ECPublicKeySpec(eCParameterSpec.getG().multiply(localKeyBig), eCParameterSpec));
        byte[] bytes = new byte[65];
        bytes[0] = 4;
        System.arraycopy(removeLeadingZero(publicKeyGenerated.getW().getAffineX().toByteArray()), 0, bytes, 1, 32);
        System.arraycopy(removeLeadingZero(publicKeyGenerated.getW().getAffineY().toByteArray()), 0, bytes, 33, 32);
        char[] publicKey = Hex.encodeHex(bytes, false);
        String result = HttpRequest.get("localhost:"+args[1]+"/wallet/utxo")
                .form("publicKey",args[3])
                .execute().body();
        Map<String, String> map = ByteUtils.fromJson(result,new HashMap<>());
        Map<ByteBuffer, List<Integer>> byteBufferListMap = new HashMap<>();
        map.forEach((key, list) -> {
            try {
                byteBufferListMap.put(ByteBuffer.wrap(Hex.decodeHex(key)),Arrays.stream(list.split(",")).map(Integer::parseInt).collect(Collectors.toList()));
            }catch (DecoderException e){
                e.printStackTrace();
            }
        });
        Transaction transaction = new Transaction();
        List<Transaction.Input> inputs = new ArrayList<>();
        List<Transaction.Output> outputs = new ArrayList<>();
        byteBufferListMap.forEach((byteBuffer,list) ->{
            for (int i = 0; i < list.size(); i++) {
                Transaction.Input input = new Transaction.Input();
                byteBuffer.flip();
                byte [] previousTransactionHash = new byte[byteBuffer.limit() - byteBuffer.position()];
                for (int j=0;j<previousTransactionHash.length;j++){
                    previousTransactionHash[i]=byteBuffer.get();
                }
                input.setPreviousTransactionHash(previousTransactionHash);
                input.setOutIndex(list.get(i));
                input.setScriptSig(signTransaction(previousTransactionHash, assemblePrivateKey(removeLeadingZero(localKeyBig.toByteArray()))));
                try {
                    input.setScriptPubKey(Hex.decodeHex(publicKey));
                } catch (DecoderException e) {
                    e.printStackTrace();
                }
                inputs.add(input);
            }
        });
        Transaction.Output output = new Transaction.Output();
        output.setValue(Long.parseLong(args[2]));
        output.setScriptPubKeyHash(Hex.decodeHex(args[3]));
        outputs.add(output);
        transaction.setInputs(inputs);
        transaction.setOutputs(outputs);
        new Thread(() ->{
            try {
                broadcast(new DatagramSocket(PORT_USER_OUT),transaction.getHashTx(),PORT_NODE_IN);
            }catch (IOException e) {
                e.printStackTrace();
            }
        }).start();
        new Thread(() ->{
            try {
                byte[] receive = new byte[1024];
                DatagramPacket packet = new DatagramPacket(receive,receive.length);
                new DatagramSocket(PORT_USER_IN).receive(packet);
            }catch (IOException e) {
                e.printStackTrace();
            }
        }).start();
    }
}
