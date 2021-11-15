package net.homework.blockchain.client;

import net.homework.blockchain.bean.Transaction;

public interface Node {
    void listenForTransaction();

    void listenForMiner();

    void broadcastNewBlock();

    void listenForNewBlock();

    void verifyTx(Transaction tx, int size);

    boolean isCoinbaseTransactionValid(Transaction tx, int size);
}
