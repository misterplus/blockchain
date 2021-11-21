package net.homework.blockchain.client;

import cn.hutool.http.HttpRequest;
import lombok.SneakyThrows;
import net.homework.blockchain.Config;
import net.homework.blockchain.entity.Block;
import net.homework.blockchain.entity.Transaction;
import net.homework.blockchain.util.ByteUtils;
import net.homework.blockchain.util.CryptoUtils;
import net.homework.blockchain.util.NetworkUtils;
import org.apache.commons.codec.binary.Hex;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.*;
import java.util.stream.Collectors;

public class MinerImpl implements Miner {

    // technically using a priority queue ordered by miner fees would be ideal
    private final Queue<Transaction> localTxPool = new LinkedList<>();
    private InetAddress node;
    private String address;
    private byte[] publicKeyHash;
    private String urlLatestHash;
    private String urlTotalInput;
    private boolean beaten = false;

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
    public boolean doPow(Block block) {
        while (!block.isBlockValid()) {
            if (beaten) {
                // beaten by other miners, solution not found
                return false;
            }
            block.increment();
        }
        // solution found
        return true;
    }

    private static class MessageThread extends Thread {
        private final Queue<byte[]> msgs = new LinkedList<>();

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
                    // add to queue
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
        miner.node = InetAddress.getByName(args[0]);
        miner.address = args[1];
        miner.publicKeyHash = CryptoUtils.getPublicKeyHashFromAddress(args[1]);
        miner.initLocalPool();
        while(true) {
            String latestHash = miner.getLatestBlockHash();
            Block newBlock = new Block(Hex.decodeHex(latestHash), miner.createCoinbase());
            // TODO: if you want to update local pool, do it now
            // keep adding txs until block is full or local pool is empty
            while(newBlock.toBytes().length <= Config.MAX_BLOCK_SIZE && !miner.localTxPool.isEmpty()) {
                // get one to add, but don't remove it from the queue
                Transaction toAdd = miner.localTxPool.element();
                // try adding it
                newBlock.addTransaction(toAdd);
                // if it doesn't fit, revert
                if (newBlock.toBytes().length > Config.MAX_BLOCK_SIZE) {
                    newBlock.revert();
                    // in theory we can add smaller transactions, but halting is simpler (we are using queue)
                    break;
                }
                // if it fits, remove the tx from local pool
                else {
                    miner.localTxPool.poll();
                }
            }
            // block finished, update miner fee
            miner.updateReward(newBlock);
            // do pow stuff
            boolean success = miner.doPow(newBlock);
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
                Map<String, Boolean> map = ByteUtils.fromBytes(packet.getData(), new HashMap<>());
                boolean accepted = map.get("BlockAccepted");
                if (accepted) {
                    System.out.printf("[MSG]Block with hash %s is accepted!\n", Hex.encodeHexString(newBlock.hashHeader(), false));
                } else {
                    System.out.printf("[MSG]Block with hash %s is rejected!\n", Hex.encodeHexString(newBlock.hashHeader(), false));
                }
            } else {
                System.out.printf("[MSG]Block with hash %s is not solved, discarding!", Hex.encodeHexString(newBlock.hashHeader()));
            }
        }
    }
}
