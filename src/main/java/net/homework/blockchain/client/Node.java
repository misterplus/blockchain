package net.homework.blockchain.client;

public interface Node {
    // 监听新转账
    void listenForTx();
    // 监听矿工连接
    void listenForMiner();
    // 广播新区块
    void broadcastNewBlock();
    // 监听新区块
    void listenForNewBlock();
}
