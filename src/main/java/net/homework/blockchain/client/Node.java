package net.homework.blockchain.client;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public interface Node {
    Logger LOGGER = LoggerFactory.getLogger(Node.class);

    void listenForMsg();

    void init();

    void gracefulShutdown();
}
