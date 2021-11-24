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
import java.net.*;
import java.util.*;
import java.util.stream.Collectors;

public class MinerImpl implements Miner {

    private final PriorityQueue<WrappedTransaction> localTxPool = new PriorityQueue<>();
    private final List<WrappedTransaction> revertList = new ArrayList<>();
    private final DatagramSocket socketOut = new DatagramSocket(Config.PORT_MINER_OUT);
    private final InetAddress node;
    private final MessageThread msgThread;
    private final byte[] publicKeyHash;
    private final String urlLatestHash;
    private final String urlInitLocalPool;


    private MinerImpl(String[] args) throws SocketException, UnknownHostException {
        this.node = InetAddress.getByName(args[0]);
        this.publicKeyHash = CryptoUtils.getPublicKeyHashFromAddress(args[1]);
        this.msgThread = new MessageThread(node);
        this.urlLatestHash = String.format("%s:%d/latestBlockHash", args[0], Config.PORT_HTTP);
        this.urlInitLocalPool = String.format("%s:%d/txPool", args[0], Config.PORT_HTTP);
    }

    @SneakyThrows
    public static void main(String[] args) {
        LOGGER.info("Starting...");
        MinerImpl miner = new MinerImpl(args);
        miner.init();
        LOGGER.info("Start completed.");
        LOGGER.info("Entering main work loop...");
        while (true) {
            // clear the revert list no matter what
            miner.revertList.clear();
            // reset beaten status
            miner.msgThread.resetBeaten();
            String latestHash = miner.getLatestBlockHash();
            LOGGER.info(String.format("Latest block hash is %s.", latestHash));
            Block block = new Block(Hex.decodeHex(latestHash), miner.createCoinbase(Hex.decodeHex(latestHash)));
            LOGGER.info(String.format("Creating new block with hash %s.", block.hashHeaderHex()));
            // try filling block, might be full, might not be (local pool not empty)
            boolean blockFull = miner.fillBlock(block);
            // do pow stuff
            while (!block.isBlockValid()) {
                if (miner.msgThread.isBeaten()) {
                    // block solved by others, revert local pool
                    miner.localTxPool.addAll(miner.revertList);
                    break;
                }
                // refilling the block every 1mil hashes
                if (!blockFull && block.getHeader().getNonce() % Config.REFILLING_INTERVAL == 0) {
                    blockFull = miner.fillBlock(block);
                }
                block.increment();
            }
            // block solved by others, discarding as if we did not solve this block
            if (miner.msgThread.isBeaten()) {
                LOGGER.info(String.format("Block with hash %s is already solved by others, discarding...", block.hashHeaderHex()));
            } else {
                LOGGER.info(String.format("Block with hash %s is solved with nonce %d, submitting...", block.hashHeaderHex(), block.getHeader().getNonce()));
                // block solved by us, submit to node
                NetworkUtils.sendPacket(miner.socketOut, block.toMsg(), miner.node, Config.PORT_NODE_IN);
                // wait for reply
                synchronized (miner.msgThread.blockMsgs) {
                    LOGGER.debug("Waiting for block message...");
                    miner.msgThread.blockMsgs.wait();
                }
                byte[] data = miner.msgThread.blockMsgs.poll();
                if (MsgUtils.isBlockAccepted(data)) {
                    // block solved by us and accepted by node
                    LOGGER.info(String.format("Block with hash %s is accepted.", block.hashHeaderHex()));
                } else if (MsgUtils.isBlockRejected(data)) {
                    // block solved by us but rejected by node, revert local pool, as if we did not solve this block
                    miner.localTxPool.addAll(miner.revertList);
                    LOGGER.info(String.format("Block with hash %s is rejected.", block.hashHeaderHex()));
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
        LOGGER.debug(String.format("Updated miner fee from %d to %d for block with hash %s", feePrev, feePrev + extraFee, block.hashHeaderHex()));
    }

    @Override
    public void initLocalPool() {
        List<WrappedTransaction> list = ByteUtils.fromJson(HttpRequest.post(urlInitLocalPool)
                .timeout(5000)
                .execute().body(), new ArrayList<>());
        this.localTxPool.addAll(list);
        if (list != null) {
            LOGGER.debug(String.format("Created local pool with transactions:\n%s", list.stream().map((wrapped -> wrapped.getTx().hashTransactionHex())).collect(Collectors.joining("\n"))));
        }
    }

    @Override
    public boolean fillBlock(Block block) {
        boolean blockFull = false;
        long extraFee = 0L;
        digestPoolMsgs();
        LOGGER.debug(String.format("Filling block with hash %s...", block.hashHeaderHex()));
        // keep adding txs until block is full or local pool is empty
        while (block.toBytes().length <= Config.MAX_BLOCK_SIZE && !localTxPool.isEmpty()) {
            // get the most valuable one, but don't remove it from the queue
            WrappedTransaction wrappedTx = localTxPool.element();
            Transaction toAdd = wrappedTx.getTx();
            // try adding it
            block.addTransaction(toAdd);
            LOGGER.debug(String.format("Adding transaction with hash %s to block...", toAdd.hashTransactionHex()));
            // if it doesn't fit, revert
            if (block.toBytes().length > Config.MAX_BLOCK_SIZE) {
                Transaction reverted = block.revert();
                blockFull = true;
                LOGGER.debug(String.format("Block size exceeded limit, reverting last addition of transaction %s....", reverted.hashTransactionHex()));
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
        updateReward(block, extraFee);
        return blockFull;
    }

    @Override
    public void digestPoolMsgs() {
        LOGGER.debug("Digesting pool messages...");
        Queue<byte[]> msgs = this.msgThread.poolMsgs;
        while (!msgs.isEmpty()) {
            byte[] multiPart = msgs.poll();
            // add or remove shit
            byte[] msg = Arrays.copyOfRange(multiPart, 1, multiPart.length);
            if (MsgUtils.isMsgAdd(multiPart)) {
                List<WrappedTransaction> toAdd = ByteUtils.fromBytes(msg, new ArrayList<>());
                this.localTxPool.addAll(toAdd);
                if (toAdd != null && !toAdd.isEmpty()) {
                    LOGGER.debug(String.format("Added transactions to local pool:\n%s", toAdd.stream().map((wrapped) -> wrapped.getTx().hashTransactionHex()).collect(Collectors.joining("\n"))));
                }
            } else if (MsgUtils.isMsgRemove(multiPart)) {
                // better implementation possibly?
                List<byte[]> toRemoveBytes = ByteUtils.fromBytes(msg, new ArrayList<>());
                if (toRemoveBytes != null) {
                    List<String> toRemoveHex = toRemoveBytes.stream().map(bytes -> Hex.encodeHexString(bytes, false)).collect(Collectors.toList());
                    LOGGER.debug(String.format("Preparing to remove transactions from local pool:\n%s", String.join("\n", toRemoveHex)));
                    this.localTxPool.removeIf(wrappedTx -> {
                        String hash = wrappedTx.getTx().hashTransactionHex();
                        boolean test = toRemoveHex.contains(hash);
                        if (test) {
                            LOGGER.debug(String.format("Transaction with hash %s is removed from local pool.", hash));
                        }
                        return test;
                    });
                }
            }
        }
    }

    @Override
    public void init() {
        this.initLocalPool();
        this.msgThread.start();
    }

    private static class MessageThread extends Thread {
        // two types of msgs: add / remove
        // add: new txs to add, just add it, block is fine
        // remove: txs to remove from the current local pool, which means another block was submitted and accepted, stop hashing the current block and discard it
        private final Queue<byte[]> poolMsgs = new LinkedList<>();
        private final Queue<byte[]> blockMsgs = new LinkedList<>();
        private final DatagramSocket socketIn = new DatagramSocket(Config.PORT_MINER_IN);
        private final InetAddress node;
        private boolean beaten = false;

        private MessageThread(InetAddress node) throws SocketException {
            super("MinerMessageThread");
            this.node = node;
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
                    // only listen for packets from the selected node
                    if (!packet.getAddress().equals(node)) {
                        continue;
                    }
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
                            LOGGER.debug("Block message received.");
                        }
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
