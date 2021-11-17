package net.homework.blockchain.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import net.homework.blockchain.Config;
import net.homework.blockchain.util.ByteUtils;
import net.homework.blockchain.util.CryptoUtils;
import org.apache.commons.codec.binary.Hex;
import org.hibernate.annotations.Immutable;

import javax.persistence.*;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Immutable
public class Block {

    @JsonIgnore
    private String hashBlock;

    @Id
    @Column(length = 64)
    public String getHashBlock() {
        return Hex.encodeHexString(hashHeader(), false);
    }

    @JsonIgnore
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    // block height
    private long height;

    private Header header;


    private List<Transaction> transactions;
    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderColumn(name = "index_in_block")
    public List<Transaction> getTransactions() {
        return transactions;
    }

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
        return header.hashHeader();
    }

    @Transient
    private boolean isBlockValid() {
        return ByteUtils.isZero(Arrays.copyOf(hashHeader(), header.difficulty));
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Embeddable
    @Immutable
    public static class Header implements Serializable {
        private static final long serialVersionUID = 0L;

        // hash of the previous block header
        private byte[] hashPrevBlock;
        // hash based on all of the transactions in the block
        private byte[] hashMerkleRoot;
        // UTC timestamp
        private long time;
        // difficulty of the network
        private int difficulty = Config.DIFFICULTY;
        // for calculating the hash of this block
        private int nonce = 0;

        @JsonIgnore
        private transient MerkleTree merkleTree;

        @Transient // do not store in db
        public MerkleTree getMerkleTree() {
            return merkleTree;
        }

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

        private byte[] hashHeader() {
            return CryptoUtils.sha256Twice(ByteUtils.toBytes(this));
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
