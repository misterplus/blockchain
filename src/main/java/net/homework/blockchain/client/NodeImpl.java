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

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class NodeImpl implements Node {

    private static final BlockchainService blockchainService = SpringContext.getBean(BlockchainService.class);

    // valid txs, to be processed by a miner
    public static Map<ByteBuffer, WrappedTransaction> TX_POOL = new HashMap<>();
    // because udp packets won't arrive in order, there'll be orphan transactions, waiting to be claimed by other txs
    public static Map<ByteBuffer, Transaction> ORPHAN_TXS = new HashMap<>();
    // orphan blocks
    public static Map<ByteBuffer, Block> ORPHAN_BLOCKS = new HashMap<>();

    private final boolean[] halt = new boolean[]{false};

    public static void main(String[] args) {
        Node node = new NodeImpl();
        node.init();
        System.out.println("[MSG]Node is up and running...");
        try {
            DatagramSocket socket = new DatagramSocket(Config.PORT_LOCAL_HALT_IN);
            byte[] data = new byte[]{0};
            DatagramPacket packet;
            while (!socket.isClosed() && !MsgUtils.isHaltingMsg(data)) {
                data = new byte[32768];
                packet = new DatagramPacket(data, data.length);
                socket.receive(packet);
            }
            socket.close();
            System.out.println("[MSG]Halting msg received, exiting...");
            node.halt();
            // TODO: stop spring as well, maybe rework halting to a http request
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void listenForTransaction() {
        new ListeningThread(Config.PORT_TX_BROADCAST_IN, halt) {
            @Override
            public void digest(byte[] data, DatagramPacket packet) {
                Transaction tx = ByteUtils.fromBytes(data, new Transaction());
                boolean accepted = false;
                // Check that size in bytes >= 100
                if (packet.getLength() >= 100) {
                    // verify this transaction
                    accepted = VerifyUtils.verifyTx(tx, TX_POOL, ORPHAN_TXS);
                }
                // send accepted msg back
                NetworkUtils.sendPacket(packet.getAddress(), Config.PORT_MSG_OUT, MsgUtils.toTxMsg(accepted), Config.PORT_MSG_IN);
            }
        }.start();
    }

    @Override
    public void listenForNewBlock() {
        new ListeningThread(Config.PORT_BLOCK_BROADCAST_IN, halt) {
            @Override
            public void digest(byte[] data, DatagramPacket packet) {
                Block block = ByteUtils.fromBytes(data, new Block());
                boolean accepted = VerifyUtils.verifyBlock(block, packet.getAddress(), ORPHAN_BLOCKS, TX_POOL);
                NetworkUtils.sendPacket(packet.getAddress(), Config.PORT_MSG_OUT, MsgUtils.toBlockMsg(accepted), Config.PORT_MSG_IN);
            }
        }.start();
    }

    @Override
    public void listenForMsg() {
        new ListeningThread(Config.PORT_MSG_IN, halt) {
            @Override
            public void digest(byte[] data, DatagramPacket packet) {
                // only listen for BLOCK_REQUEST
                if (MsgUtils.isBlockRequestMsg(data)) {
                    byte[] headerHash = Arrays.copyOfRange(data, 1, data.length);
                    byte[] blockBytes = blockchainService.getBlockOnChainByHash(headerHash).toBytes();
                    NetworkUtils.sendPacket(packet.getAddress(), Config.PORT_BLOCK_BROADCAST_OUT, blockBytes, Config.PORT_BLOCK_BROADCAST_IN);
                }
            }
        }.start();
    }

    public void halt() {
        this.halt[0] = true;
    }

    @Override
    public void init() {
        this.listenForMsg();
        this.listenForNewBlock();
        this.listenForTransaction();
    }

    private static abstract class ListeningThread extends Thread {
        private final int portIn;
        private final boolean[] halt;

        public ListeningThread(int portIn, boolean[] halt) {
            this.portIn = portIn;
            this.halt = halt;
        }

        public abstract void digest(byte[] data, DatagramPacket packet);

        @Override
        public void run() {
            try {
                DatagramSocket socket = new DatagramSocket(portIn);
                byte[] data;
                DatagramPacket packet;
                while (!socket.isClosed() && !halt[0]) {
                    data = new byte[32768];
                    packet = new DatagramPacket(data, data.length);
                    // blocking
                    socket.receive(packet);
                    byte[] finalData = data;
                    DatagramPacket finalPacket = packet;
                    new Thread(() -> digest(finalData, finalPacket)).start();
                }
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
