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
    public static final Map<ByteBuffer, WrappedTransaction> TX_POOL = new HashMap<>();
    // because udp packets won't arrive in order, there'll be orphan transactions, waiting to be claimed by other txs
    public static final Map<ByteBuffer, Transaction> ORPHAN_TXS = new HashMap<>();
    // orphan blocks
    public static final Map<ByteBuffer, Block> ORPHAN_BLOCKS = new HashMap<>();

    private ListeningThread listeningThread;
    private final DatagramSocket socketOut = new DatagramSocket(Config.PORT_NODE_OUT);
    private static final Set<InetAddress> PEERS = new HashSet<>();

    public NodeImpl() throws SocketException {
    }

    @Override
    public void listenForMsg() {
        try {
            listeningThread = new ListeningThread() {
                @Override
                public void digest(byte[] data, DatagramPacket packet) {
                    switch (data[0]) {
                        case MsgUtils.TX_NEW: {
                            Transaction tx = ByteUtils.fromBytes(Arrays.copyOfRange(data, 1, data.length), new Transaction());
                            String hashString = tx == null ? "null" : tx.hashTransactionHex();
                            LOGGER.info(String.format("Received transaction with hash %s from %s", hashString, packet.getAddress().toString()));
                            boolean accepted = false;
                            // Check that size in bytes >= 100
                            if (packet.getLength() >= 100) {
                                // verify this transaction
                                accepted = VerifyUtils.verifyTx(tx, TX_POOL, ORPHAN_TXS, socketOut, PEERS);
                            }
                            // send accepted msg back
                            NetworkUtils.sendPacket(socketOut, MsgUtils.toTxMsg(accepted), packet.getAddress(), Config.PORT_USER_IN);
                            LOGGER.info(String.format("%s transaction with hash %s from %s", accepted ? "Accepted" : "Rejected", hashString, packet.getAddress().toString()));
                            break;
                        }
                        case MsgUtils.BLOCK_NEW: {
                            Block block = ByteUtils.fromBytes(Arrays.copyOfRange(data, 1, data.length), new Block());
                            String hashString = block == null ? "null" : block.hashHeaderHex();
                            LOGGER.info(String.format("Received block with hash %s from %s", hashString, packet.getAddress().toString()));
                            boolean accepted = VerifyUtils.verifyBlock(block, packet.getAddress(), ORPHAN_BLOCKS, TX_POOL, socketOut, PEERS);
                            NetworkUtils.sendPacket(socketOut, MsgUtils.toBlockMsg(accepted), packet.getAddress(), Config.PORT_MINER_IN);
                            LOGGER.info(String.format("%s block with hash %s from %s", accepted ? "Accepted" : "Rejected", hashString, packet.getAddress().toString()));
                            break;
                        }
                        case MsgUtils.BLOCK_REQUESTED: {
                            byte[] headerHash = Arrays.copyOfRange(data, 1, data.length);
                            LOGGER.info(String.format("Received block query with hash %s from %s", Hex.encodeHexString(headerHash, false), packet.getAddress().toString()));
                            byte[] blockMsg = blockchainService.getBlockOnChainByHash(headerHash).toMsg();
                            NetworkUtils.sendPacket(socketOut, blockMsg, packet.getAddress(), Config.PORT_NODE_IN);
                            break;
                        }
                        case MsgUtils.PEER: {
                            LOGGER.info(String.format("Received peer gossip from %s, adding it to peers...", packet.getAddress().toString()));
                            boolean newPeer = PEERS.add(packet.getAddress());
                            if (newPeer) {
                                LOGGER.info(String.format("Replying peer gossip for %s", packet.getAddress().toString()));
                                NetworkUtils.sendPacket(socketOut, new byte[]{MsgUtils.PEER_ADDED}, packet.getAddress(), Config.PORT_NODE_IN);
                            } else {
                                LOGGER.info(String.format("%s is already our peer, continuing...", packet.getAddress().toString()));
                            }
                            break;
                        }
                        case MsgUtils.PEER_ADDED: {
                            LOGGER.info(String.format("%s added us as a peer.", packet.getAddress().toString()));
                            break;
                        }
                    }
                }
            };
            listeningThread.start();
        } catch (SocketException e) {
            e.printStackTrace();
        }
    }

    public void gracefulShutdown() {
        LOGGER.info("Shutdown initiated...");
        try {
            // if a thread is blocked by receive, interrupt it (no processing in place, we're fine)
            listeningThread.close();
            listeningThread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        LOGGER.info("Shutdown completed.");
    }

    @Override
    public void gossipForPeers() {
        NetworkUtils.broadcast(socketOut, new byte[]{MsgUtils.PEER}, Config.PORT_NODE_IN);
    }

    @Override
    public void init() {
        LOGGER.info("Starting...");
        this.listenForMsg();
        this.gossipForPeers();
        LOGGER.info("Start completed.");
    }

    private static abstract class ListeningThread extends Thread {
        private final DatagramSocket socketIn = new DatagramSocket(Config.PORT_NODE_IN);

        public ListeningThread() throws SocketException {
            super("NodeListeningThread");
        }

        public abstract void digest(byte[] data, DatagramPacket packet);

        public void close() {
            this.socketIn.close();
        }

        @Override
        public void run() {
            try {
                byte[] data;
                DatagramPacket packet;
                while (!socketIn.isClosed()) {
                    data = new byte[32768];
                    packet = new DatagramPacket(data, data.length);
                    // blocking
                    socketIn.receive(packet);
                    // skips loopback packets
                    InetAddress address = packet.getAddress();
//                    if (address.isAnyLocalAddress() || address.isLoopbackAddress() || NetworkInterface.getByInetAddress(address) != null) {
//                        continue;
//                    }
                    byte[] finalData = data;
                    DatagramPacket finalPacket = packet;
                    new Thread(() -> digest(finalData, finalPacket)).start();
                }
            } catch (IOException ignored) {

            }
        }
    }
}
