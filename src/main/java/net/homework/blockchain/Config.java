package net.homework.blockchain;

public interface Config {
    // difficulty is BYTES (8 times bits)
    int DIFFICULTY = 3;
    int PORT_TX_BROADCAST_IN = 10240;
    int PORT_TX_BROADCAST_OUT = 10241;
    int PORT_BLOCK_BROADCAST_IN = 10242;
    int PORT_BLOCK_BROADCAST_OUT = 10243;
    int COINBASE_MATURITY = 10;
    int MAX_BLOCK_SIZE = 32768;
    // 5 btc
    long BLOCK_FEE = 500000000L;

}
