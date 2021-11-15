package net.homework.blockchain;

public interface Config {
    int DIFFICULTY = 10;
    int PORT_TX_BROADCAST_IN = 10240;
    int PORT_TX_BROADCAST_OUT = 10241;
    int PORT_BLOCK_BROADCAST_IN = 10242;
    int COINBASE_MATURITY = 10;
    int MAX_BLOCK_SIZE = 32768;
}
