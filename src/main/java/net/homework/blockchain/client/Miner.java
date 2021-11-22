package net.homework.blockchain.client;

import net.homework.blockchain.entity.Block;
import net.homework.blockchain.entity.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public interface Miner {
    Logger LOGGER = LoggerFactory.getLogger(Miner.class);

    String getLatestBlockHash();

    List<Transaction> createCoinbase();

    void updateReward(Block block);

    void updateReward(Block block, long extraFee);

    void initLocalPool();

    boolean fillBlock(Block block);

    void digestPoolMsgs();

    void init(String[] args);
}
