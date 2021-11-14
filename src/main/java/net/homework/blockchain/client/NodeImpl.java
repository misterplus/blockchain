package net.homework.blockchain.client;

import net.homework.blockchain.Config;
import net.homework.blockchain.bean.Transaction;
import net.homework.blockchain.util.ByteUtils;
import net.homework.blockchain.util.VerifyUtils;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.util.HashMap;
import java.util.Map;

public class NodeImpl implements Node {
    @Override
    public void listenForTransaction() {
        new Thread(() -> {
            try {
                DatagramSocket socket = new DatagramSocket(Config.PORT_TX_BROADCAST);
                byte[] data;
                DatagramPacket packet;
                while(!socket.isClosed()) {
                    data = new byte[32768];
                    packet = new DatagramPacket(data, data.length);
                    // blocking
                    socket.receive(packet);
                    Transaction tx = ByteUtils.fromBytes(data, new Transaction());
                    // TODO: verify this transaction
                    if (isTransactionValid(tx, packet.getLength())) {

                    }
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
    public boolean isTransactionValid(Transaction tx, int size) {
        // tx is syntactically correct && neither inputs and outputs are empty
        if (tx == null || VerifyUtils.isListEmpty(tx.getInputs()) || VerifyUtils.isListEmpty(tx.getOutputs()))
            return false;
        for (Transaction.Output output : tx.getOutputs()) {
            // each output value is valid
            if (output.getValue() < 0L)
                return false;
        }
        byte[] prevHash;
        int outIndex;
        for (Transaction.Input input : tx.getInputs()) {
            prevHash = input.getPreviousTransactionHash();
            outIndex = input.getOutIndex();
            // each input aren't a coinbase transaction input
            if (ByteUtils.isZero(prevHash) || outIndex == -1)
                return false;
            // reject if the referenced output by an input exists in any other tx in the pool
            for (Transaction txInPool : txPool.values()) {
                for (Transaction.Input inputInPool : txInPool.getInputs()) {
                    if (inputInPool.getPreviousTransactionHash() == prevHash && inputInPool.getOutIndex() == outIndex)
                        return false;
                }
            }
        }
        // size should be bigger or equal to 100
        if (size < 100)
            return false;
        // reject if already in the pool
        byte[] txHash = tx.hashTransaction();
        if (txPool.containsKey(txHash))
            return false;
        // TODO: reject if in a block on-chain
        //  fetch from database?

        return false;
    }

    // valid txs, to be processed by a miner
    public static Map<byte[], Transaction> txPool = new HashMap<>();
}
