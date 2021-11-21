package net.homework.blockchain.client;

import net.homework.blockchain.Config;
import net.homework.blockchain.entity.Block;
import net.homework.blockchain.entity.Transaction;
import net.homework.blockchain.entity.WrappedTransaction;
import net.homework.blockchain.util.ByteUtils;
import net.homework.blockchain.util.MsgUtils;
import net.homework.blockchain.util.NetworkUtils;
import net.homework.blockchain.util.VerifyUtils;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;

public class NodeImpl implements Node {

    // valid txs, to be processed by a miner
    public static Map<ByteBuffer, WrappedTransaction> TX_POOL = new HashMap<>();
    // because udp packets won't arrive in order, there'll be orphan transactions, waiting to be claimed by other txs
    public static Map<ByteBuffer, Transaction> ORPHAN_TXS = new HashMap<>();
    // orphan blocks
    public static Map<ByteBuffer, Block> ORPHAN_BLOCKS = new HashMap<>();

    @Override
    public void listenForTransaction() {
        new Thread(() -> {
            try {
                DatagramSocket socket = new DatagramSocket(Config.PORT_TX_BROADCAST_IN);
                byte[] data;
                DatagramPacket packet;
                while (!socket.isClosed()) {
                    data = new byte[32768];
                    packet = new DatagramPacket(data, data.length);
                    // blocking
                    socket.receive(packet);
                    Transaction tx = ByteUtils.fromBytes(data, new Transaction());
                    // Check that size in bytes >= 100
                    if (packet.getLength() >= 100) {
                        // verify this transaction
                        boolean accepted = VerifyUtils.verifyTx(tx, TX_POOL, ORPHAN_TXS);

                        // TODO: send accepted msg back
                    }
                }
                // TODO: gracefully exit loop, send loopback msg?
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();
    }

    @Override
    public void listenForMiner() {
        // TODO: listen for miner connections, feed them TX_POOL
    }

    @Override
    public void listenForNewBlock() {
        new Thread(() -> {
            try {
                DatagramSocket socket = new DatagramSocket(Config.PORT_BLOCK_BROADCAST_IN);
                byte[] data;
                DatagramPacket packet;
                while (!socket.isClosed()) {
                    data = new byte[32768];
                    packet = new DatagramPacket(data, data.length);
                    // blocking
                    socket.receive(packet);
                    byte[] finalData = data;
                    DatagramPacket finalPacket = packet;
                    new Thread(() -> {
                        Block block = ByteUtils.fromBytes(finalData, new Block());
                        boolean accepted = VerifyUtils.verifyBlock(block, finalPacket.getAddress(), ORPHAN_BLOCKS, TX_POOL);
                        byte[] resp = new byte[]{accepted ? MsgUtils.BLOCK_ACCEPTED : MsgUtils.BLOCK_REJECTED};
                        NetworkUtils.sendPacket(finalPacket.getAddress(), Config.PORT_MSG_OUT, resp, Config.PORT_MSG_IN);
                    }).start();
                }
                // TODO: gracefully exit loop, send loopback msg?
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();
    }
}
