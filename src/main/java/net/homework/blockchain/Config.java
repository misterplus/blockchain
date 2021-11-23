package net.homework.blockchain;

public interface Config {

    // chain constants
    int DIFFICULTY = 3; // difficulty in BYTES (8 * DIFFICULTY bits)
    int COINBASE_MATURITY = 10;
    int MAX_BLOCK_SIZE = 32768;
    long BLOCK_FEE = 500000000L; // 5 coins

    // miner constants
    int REFILLING_INTERVAL = 1000000; // refilling block after every x hashes

    // web port
    int PORT_HTTP = 8080;

    // udp port
    int PORT_IN = 10240;
    int PORT_OUT = 10241;
}
