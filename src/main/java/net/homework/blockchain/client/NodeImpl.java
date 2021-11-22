package net.homework.blockchain.client;

import net.homework.blockchain.Config;
import net.homework.blockchain.SpringContext;
import net.homework.blockchain.entity.Block;
import net.homework.blockchain.entity.Transaction;
import net.homework.blockchain.entity.WrappedTransaction;
import net.homework.blockchain.service.BlockchainService;
import net.homework.blockchain.util.ByteUtils;
import net.homework.blockchain.util.MsgUtils;
import net.homework.blockchain.util.NetworkUtils;
import net.homework.blockchain.util.VerifyUtils;
import org.apache.commons.codec.binary.Hex;

import java.io.IOException;
import java.net.*;
import java.nio.ByteBuffer;
import java.util.*;

public class NodeImpl implements Node {

    private static final BlockchainService blockchainService = SpringContext.getBean(BlockchainService.class);

    // valid txs, to be processed by a miner
    public static Map<ByteBuffer, WrappedTransaction> TX_POOL = new HashMap<>();
    // because udp packets won't arrive in order, there'll be orphan transactions, waiting to be claimed by other txs
    public static Map<ByteBuffer, Transaction> ORPHAN_TXS = new HashMap<>();
    // orphan blocks
    public static Map<ByteBuffer, Block> ORPHAN_BLOCKS = new HashMap<>();

    private final List<ListeningThread> threads = new ArrayList<>();

    @Override
    public void listenForTransaction() {
        ListeningThread thread = new ListeningThread(Config.PORT_TX_BROADCAST_IN) {
            @Override
            public void digest(byte[] data, DatagramPacket packet) {
                Transaction tx = ByteUtils.fromBytes(data, new Transaction());
                String hashString = tx == null ? "null" : Hex.encodeHexString(tx.hashTransaction(), false);
                LOGGER.info(String.format("Node - Received transaction with hash %s from %s", hashString, packet.getAddress().toString()));
                boolean accepted = false;
                // Check that size in bytes >= 100
                if (packet.getLength() >= 100) {
                    // verify this transaction
                    accepted = VerifyUtils.verifyTx(tx, TX_POOL, ORPHAN_TXS);
                }
                // send accepted msg back
                NetworkUtils.sendPacket(packet.getAddress(), Config.PORT_MSG_OUT, MsgUtils.toTxMsg(accepted), Config.PORT_MSG_IN);
                LOGGER.info(String.format("Node - %s transaction with hash %s from %s", accepted ? "Accepted" : "Rejected", hashString, packet.getAddress().toString()));
            }
        };
        threads.add(thread);
        thread.start();
    }

    @Override
    public void listenForNewBlock() {
        ListeningThread thread = new ListeningThread(Config.PORT_BLOCK_BROADCAST_IN) {
            @Override
            public void digest(byte[] data, DatagramPacket packet) {
                Block block = ByteUtils.fromBytes(data, new Block());
                String hashString = block == null ? "null" : Hex.encodeHexString(block.hashHeader(), false);
                LOGGER.info(String.format("Node - Received block with hash %s from %s", hashString, packet.getAddress().toString()));
                boolean accepted = VerifyUtils.verifyBlock(block, packet.getAddress(), ORPHAN_BLOCKS, TX_POOL);
                NetworkUtils.sendPacket(packet.getAddress(), Config.PORT_MSG_OUT, MsgUtils.toBlockMsg(accepted), Config.PORT_MSG_IN);
                LOGGER.info(String.format("Node - %s block with hash %s from %s", accepted ? "Accepted" : "Rejected", hashString, packet.getAddress().toString()));
            }
        };
        threads.add(thread);
        thread.start();
    }

    @Override
    public void listenForMsg() {
        ListeningThread thread = new ListeningThread(Config.PORT_MSG_IN) {
            @Override
            public void digest(byte[] data, DatagramPacket packet) {
                // only listen for BLOCK_REQUEST
                if (MsgUtils.isBlockRequestMsg(data)) {
                    byte[] headerHash = Arrays.copyOfRange(data, 1, data.length);
                    LOGGER.info(String.format("Node - Received block query with hash %s from %s", Hex.encodeHexString(headerHash, false), packet.getAddress().toString()));
                    byte[] blockBytes = blockchainService.getBlockOnChainByHash(headerHash).toBytes();
                    NetworkUtils.sendPacket(packet.getAddress(), Config.PORT_BLOCK_BROADCAST_OUT, blockBytes, Config.PORT_BLOCK_BROADCAST_IN);
                }
            }
        };
        threads.add(thread);
        thread.start();
    }

    public void gracefulShutdown() {
        LOGGER.info("Node - Shutdown initiated...");
        for (ListeningThread t : threads) {
            try {
                // if a thread is blocked by receive, interrupt it (no processing in place, we're fine)
                t.close();
                t.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        LOGGER.info("Node - Shutdown completed.");
    }

    @Override
    public void init() {
        LOGGER.info("Node - Starting...");
        this.listenForMsg();
        this.listenForNewBlock();
        this.listenForTransaction();
        LOGGER.info("Node - Start completed.");
    }

    private static abstract class ListeningThread extends Thread {
        private final int portIn;
        private DatagramSocket socket;

        public ListeningThread(int portIn) {
            this.portIn = portIn;
        }

        public abstract void digest(byte[] data, DatagramPacket packet);

        public void close() {
            this.socket.close();
        }

        @Override
        public void run() {
            try {
                socket = new DatagramSocket(portIn);
                byte[] data;
                DatagramPacket packet;
                while (!socket.isClosed()) {
                    data = new byte[32768];
                    packet = new DatagramPacket(data, data.length);
                    // blocking
                    socket.receive(packet);
                    // skips localhost packets
                    InetAddress address = packet.getAddress();
                    if (address.isAnyLocalAddress() || address.isLoopbackAddress() || NetworkInterface.getByInetAddress(address) != null) {
                        continue;
                    }
                    byte[] finalData = data;
                    DatagramPacket finalPacket = packet;
                    new Thread(() -> digest(finalData, finalPacket)).start();
                }
            } catch (IOException e) {

            }
        }
    }
}
