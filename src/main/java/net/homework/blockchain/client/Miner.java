package net.homework.blockchain.client;

import net.homework.blockchain.entity.Block;
import net.homework.blockchain.entity.Transaction;

import java.util.List;

public interface Miner {
    boolean doPow(Block block);
    String getLatestBlockHash();
    List<Transaction> createCoinbase();
    void updateReward(Block block);
}
