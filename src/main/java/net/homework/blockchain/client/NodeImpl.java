package net.homework.blockchain.client;

import net.homework.blockchain.Config;
import net.homework.blockchain.bean.Transaction;
import net.homework.blockchain.sql.dao.TxDao;
import net.homework.blockchain.sql.dao.impl.TxDaoImpl;
import net.homework.blockchain.util.ByteUtils;
import net.homework.blockchain.util.VerifyUtils;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class NodeImpl implements Node {

    // valid txs, to be processed by a miner
    public static Map<byte[], Transaction> txPool = new HashMap<>();
    // orphan transactions, waiting to be claimed by other txs
    public static Map<byte[], Transaction> orphanTxs = new HashMap<>();
    private static final TxDao txDao = new TxDaoImpl();

    @Override
    public void listenForTransaction() {
        new Thread(() -> {
            try {
                DatagramSocket socket = new DatagramSocket(Config.PORT_TX_BROADCAST);
                byte[] data;
                DatagramPacket packet;
                while (!socket.isClosed()) {
                    data = new byte[32768];
                    packet = new DatagramPacket(data, data.length);
                    // blocking
                    socket.receive(packet);
                    Transaction tx = ByteUtils.fromBytes(data, new Transaction());
                    // TODO: verify this transaction
                    verifyTx(tx, packet.getLength());
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();
    }

    @Override
    public void listenForMiner() {

    }

    @Override
    public void broadcastNewBlock() {

    }

    @Override
    public void listenForNewBlock() {

    }

    @Override
    public void verifyTx(Transaction tx, int size) {
        /*
          Check syntactic correctness
          Make sure neither in or out lists are empty
         */
        if (tx == null || VerifyUtils.isListEmpty(tx.getInputs()) || VerifyUtils.isListEmpty(tx.getOutputs())) {
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
        if (outputSum < 0L)
            return;
        // Make sure none of the inputs have hash=0, n=-1 (coinbase transactions)
        for (Transaction.Input input : tx.getInputs()) {
            if (VerifyUtils.isCoinbaseInput(input)) {
                return;
            }
        }
        // Check that size in bytes >= 100
        if (size < 100) {
            return;
        }
        // Reject if we already have matching tx in the pool, or in a block in the main branch
        byte[] txHash = tx.hashTransaction();
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
                if (VerifyUtils.isOutputPresentInTx(txInPool, refOut, outIndex)) {
                    refOutTx = txInPool;
                }
            }
            // if it's still orphan, try to find the referenced output on-chain, if found, it's not orphan
            if (refOutTx == null) {
                Transaction findOnChain = txDao.getTx(refOut);
                refOutTx = findOnChain != null && VerifyUtils.isOutputPresentInTx(findOnChain, refOut, outIndex) ? findOnChain : null;
            }
            // add to the orphan txs if is orphan
            if (refOutTx == null) {
                orphanTxs.putIfAbsent(tx.hashTransaction(), tx);
                return;
            } else {
                // if the referenced output is coinbase, it must be matured, or we reject it
                if (VerifyUtils.isCoinbaseTx(refOutTx) && !VerifyUtils.isCoinbaseMature(refOutTx)) {
                    return;
                }
                // TODO: if the referenced output is spent or do not exist, reject it
                if (VerifyUtils.isOutputSpent()) {

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
        if (inputSum < 0L || inputSum <= outputSum)
            return;

        for (Transaction.Input input : tx.getInputs()) {
            // Verify the scriptPubKey accepts for each input; reject if any are bad
            if (!VerifyUtils.verifyInput(input))
                return;
        }


        // Add to transaction pool
        txPool.put(txHash, tx);
        // if it's a orphan who found parents, remove it from orphan pool
        orphanTxs.remove(txHash);
        // TODO: Relay transaction to peers

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
            if (flag)
                verifyTx(orphan, ByteUtils.toBytes(orphan).length);
        }
    }
}
