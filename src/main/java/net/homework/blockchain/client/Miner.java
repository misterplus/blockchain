package net.homework.blockchain.client;

import net.homework.blockchain.entity.Block;
import net.homework.blockchain.entity.Transaction;

import java.util.List;

public interface Miner {
    String getLatestBlockHash();

    List<Transaction> createCoinbase();

    void updateReward(Block block);

    void updateReward(Block block, long extraFee);

    void initLocalPool();

    boolean fillBlock(Block block);

    void digestMsgs();
}
