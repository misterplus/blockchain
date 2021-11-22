package net.homework.blockchain;

import net.homework.blockchain.client.User;
import net.homework.blockchain.client.UserImpl;
import net.homework.blockchain.entity.Block;
import net.homework.blockchain.entity.Transaction;
import net.homework.blockchain.util.ByteUtils;
import net.homework.blockchain.util.CryptoUtils;
import org.apache.commons.codec.binary.Hex;

import java.util.Collections;
import java.util.List;

public class Test {
    public static void main(String[] args) {
        long timeStart = System.currentTimeMillis();
        Transaction.Input input = new Transaction.Input();
        input.setPreviousTransactionHash(new byte[]{0});
        input.setOutIndex(-1);
        input.setScriptSig(new byte[]{0});
        input.setScriptPubKey(new byte[]{0});
        Transaction.Output output = new Transaction.Output();
        output.setValue(Config.BLOCK_FEE);
        output.setScriptPubKeyHash(CryptoUtils.getPublicKeyHashFromAddress("16SChybffW7NEM7L9Nq78K2PQTV2NPCEFn"));
        List<Transaction> txs = Collections.singletonList(new Transaction(Collections.singletonList(input), Collections.singletonList(output)));
        Block block = new Block(new byte[]{0}, txs);
        block.getHeader().setNonce(4913801);
        block.getHeader().setTime(1637148832393L);

        System.out.println(Hex.encodeHexString(txs.get(0).hashTransaction(), false));

//        while (!block.isBlockValid()) {
//            block.increment();
//        }
//        long timeEnd = System.currentTimeMillis();
//        System.out.print("Found solution: ");
//        System.out.println(Hex.encodeHexString(block.hashHeader(), false));
//        System.out.print("Time spent: ");
//        System.out.println((timeEnd - timeStart) / 1000 + " seconds");
//        System.out.print("Nonce: ");
//        System.out.println(block.getHeader().getNonce());
//        System.out.print("ExtraNonce: ");
//        System.out.println(block.getTransactions().get(0).getInputs().get(0).getOutIndex());
//        System.out.print("Block timestamp: ");
//        System.out.println(block.getHeader().getTime());
    }
}
