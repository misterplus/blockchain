package net.homework.blockchain.entity;

import net.homework.blockchain.util.ByteUtils;
import net.homework.blockchain.util.CryptoUtils;

import java.util.ArrayList;

public class MerkleTree {

    /**
     * List of hashes of all txs.
     */
    private final ArrayList<byte[]> tree;
    private byte[] merkleRoot;

    public MerkleTree(ArrayList<byte[]> tree) {
        this.tree = tree;
        rehash();
    }

    public byte[] hashMerkleTree() {
        return merkleRoot;
    }

    public void rehash() {
        this.merkleRoot = doRehash((ArrayList<byte[]>) tree.clone()).get(0);
    }

    public void addTransaction(Transaction transaction) {
        if (this.tree.add(transaction.hashTransaction())) {
            rehash();
        }
    }

    public void updateCoinbaseHash(byte[] coinbaseTxHash) {
        this.tree.set(0, coinbaseTxHash);
        rehash();
    }

    public void revert() {
        this.tree.remove(this.tree.size() - 1);
        rehash();
    }

    private ArrayList<byte[]> doRehash(ArrayList<byte[]> tree) {
        // return the root
        if (tree.size() == 1) {
            return tree;
        }
        ArrayList<byte[]> parentHashList = new ArrayList<>();
        // hash the leaf transaction pair to get parent transaction
        for (int i = 0; i < tree.size(); i += 2) {
            byte[] parentHash = CryptoUtils.sha256Twice(ByteUtils.concat(tree.get(i), tree.get(i + 1)));
            parentHashList.add(parentHash);
        }
        // if odd number of transactions, duplicate the last transaction
        if (tree.size() % 2 == 1) {
            byte[] lastHash = tree.get(tree.size() - 1);
            byte[] lastParent = CryptoUtils.sha256Twice(ByteUtils.concat(lastHash, lastHash));
            parentHashList.add(lastParent);
        }
        return doRehash(parentHashList);
    }

}
