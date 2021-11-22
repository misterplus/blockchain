package net.homework.blockchain.client;

import cn.hutool.http.HttpRequest;
import lombok.SneakyThrows;
import net.homework.blockchain.Config;
import net.homework.blockchain.entity.Block;
import net.homework.blockchain.entity.Transaction;
import net.homework.blockchain.entity.WrappedTransaction;
import net.homework.blockchain.util.ByteUtils;
import net.homework.blockchain.util.CryptoUtils;
import net.homework.blockchain.util.MsgUtils;
import net.homework.blockchain.util.NetworkUtils;
import org.apache.commons.codec.binary.Hex;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.*;

public class MinerImpl implements Miner {

    private final PriorityQueue<WrappedTransaction> localTxPool = new PriorityQueue<>();
    private final List<WrappedTransaction> revertList = new ArrayList<>();
    private final DatagramSocket socketOut = new DatagramSocket(Config.PORT_OUT);
    private final MessageThread msgThread = new MessageThread();
    private InetAddress node;
    private byte[] publicKeyHash;
    private String urlLatestHash;
    private String urlInitLocalPool;


    private MinerImpl() throws SocketException {
    }

    @SneakyThrows
    public static void main(String[] args) {
        MinerImpl miner = new MinerImpl();
        miner.init(args);
        LOGGER.info("Entering main work loop...");
        while (true) {
            // clear the revert list no matter what
            miner.revertList.clear();
            // reset beaten status
            miner.msgThread.resetBeaten();
            String latestHash = miner.getLatestBlockHash();
            LOGGER.info(String.format("Latest block hash is %s.", latestHash));
            LOGGER.info("Creating new block...");
            Block newBlock = new Block(Hex.decodeHex(latestHash), miner.createCoinbase(Hex.decodeHex(latestHash)));
            // try filling block, might be full, might not be (local pool not empty)
            boolean blockFull = miner.fillBlock(newBlock);
            // do pow stuff
            while (!newBlock.isBlockValid()) {
                if (miner.msgThread.isBeaten()) {
                    // block solved by others, revert local pool
                    miner.localTxPool.addAll(miner.revertList);
                    break;
                }
                // refilling the block every 1mil hashes
                if (!blockFull && newBlock.getHeader().getNonce() % Config.REFILLING_INTERVAL == 0) {
                    blockFull = miner.fillBlock(newBlock);
                }
                newBlock.increment();
            }
            // block solved by others, discarding as if we did not solve this block
            if (miner.msgThread.isBeaten()) {
                LOGGER.info(String.format("Block with hash %s is already solved by others, discarding...", Hex.encodeHexString(newBlock.hashHeader(), false)));
            } else {
                LOGGER.info(String.format("Block with hash %s is solved with nonce %d.", Hex.encodeHexString(newBlock.hashHeader(), false), newBlock.getHeader().getNonce()));
                // block solved by us, submit to node
                NetworkUtils.sendPacket(miner.socketOut, newBlock.toMsg(), miner.node);
                // wait for reply
                synchronized (miner.msgThread.blockMsgs) {
                    miner.msgThread.blockMsgs.wait();
                }
                byte[] data = miner.msgThread.blockMsgs.poll();
                if (MsgUtils.isBlockAccepted(data)) {
                    // block solved by us and accepted by node
                    LOGGER.info(String.format("Block with hash %s is accepted.", Hex.encodeHexString(newBlock.hashHeader(), false)));
                } else if (MsgUtils.isBlockRejected(data)) {
                    // block solved by us but rejected by node, revert local pool, as if we did not solve this block
                    miner.localTxPool.addAll(miner.revertList);
                    LOGGER.info(String.format("Block with hash %s is rejected.", Hex.encodeHexString(newBlock.hashHeader(), false)));
                }
            }
        }
    }

    @Override
    public String getLatestBlockHash() {
        return HttpRequest.post(urlLatestHash)
                .timeout(5000)
                .execute().body();
    }

    @Override
    public List<Transaction> createCoinbase(byte[] hashPrevBlock) {
        List<Transaction> transactions = new ArrayList<>();
        Transaction tx = new Transaction(Collections.singletonList(new Transaction.Input(hashPrevBlock)), Collections.singletonList(new Transaction.Output(Config.BLOCK_FEE, publicKeyHash)));
        transactions.add(tx);
        return transactions;
    }

    @Override
    public void updateReward(Block block, long extraFee) {
        long feePrev = block.getTransactions().get(0).getOutputs().get(0).getValue();
        block.getTransactions().get(0).getOutputs().get(0).setValue(feePrev + extraFee);
    }

    @Override
    public void initLocalPool() {
        List<WrappedTransaction> list = ByteUtils.fromJson(HttpRequest.post(urlInitLocalPool)
                .timeout(5000)
                .execute().body(), new ArrayList<>());
        this.localTxPool.addAll(list);
    }

    @Override
    public boolean fillBlock(Block newBlock) {
        boolean blockFull = false;
        long extraFee = 0L;
        digestPoolMsgs();
        // keep adding txs until block is full or local pool is empty
        while (newBlock.toBytes().length <= Config.MAX_BLOCK_SIZE && !localTxPool.isEmpty()) {
            // get the most valuable one, but don't remove it from the queue
            WrappedTransaction wrappedTx = localTxPool.element();
            Transaction toAdd = wrappedTx.getTx();
            // try adding it
            newBlock.addTransaction(toAdd);
            // if it doesn't fit, revert
            if (newBlock.toBytes().length > Config.MAX_BLOCK_SIZE) {
                newBlock.revert();
                blockFull = true;
                // in theory we can add smaller transactions, but halting is simpler (we are using queue)
                break;
            }
            // if it fits, remove the tx from local pool
            else {
                revertList.add(localTxPool.poll());
                extraFee += wrappedTx.getFee();
            }
        }
        // block finished, update miner fee
        updateReward(newBlock, extraFee);
        return blockFull;
    }

    @Override
    public void digestPoolMsgs() {
        Queue<byte[]> msgs = this.msgThread.poolMsgs;
        while (!msgs.isEmpty()) {
            byte[] multiPart = msgs.poll();
            // add or remove shit
            byte[] msg = Arrays.copyOfRange(multiPart, 1, multiPart.length);
            if (MsgUtils.isMsgAdd(multiPart)) {
                List<WrappedTransaction> toAdd = ByteUtils.fromBytes(msg, new ArrayList<>());
                this.localTxPool.addAll(toAdd);
            } else if (MsgUtils.isMsgRemove(multiPart)) {
                // better implementation possibly?
                List<Object> toRemove = ByteUtils.fromBytes(msg, new ArrayList<>());
                if (toRemove != null) {
                    toRemove.replaceAll(o -> Hex.encodeHexString((byte[]) o, false));
                    this.localTxPool.removeIf(wrappedTx -> toRemove.contains(Hex.encodeHexString(wrappedTx.getTx().hashTransaction(), false)));
                }
            }
        }
    }

    @SneakyThrows
    @Override
    public void init(String[] args) {
        LOGGER.info("Starting...");
        this.urlLatestHash = String.format("%s:%d/latestBlockHash", args[0], Config.PORT_HTTP);
        this.urlInitLocalPool = String.format("%s:%d/txPool", args[0], Config.PORT_HTTP);
        this.node = InetAddress.getByName(args[0]);
        this.publicKeyHash = CryptoUtils.getPublicKeyHashFromAddress(args[1]);
        this.initLocalPool();
        this.msgThread.start();
        LOGGER.info("Start completed.");
    }

    private static class MessageThread extends Thread {
        // two types of msgs: add / remove
        // add: new txs to add, just add it, block is fine
        // remove: txs to remove from the current local pool, which means another block was submitted and accepted, stop hashing the current block and discard it
        private final Queue<byte[]> poolMsgs = new LinkedList<>();
        private final Queue<byte[]> blockMsgs = new LinkedList<>();
        private final DatagramSocket socketIn = new DatagramSocket(Config.PORT_IN);
        private boolean beaten = false;

        private MessageThread() throws SocketException {
        }

        public boolean isBeaten() {
            return beaten;
        }

        public void resetBeaten() {
            this.beaten = false;
        }

        @Override
        public void run() {
            try {
                byte[] data;
                DatagramPacket packet;
                while (!socketIn.isClosed()) {
                    data = new byte[32768];
                    packet = new DatagramPacket(data, data.length);
                    socketIn.receive(packet);
                    boolean isRemove = MsgUtils.isMsgRemove(data);
                    // add to queue
                    if (isRemove) {
                        beaten = true;
                    }
                    if (MsgUtils.isPoolMsg(data)) {
                        poolMsgs.add(data);
                    } else if (MsgUtils.isBlockMsg(data)) {
                        blockMsgs.add(data);
                        synchronized (blockMsgs) {
                            blockMsgs.notify();
                        }
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
