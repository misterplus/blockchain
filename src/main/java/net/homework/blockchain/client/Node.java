package net.homework.blockchain.client;

public interface Node {
    void listenForTransaction();

    void listenForNewBlock();

    void listenForMsg();

    void init();

    void halt();
}
