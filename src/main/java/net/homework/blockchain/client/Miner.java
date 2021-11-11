package net.homework.blockchain.client;

public interface Miner {
    // 设置钱包
    void setWallet(String wallet);
    // 连接到节点并获取新区块信息
    void connectToNode(String nodeIP);
    // 寻找工作量证明
    void doPow();
}
