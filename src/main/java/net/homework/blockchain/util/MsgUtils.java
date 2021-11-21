package net.homework.blockchain.util;

public class MsgUtils {
    // msg types

    // single byte msgs
    public final static byte BLOCK_ACCEPTED = 0;
    public final static byte BLOCK_REJECTED = 1;
    public final static byte TX_ACCEPTED = 2;
    public final static byte TX_REJECTED = 3;

    // multi-part msgs
    public final static byte TX_POOL_ADD = 4; // + a list of wrapped txs
    public final static byte TX_POOL_REMOVE = 5; // + a list of tx hashes

    public static boolean isBlockAccepted(byte[] data) {
        return data[0] == BLOCK_ACCEPTED;
    }

    public static boolean isMsgRemove(byte[] data) {
        return data[0] == TX_POOL_REMOVE;
    }
}
