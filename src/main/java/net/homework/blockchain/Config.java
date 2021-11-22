package net.homework.blockchain;

public interface Config {
    // difficulty in BYTES (8 * DIFFICULTY bits)
    int DIFFICULTY = 3;
    // refilling block after every x hashes
    int REFILLING_INTERVAL = 1000000;
    int PORT_HTTP = 8080;
    int PORT_TX_BROADCAST_IN = 10240;
    int PORT_TX_BROADCAST_OUT = 10241;
    int PORT_BLOCK_BROADCAST_IN = 10242;
    int PORT_BLOCK_BROADCAST_OUT = 10243;
    int PORT_MSG_IN = 10244;
    int PORT_MSG_OUT = 10245;
    int PORT_LOCAL_HALT_IN = 10246;
    int PORT_LOCAL_HALT_OUT = 10247;
    int COINBASE_MATURITY = 10;
    int MAX_BLOCK_SIZE = 32768;
    // 5 btc
    long BLOCK_FEE = 500000000L;
    // for debugging
    int PORT_PLACEHOLDER = 10239;

}
