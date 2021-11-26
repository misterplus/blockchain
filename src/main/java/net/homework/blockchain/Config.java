package net.homework.blockchain;

public interface Config {

    // chain constants
    int DIFFICULTY = 3; // difficulty in BYTES (8 * DIFFICULTY bits)
    int COINBASE_MATURITY = 0;
    int MAX_BLOCK_SIZE = 32768;
    long BLOCK_FEE = 500000000L; // 5 coins

    // miner constants
    int REFILLING_INTERVAL = 1000000; // refilling block after every x hashes

    // web port
    int PORT_HTTP = 8080;

    // node port
    int PORT_NODE_IN = 10240;
    int PORT_NODE_OUT = 10241;

    // miner port
    int PORT_MINER_IN = 10242;
    int PORT_MINER_OUT = 10243;

    // user port
    int PORT_USER_IN = 10244;
    int PORT_USER_OUT = 10245;
}
