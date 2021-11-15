package net.homework.blockchain.client;

import net.homework.blockchain.Config;
import net.homework.blockchain.bean.Block;
import net.homework.blockchain.bean.Transaction;
import net.homework.blockchain.util.ByteUtils;
import net.homework.blockchain.util.VerifyUtils;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.util.HashMap;
import java.util.Map;

public class NodeImpl implements Node {

    // valid txs, to be processed by a miner
    public static Map<byte[], Transaction> TX_POOL = new HashMap<>();
    // because udp packets won't arrive in order, there'll be orphan transactions, waiting to be claimed by other txs
    public static Map<byte[], Transaction> ORPHAN_TXS = new HashMap<>();
    // orphan blocks
    public static Map<byte[], Block> ORPHAN_BLOCKS = new HashMap<>();

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
                        VerifyUtils.verifyTx(tx, TX_POOL, ORPHAN_TXS);
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
                    Block block = ByteUtils.fromBytes(data, new Block());
                    VerifyUtils.verifyBlock(block, packet.getAddress(), ORPHAN_BLOCKS, TX_POOL);
                }
                // TODO: gracefully exit loop, send loopback msg?
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();
    }
}
