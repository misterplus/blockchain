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
import java.util.*;
import java.util.stream.Collectors;

public class MinerImpl implements Miner {

    private final PriorityQueue<WrappedTransaction> localTxPool = new PriorityQueue<>();
    private final List<WrappedTransaction> revertList = new ArrayList<>();
    private InetAddress node;
    private String address;
    private byte[] publicKeyHash;
    private String urlLatestHash;
    private String urlTotalInput;
    private String urlInitLocalPool;
    private MessageThread msgThread = new MessageThread();


    private MinerImpl() {
    }

    @Override
    public String getLatestBlockHash() {
        return HttpRequest.post(urlLatestHash)
                .timeout(5000)
                .execute().body();
    }

    @Override
    public List<Transaction> createCoinbase() {
        List<Transaction> transactions = new ArrayList<>();
        Transaction tx = new Transaction(Collections.singletonList(new Transaction.Input()), Collections.singletonList(new Transaction.Output(Config.BLOCK_FEE, publicKeyHash)));
        transactions.add(tx);
        return transactions;
    }

    @Override
    public void updateReward(Block block) {
        long feePrev = block.getTransactions().get(0).getOutputs().get(0).getValue();
        long outputSum = 0L;
        Map<String, Object> map = new HashMap<>();
        for (Transaction tx : block.getTransactions()) {
            for (Transaction.Output output : tx.getOutputs()) {
                outputSum += output.getValue();
            }
            for (Transaction.Input input : tx.getInputs()) {
                String key = Hex.encodeHexString(input.getPreviousTransactionHash(), false);
                if (!map.containsKey(key)) {
                    List<Integer> list = new ArrayList<>();
                    list.add(input.getOutIndex());
                    map.put(key, list);
                } else {
                    ((List<Integer>) map.get(key)).add(input.getOutIndex());
                }
            }
        }
        map.replaceAll((k, v) -> ((List<Integer>) map.get(k)).stream().map(Object::toString).collect(Collectors.joining(",")));
        long inputSum = Long.parseLong(HttpRequest.post(urlTotalInput)
                .timeout(5000)
                .body(ByteUtils.toJson(map))
                .execute().body());
        long minerFeeSum = outputSum - inputSum;
        block.getTransactions().get(0).getOutputs().get(0).setValue(feePrev + minerFeeSum);
    }

    @Override
    public void updateReward(Block block, long extraFee) {
        long feePrev = block.getTransactions().get(0).getOutputs().get(0).getValue();
        block.getTransactions().get(0).getOutputs().get(0).setValue(feePrev + extraFee);
    }

    @Override
    public void initLocalPool() {
        List<WrappedTransaction> list = ByteUtils.fromBytes(HttpRequest.post(urlInitLocalPool)
                .timeout(5000)
                .execute().body().getBytes(), new ArrayList<>());
        this.localTxPool.addAll(list);
    }

    @Override
    public boolean fillBlock(Block newBlock) {
        boolean blockFull = false;
        long extraFee = 0L;
        // TODO: if you want to update local pool, do it now
        // keep adding txs until block is full or local pool is empty
        while(newBlock.toBytes().length <= Config.MAX_BLOCK_SIZE && !localTxPool.isEmpty()) {
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
    public void digestMsgs() {
        Queue<byte[]> msgs = this.msgThread.msgs;
        while (!msgs.isEmpty()) {
            byte[] msg = msgs.poll();
            // add or remove shit
        }
    }

    private static class MessageThread extends Thread {
        // two types of msgs: add / remove
        // add: new txs to add, just add it, block is fine
        // remove: txs to remove from the current local pool, which means another block was submitted and accepted, stop hashing the current block and discard it
        private final Queue<byte[]> msgs = new LinkedList<>();
        private boolean beaten = false;

        public boolean isBeaten() {
            return beaten;
        }

        public void resetBeaten() {
            this.beaten = false;
        }

        @Override
        public void run() {
            try {
                DatagramSocket socket = new DatagramSocket(Config.PORT_MSG_IN);
                byte[] data;
                DatagramPacket packet;
                while (!socket.isClosed()) {
                    data = new byte[32768];
                    packet = new DatagramPacket(data, data.length);
                    socket.receive(packet);
                    boolean isRemove = MsgUtils.isMsgRemove(data);
                    // add to queue
                    if (isRemove) {
                        beaten = true;
                    }
                    msgs.add(data);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @SneakyThrows
    public static void main(String[] args) {
        MinerImpl miner = new MinerImpl();
        miner.urlLatestHash = String.format("%s:%d/block/latestHash", args[0], Config.PORT_HTTP);
        miner.urlTotalInput = String.format("%s:%d/totalInput", args[0], Config.PORT_HTTP);
        miner.urlInitLocalPool = String.format("%s:%d/txPool", args[0], Config.PORT_HTTP);
        miner.node = InetAddress.getByName(args[0]);
        miner.address = args[1];
        miner.publicKeyHash = CryptoUtils.getPublicKeyHashFromAddress(args[1]);
        miner.initLocalPool();
        miner.msgThread.start();
        while(true) {
            String latestHash = miner.getLatestBlockHash();
            Block newBlock = new Block(Hex.decodeHex(latestHash), miner.createCoinbase());
            // try filling block, might be full, might not be (local pool not empty)
            boolean blockFull = miner.fillBlock(newBlock);
            // do pow stuff
            boolean success = true;
            while (!newBlock.isBlockValid()) {
                if (miner.msgThread.isBeaten()) {
                    // beaten by other miners, solution not found
                    success = false;
                    break;
                }
                // refilling the block every 1mil hashes
                if (!blockFull && newBlock.getHeader().getNonce() % Config.REFILLING_INTERVAL == 0) {
                    blockFull = miner.fillBlock(newBlock);
                }
                newBlock.increment();
            }
            if (success) {
                System.out.printf("[MSG]Block with hash %s is solved with nonce %d!", Hex.encodeHexString(newBlock.hashHeader()), newBlock.getHeader().getNonce());
                // submit to node
                NetworkUtils.sendPacket(miner.node, Config.PORT_BLOCK_BROADCAST_OUT, newBlock.toBytes(), Config.PORT_BLOCK_BROADCAST_IN);
                // wait for reply
                DatagramSocket socket = new DatagramSocket(Config.PORT_MSG_IN);
                byte[] data;
                DatagramPacket packet;
                data = new byte[32768];
                packet = new DatagramPacket(data, data.length);
                socket.receive(packet);
                boolean accepted = MsgUtils.isBlockAccepted(data);
                if (accepted) {
                    // block solved by us and accepted by node
                    System.out.printf("[MSG]Block with hash %s is accepted!\n", Hex.encodeHexString(newBlock.hashHeader(), false));
                } else {
                    // block solved by us but rejected by node, revert local pool, as if we did not solve this block
                    miner.localTxPool.addAll(miner.revertList);
                    System.out.printf("[MSG]Block with hash %s is rejected!\n", Hex.encodeHexString(newBlock.hashHeader(), false));
                }
            } else {
                // block solved by others, revert local pool, as if we did not solve this block
                miner.localTxPool.addAll(miner.revertList);
                miner.msgThread.resetBeaten();
                System.out.printf("[MSG]Block with hash %s is already solved by others, discarding!", Hex.encodeHexString(newBlock.hashHeader()));
            }
            // clear the revert list no matter what
            miner.revertList.clear();
        }
    }
}
