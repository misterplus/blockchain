package net.homework.blockchain.client;

import cn.hutool.http.HttpRequest;
import net.homework.blockchain.entity.Transaction;
import net.homework.blockchain.util.ByteUtils;
import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;

import java.io.*;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.security.*;
import java.security.spec.InvalidKeySpecException;
import java.util.*;
import java.util.stream.Collectors;

import static net.homework.blockchain.util.ByteUtils.removeLeadingZero;
import static net.homework.blockchain.util.CryptoUtils.assemblePrivateKey;
import static net.homework.blockchain.util.CryptoUtils.signTransaction;

public class Usertest {
    public static void main(String[] args) throws IOException, NoSuchAlgorithmException, InvalidAlgorithmParameterException, DecoderException, NoSuchProviderException, InvalidKeySpecException {
        UserImpl userImpl = new UserImpl();
        if(args[0].equals("1")){
            userImpl.generatePrivateKey();
        }
        BigInteger localKeyBig = userImpl.loadPrivateKey();
        char[] publicKey = userImpl.getPublicKey(localKeyBig);
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
        userImpl.broadcastTx(transaction);
    }
}
