package net.homework.blockchain.bean;

import net.homework.blockchain.util.ByteUtils;
import net.homework.blockchain.util.CryptoUtils;

import java.util.List;

public class Transaction {

    private static class Input {
        private byte[] previousTransactionHash;
        private int outIndex;
        private byte[] scriptSig;
        private byte[] scriptPubKey;

        /**
         *
         * @param previousTransactionHash previous tx to redeem
         * @param outIndex index of the to-be-redeemed output in the previous tx
         * @param scriptSig a signature of previousTransactionHash, signed by the redeemer's private key
         * @param scriptPubKey redeemer's public key, used to verify scriptSig
         */
        public Input(byte[] previousTransactionHash, int outIndex, byte[] scriptSig, byte[] scriptPubKey) {
            this.previousTransactionHash = previousTransactionHash;
            this.outIndex = outIndex;
            this.scriptSig = scriptSig;
            this.scriptPubKey = scriptPubKey;
        }
    }

    private static class Output {
        private byte[] value;
        private byte[] scriptPubKeyHash;

        /**
         *
         * @param value amount of coins to be sent
         * @param scriptPubKeyHash hash of the public key of the recipient
         */
        public Output(byte[] value, byte[] scriptPubKeyHash) {
            this.value = value;
            this.scriptPubKeyHash = scriptPubKeyHash;
        }
    }

    private List<Input> inputs;
    private List<Output> outputs;
    /**
     * Only present in a coinbase transaction. <br>
     * When nonce overflows, this increments.
     */
    private Integer extraNonce;

    /**
     * Construct a normal transaction, do not initialize extraNonce.
     * @param inputs
     * @param outputs
     */
    public Transaction(List<Input> inputs, List<Output> outputs) {
        this.inputs = inputs;
        this.outputs = outputs;
    }

    /**
     * Construct a coinbase transaction, initialize extraNonce.
     * @param outputs
     */
    public Transaction(List<Output> outputs) {
        this(null, outputs);
        this.extraNonce = 0;
    }

    public byte[] hashTransaction() {
        return CryptoUtils.sha256(ByteUtils.toBytes(this));
    }
}
