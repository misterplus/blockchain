package net.homework.blockchain.bean;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import net.homework.blockchain.Config;
import net.homework.blockchain.util.ByteUtils;
import net.homework.blockchain.util.CryptoUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Block {
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

    /**
     * Called when a node needs to verify a block's merkle hash
     * we don't send the merkle tree over the network because it can be tempered with
     */
    public void reconstructMerkleTree() {
        ArrayList<byte[]> tree = new ArrayList<>();
        for (Transaction tx : this.transactions) {
            tree.add(tx.hashTransaction());
        }
        this.header.merkleTree = new MerkleTree(tree);
    }

    public byte[] toBytes() {
        return ByteUtils.toBytes(this);
    }

    public byte[] hashHeader() {
        return CryptoUtils.sha256Twice(ByteUtils.toBytes(header));
    }

    private boolean isBlockValid() {
        return ByteUtils.isZero(Arrays.copyOf(hashHeader(), header.difficulty));
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Header {
        // hash of the previous block header
        private byte[] hashPrevBlock;
        // hash based on all of the transactions in the block
        private byte[] hashMerkleRoot;
        // UTC timestamp
        private long time;
        // difficulty of the network
        private final int difficulty = Config.DIFFICULTY;
        // for calculating the hash of this block
        private int nonce = 0;

        // not present if received from network
        @JsonIgnore
        private MerkleTree merkleTree;

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



    /**
     * Mining:
     *  construct a new block, add txs
     *  while block is not valid
     *      increment()
     *  send to node
     */
}
