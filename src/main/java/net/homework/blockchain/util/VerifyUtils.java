package net.homework.blockchain.util;

import net.homework.blockchain.Config;
import net.homework.blockchain.bean.Block;
import net.homework.blockchain.bean.Transaction;
import net.homework.blockchain.sql.dao.TxDao;
import net.homework.blockchain.sql.dao.impl.TxDaoImpl;

import java.io.IOException;
import java.net.*;
import java.util.List;
import java.util.Map;

public class VerifyUtils {

    private static final TxDao txDao = new TxDaoImpl();

    public static boolean isListEmpty(List<?> list) {
        return list == null || list.isEmpty();
    }

    public static boolean isCoinbaseTx(Transaction tx) {
        if (tx.getInputs().size() == 1) {
            return isCoinbaseInput(tx.getInputs().get(0));
        } else {
            return false;
        }
    }

    public static boolean isCoinbaseInput(Transaction.Input input) {
        return ByteUtils.isZero(input.getPreviousTransactionHash()) && input.getOutIndex() == -1;
    }

    public static boolean isCoinbaseMature(Transaction refOutTx) {
        // TODO
        return false;
    }

    public static boolean isOutputPresentInTx(Transaction toCheck, byte[] refOut, int outIndex) {
        return toCheck.hashTransaction() == refOut && toCheck.getOutputs().size() > outIndex;
    }

    public static boolean verifyInput(Transaction.Input input) {
        return CryptoUtils.verifyTransaction(input.getScriptSig(), input.getScriptPubKey()) == input.getPreviousTransactionHash();
    }

    public static boolean isOutputSpent() {
        // TODO
        return false;
    }

    public static void verifyTx(Transaction tx, Map<byte[], Transaction> txPool, Map<byte[], Transaction> orphanTxs) {
        /*
          Check syntactic correctness
          Make sure neither in or out lists are empty
         */
        if (tx == null || isListEmpty(tx.getInputs()) || isListEmpty(tx.getOutputs())) {
            return;
        }
        // Each output value, as well as the total, must be in legal money range
        long outputSum = 0;
        long outputValue;
        for (Transaction.Output output : tx.getOutputs()) {
            // each output value is valid
            outputValue = output.getValue();
            if (outputValue < 0L) {
                return;
            } else {
                outputSum += outputValue;
            }
        }
        if (outputSum < 0L) {
            return;
        }
        // Make sure none of the inputs have hash=0, n=-1 (coinbase transactions)
        for (Transaction.Input input : tx.getInputs()) {
            if (isCoinbaseInput(input)) {
                return;
            }
        }
        // Reject if we already have matching tx in the pool, or in a block in the main branch
        byte[] txHash = tx.hashTransaction();
        // if it's a orphan who found parents, remove it from orphan pool
        orphanTxs.remove(txHash);
        if (txPool.containsKey(txHash) || txDao.getTx(txHash) != null) {
            return;
        }
        byte[] refOut;
        int outIndex;
        Transaction refOutTx;
        long inputSum = 0;
        long inputValue;
        // For each input,
        for (Transaction.Input input : tx.getInputs()) {
            refOut = input.getPreviousTransactionHash();
            outIndex = input.getOutIndex();
            refOutTx = null;
            for (Transaction txInPool : txPool.values()) {
                for (Transaction.Input inputInPool : txInPool.getInputs()) {
                    // if the referenced output exists in any other tx in the pool, reject this transaction.
                    if (inputInPool.getPreviousTransactionHash() == refOut && inputInPool.getOutIndex() == outIndex) {
                        return;
                    }
                }
                // find the referenced output transaction in pool, if present, this tx is not orphan
                if (isOutputPresentInTx(txInPool, refOut, outIndex)) {
                    refOutTx = txInPool;
                }
            }
            // if it's still orphan, try to find the referenced output on-chain, if found, it's not orphan
            if (refOutTx == null) {
                Transaction findOnChain = txDao.getTx(refOut);
                refOutTx = findOnChain != null && isOutputPresentInTx(findOnChain, refOut, outIndex) ? findOnChain : null;
            }
            // add to the orphan txs if is orphan
            if (refOutTx == null) {
                orphanTxs.putIfAbsent(tx.hashTransaction(), tx);
                // TODO: remove orphanTxs who stayed too long?
                return;
            } else {
                // if the referenced output is coinbase, it must be matured, or we reject it
                if (isCoinbaseTx(refOutTx) && !isCoinbaseMature(refOutTx)) {
                    return;
                }
                // if the referenced output is spent, reject it
                if (isOutputSpent()) {
                    return;
                }
                /*
                    Using the referenced output transactions to get input values,
                    check that each input value, as well as the sum, are in legal money range
                 */
                inputValue = refOutTx.getOutputs().get(outIndex).getValue();
                if (inputValue < 0L) {
                    return;
                } else {
                    inputSum += inputValue;
                }
            }

        }
        // Reject if the sum of input values < sum of output values
        // Reject if transaction fee (defined as sum of input values minus sum of output values) would be too low to get into an empty block
        if (inputSum < 0L || inputSum <= outputSum) {
            return;
        }

        for (Transaction.Input input : tx.getInputs()) {
            // Verify the scriptPubKey accepts for each input; reject if any are bad
            if (!verifyInput(input)) {
                return;
            }
        }


        // Add to transaction pool
        txPool.put(txHash, tx);

        // Broadcast transaction to nodes
        new Thread(() -> {
            try {
                DatagramSocket socket = new DatagramSocket(Config.PORT_TX_BROADCAST_OUT, InetAddress.getLocalHost());
                socket.setBroadcast(true);
                byte[] data = tx.toBytes();
                DatagramPacket packet = new DatagramPacket(data, data.length, InetAddress.getByName("255.255.255.255"), Config.PORT_TX_BROADCAST_IN);
                socket.send(packet);
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();

        /*
            For each orphan transaction that uses this one as one of its inputs,
            run all these steps (including this one) recursively on that orphan.
         */
        boolean flag;
        for (Transaction orphan : orphanTxs.values()) {
            flag = false;
            for (Transaction.Input input : orphan.getInputs()) {
                if (input.getPreviousTransactionHash() == txHash && tx.getOutputs().size() > input.getOutIndex()) {
                    flag = true;
                    break;
                }
            }
            if (flag) {
                verifyTx(orphan, txPool, orphanTxs);
            }
        }
    }

    public static void verifyBlock(Block block) {
        // TODO
    }
}
