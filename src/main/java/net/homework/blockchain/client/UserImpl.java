package net.homework.blockchain.client;

import cn.hutool.http.HttpRequest;
import lombok.SneakyThrows;
import net.homework.blockchain.entity.Transaction;
import net.homework.blockchain.util.ByteUtils;
import net.homework.blockchain.util.CryptoUtils;
import net.homework.blockchain.util.MsgUtils;
import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;
import org.bouncycastle.jce.ECNamedCurveTable;
import org.bouncycastle.jce.interfaces.ECPrivateKey;
import org.bouncycastle.jce.interfaces.ECPublicKey;
import org.bouncycastle.jce.provider.BouncyCastleProvider;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.nio.ByteBuffer;
import java.security.*;

import java.util.*;
import java.util.stream.Collectors;

import static net.homework.blockchain.Config.*;
import static net.homework.blockchain.util.CryptoUtils.generatePublicKey;
import static net.homework.blockchain.util.CryptoUtils.signTransaction;
import static net.homework.blockchain.util.NetworkUtils.broadcast;

public class UserImpl implements User {

    private ECPrivateKey privateKey;
    private ECPublicKey publicKey;
    private String node;

    @SneakyThrows
    public static void main(String[] args){
        UserImpl userImpl = new UserImpl();
//        if (args[0].equals("1")) {
//            userImpl.privateKey = userImpl.generatePrivateKey();
//            userImpl.savePrivateKey(userImpl.privateKey);
//        }
        userImpl.privateKey = CryptoUtils.assemblePrivateKey(Hex.decodeHex(args[0]));
        userImpl.publicKey = generatePublicKey(userImpl.privateKey);
        userImpl.node = args[1];
        Map<byte[], Long> recipientsWithAmount = new HashMap<>();
        recipientsWithAmount.put(CryptoUtils.getPublicKeyHashFromAddress(args[3]), Long.parseLong(args[2]));
        Transaction transaction = userImpl.assembleTx(recipientsWithAmount);
        userImpl.broadcastTx(transaction);
    }

    @Override
    public ECPrivateKey generatePrivateKey(){
        Security.addProvider(new BouncyCastleProvider());
        KeyPairGenerator kpg = null;
        try {
            kpg = KeyPairGenerator.getInstance("ECDSA", "BC");
            kpg.initialize(ECNamedCurveTable.getParameterSpec("secp256k1"));
        } catch (NoSuchAlgorithmException | NoSuchProviderException | InvalidAlgorithmParameterException e) {
            e.printStackTrace();
        }
        if (kpg != null) {
            return (ECPrivateKey) kpg.genKeyPair().getPrivate();
        }
        return null;
    }

    public void savePrivateKey(ECPrivateKey privateKey){
        byte[] privateKeyBytes = privateKey.getD().toByteArray();
        try {
            FileOutputStream out = new FileOutputStream("./privateKey.key");
            out.write(privateKeyBytes);
            out.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    @Override
    public ECPrivateKey loadPrivateKey() throws IOException {
        FileInputStream input = new FileInputStream("./privateKey.key");
        byte[] bytes = new byte[32];
        input.read(bytes);
        ECPrivateKey privateKey = CryptoUtils.assemblePrivateKey(bytes);
        input.close();
        return privateKey;
    }


    @Override
    public Transaction assembleTx(Map<byte[], Long> recipientsWithAmount) {
        Transaction transaction = new Transaction();
        List<Transaction.Input> inputs = new ArrayList<>();
        List<Transaction.Output> outputs = new ArrayList<>();
        getUTXOs().forEach((byteBuffer, list) -> {
            for (Integer integer : list) {
                Transaction.Input input = new Transaction.Input();
                byte[] previousTransactionHash = byteBuffer.array();
                input.setPreviousTransactionHash(previousTransactionHash);
                input.setOutIndex(integer);
                input.setScriptSig(signTransaction(previousTransactionHash, integer, privateKey));
                input.setScriptPubKey(publicKey.getQ().getEncoded());
                inputs.add(input);
            }
        });
        recipientsWithAmount.forEach((bytes, value) -> {
            Transaction.Output output = new Transaction.Output();
            output.setValue(value);
            output.setScriptPubKeyHash(bytes);
            outputs.add(output);
        });
        transaction.setInputs(inputs);
        transaction.setOutputs(outputs);
        return transaction;
    }

    @Override
    public void broadcastTx(Transaction tx) {
        new Thread(() -> {
            try {
                broadcast(new DatagramSocket(PORT_USER_OUT), tx.toMsg(), PORT_NODE_IN);
                byte[] receive = new byte[1];
                DatagramPacket packet = new DatagramPacket(receive, receive.length);
                new DatagramSocket(PORT_USER_IN).receive(packet);
                switch (receive[0]) {
                    case MsgUtils.TX_ACCEPTED: {
                        System.out.println("Tx accpeted");
                        break;
                    }
                    case MsgUtils.TX_REJECTED: {
                        System.out.println("Tx rejected");
                        break;
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();
    }

    @Override
    public Map<ByteBuffer, List<Integer>> getUTXOs() {
        String result = HttpRequest.get(node + ":8080/wallet/utxo")
                .form("publicKey", Hex.encodeHexString(publicKey.getQ().getEncoded()))
                .execute().body();
        Map<String, String> map = ByteUtils.fromJson(result, new HashMap<>());
        Map<ByteBuffer, List<Integer>> byteBufferListMap = new HashMap<>();
        if (map != null) {
            map.forEach((key, list) -> {
                try {
                    byteBufferListMap.put(ByteBuffer.wrap(Hex.decodeHex(key)), Arrays.stream(list.split(",")).map(Integer::parseInt).collect(Collectors.toList()));
                } catch (DecoderException e) {
                    e.printStackTrace();
                }
            });
        }
        return byteBufferListMap;
    }
}