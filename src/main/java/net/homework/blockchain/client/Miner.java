package net.homework.blockchain.client;

public interface Miner {
    void setWallet(String wallet);
    void connectToNode(String nodeIP);
    void doPow();
}
