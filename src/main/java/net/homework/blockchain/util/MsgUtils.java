package net.homework.blockchain.util;

import java.util.List;

public class MsgUtils {
    // msg types

    // single byte msgs
    public final static byte BLOCK_ACCEPTED = 1; // node to miner
    public final static byte BLOCK_REJECTED = 2; // node to miner
    public final static byte TX_ACCEPTED = 3; // node to user
    public final static byte TX_REJECTED = 4; // node to user

    // multi-part msgs
    public final static byte TX_POOL_ADD = -1; // + a list of wrapped txs | node to miner
    public final static byte TX_POOL_REMOVE = -2; // + a list of tx hashes | node to miner
    public final static byte BLOCK_REQUESTED = -3; // + requested block's hash | node to node

    public static boolean isBlockAccepted(byte[] data) {
        return data[0] == BLOCK_ACCEPTED;
    }

    public static boolean isBlockRejected(byte[] data) {
        return data[0] == BLOCK_REJECTED;
    }

    public static boolean isMsgRemove(byte[] data) {
        return data[0] == TX_POOL_REMOVE;
    }

    public static boolean isMsgAdd(byte[] data) {
        return data[0] == TX_POOL_ADD;
    }

    public static boolean isPoolMsg(byte[] data) {
        return isMsgAdd(data) || isMsgRemove(data);
    }

    public static boolean isBlockMsg(byte[] data) {
        return isBlockAccepted(data) || isBlockRejected(data);
    }

    public static boolean isBlockRequestMsg(byte[] data) {
        return data[0] == BLOCK_REQUESTED;
    }

    public static byte[] toBlockRequestMsg(byte[] part) {
        byte[] msg = new byte[part.length + 1];
        msg[0] = BLOCK_REQUESTED;
        System.arraycopy(part, 0, msg, 1, part.length);
        return msg;
    }

    public static byte[] toRemoveMsg(List<byte[]> removedTxHashes) {
        byte[] part = ByteUtils.toBytes(removedTxHashes);
        if (part == null) {
            return null;
        }
        byte[] msg = new byte[part.length + 1];
        msg[0] = TX_POOL_REMOVE;
        System.arraycopy(part, 0, msg, 1, part.length);
        return msg;
    }

    public static byte[] toBlockMsg(boolean accepted) {
        return new byte[]{accepted ? BLOCK_ACCEPTED : BLOCK_REJECTED};
    }

    public static byte[] toTxMsg(boolean accepted) {
        return new byte[]{accepted ? TX_ACCEPTED : TX_REJECTED};
    }
}
