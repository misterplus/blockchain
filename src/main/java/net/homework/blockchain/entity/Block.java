package net.homework.blockchain.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import net.homework.blockchain.Config;
import net.homework.blockchain.util.ByteUtils;
import net.homework.blockchain.util.CryptoUtils;
import net.homework.blockchain.util.MsgUtils;
import org.apache.commons.codec.binary.Hex;

import javax.persistence.*;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
public class Block implements IMessage {

    @JsonIgnore
    private byte[] hashBlock;
    @JsonIgnore
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    // block height
    private long height;
    private Header header;
    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @OrderColumn(name = "index_in_block")
    private List<Transaction> transactions;

    public Block(byte[] hashPrevBlock, List<Transaction> transactions) {
        ArrayList<byte[]> tree = new ArrayList<>();
        for (Transaction tx : transactions) {
            tree.add(tx.hashTransaction());
        }
        MerkleTree merkleTree = new MerkleTree(tree);
        this.header = new Header(hashPrevBlock, merkleTree);
        this.transactions = transactions;
    }

    @PrePersist
    public void preSave() {
        this.hashBlock = hashHeader();
    }

    public void addTransaction(Transaction tx) {
        this.transactions.add(tx);
        this.header.addTransaction(tx);
    }

    public void updateCoinbase() {
        this.getHeader().getMerkleTree().updateCoinbaseHash(this.transactions.get(0).hashTransaction());
        this.getHeader().updateMerkleRoot();
    }

    public Transaction revert() {
        this.header.revert();
        return this.transactions.remove(this.transactions.size() - 1);
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

    @Override
    public byte msgType() {
        return MsgUtils.BLOCK_NEW;
    }

    @Override
    public byte[] toBytes() {
        return ByteUtils.toBytes(this);
    }

    public byte[] hashHeader() {
        return header.hashHeader();
    }

    @Transient
    @JsonIgnore
    public boolean isBlockValid() {
        return ByteUtils.isZero(Arrays.copyOf(hashHeader(), header.difficulty));
    }

    public void increment() {
        if (++this.header.nonce == 0) {
            transactions.get(0).getInputs().get(0).incrementExtraNonce();
        }
        this.header.time = System.currentTimeMillis();
    }

    public String hashHeaderHex() {
        return Hex.encodeHexString(hashHeader(), false);
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Embeddable
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

        public Header(byte[] hashPrevBlock, MerkleTree merkleTree) {
            this.hashPrevBlock = hashPrevBlock;
            this.merkleTree = merkleTree;
            this.hashMerkleRoot = merkleTree.hashMerkleTree();
            this.time = System.currentTimeMillis();
        }

        @Transient // do not store in db
        public MerkleTree getMerkleTree() {
            return merkleTree;
        }

        private void updateMerkleRoot() {
            this.hashMerkleRoot = this.merkleTree.hashMerkleTree();
        }

        public void addTransaction(Transaction tx) {
            this.merkleTree.addTransaction(tx);
            this.updateMerkleRoot();
        }

        public void revert() {
            this.merkleTree.revert();
            this.updateMerkleRoot();
        }

        private byte[] hashHeader() {
            return CryptoUtils.sha256Twice(ByteUtils.toBytes(this));
        }
    }
}
