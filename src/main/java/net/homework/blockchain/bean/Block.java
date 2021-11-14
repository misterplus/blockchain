package net.homework.blockchain.bean;

import com.fasterxml.jackson.annotation.JsonIgnore;
import net.homework.blockchain.Config;
import net.homework.blockchain.util.ByteUtils;
import net.homework.blockchain.util.CryptoUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Block {
    private static class Header {
        private final byte[] hashPrevBlock;
        private byte[] hashMerkleRoot;
        private long time;
        private int difficulty = Config.DIFFICULTY;
        private int nonce = 0;
        @JsonIgnore
        private MerkleTree merkleTree;

        /**
         * @param hashPrevBlock hash of the previous block header
         * hashMerkleRoot hash based on all of the transactions in the block
         * time UTC timestamp
         * difficulty difficulty of the network
         * nonce for calculating the hash of this block
         */
        public Header(byte[] hashPrevBlock, MerkleTree merkleTree) {
            this.hashPrevBlock = hashPrevBlock;
            this.merkleTree = merkleTree;
            this.hashMerkleRoot = merkleTree.hashMerkleTree();
            this.time = System.currentTimeMillis();
        }

        public void increment() {
            if (++nonce == 0) {
                // increment extraNonce
            }
            this.time = System.currentTimeMillis();
        }

        public void updateMerkleRoot(byte[] newRoot) {
            this.hashMerkleRoot = newRoot;
        }

        public void addHash(Transaction tx) {
            this.merkleTree.addHash(tx);
            this.updateMerkleRoot(this.merkleTree.hashMerkleTree());
        }
    }

    private Header header;
    private List<Transaction> transactions;

    public Block(byte[] hashPrevBlock, List<Transaction> transactions) {
        ArrayList<byte[]> tree = new ArrayList<>();
        for (Transaction tx : transactions) {
            tree.add(tx.hashTransaction());
        }
        MerkleTree merkleTree = new MerkleTree(tree);
        Block.Header header = new Header(hashPrevBlock, merkleTree);
        this.header = header;
        this.transactions = transactions;
    }

    public void addTransaction(Transaction tx) {
        this.transactions.add(tx);
        this.header.addHash(tx);
    }

    private byte[] hashHeader() {
        return CryptoUtils.sha256Twice(ByteUtils.toBytes(header));
    }

    private boolean isBlockValid() {
        return ByteUtils.isZero(Arrays.copyOf(hashHeader(), header.difficulty));
    }

    /**
     * Mining:
     *  construct a new block, add txs
     *  while block is not valid
     *      increment()
     *  send to node
     */
}
