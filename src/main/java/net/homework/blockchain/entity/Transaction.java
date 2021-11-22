package net.homework.blockchain.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import lombok.NoArgsConstructor;
import net.homework.blockchain.util.ByteUtils;
import net.homework.blockchain.util.CryptoUtils;
import net.homework.blockchain.util.MsgUtils;

import javax.persistence.*;
import java.util.List;

@NoArgsConstructor
@Data
@Entity
public class Transaction implements IMessage {
    // we use eager here cause everytime we fetch a transaction we would always need these lists
    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @OrderColumn(name = "input_index_in_tx")
    private List<Input> inputs;
    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @OrderColumn(name = "output_index_in_tx")
    private List<Output> outputs;

    @JsonIgnore
    private byte[] hashTx;

    @JsonIgnore
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long dummyId;

    /**
     * Construct a normal transaction.
     */
    public Transaction(List<Input> inputs, List<Output> outputs) {
        this.inputs = inputs;
        this.outputs = outputs;
    }

    @PrePersist
    public void preSave() {
        this.hashTx = hashTransaction();
    }

    public byte[] hashTransaction() {
        return CryptoUtils.sha256(toBytes());
    }

    @Override
    public byte msgType() {
        return MsgUtils.TX_NEW;
    }

    @Override
    public byte[] toBytes() {
        return ByteUtils.toBytes(this);
    }

    @Data
    @Entity
    @NoArgsConstructor
    public static class Input {

        private byte[] previousTransactionHash;
        private int outIndex;
        private byte[] scriptSig;
        private byte[] scriptPubKey;
        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        @JsonIgnore
        private long dummyId;

        /**
         * @param previousTransactionHash previous tx to redeem
         * @param outIndex                index of the to-be-redeemed output in the previous tx
         * @param scriptSig               a signature of previousTransactionHash, signed by the redeemer's private key
         * @param scriptPubKey            redeemer's public key, used to verify scriptSig
         */
        public Input(byte[] previousTransactionHash, int outIndex, byte[] scriptSig, byte[] scriptPubKey) {
            this.previousTransactionHash = previousTransactionHash;
            this.outIndex = outIndex;
            this.scriptSig = scriptSig;
            this.scriptPubKey = scriptPubKey;
        }

        /**
         * Construct a coinbase transaction input
         */
        public Input(byte[] hashPrevBlock) {
            this.previousTransactionHash = new byte[]{0};
            this.outIndex = -1;
            this.scriptSig = hashPrevBlock;
            this.scriptPubKey = new byte[]{0};
        }

        public void incrementExtraNonce() {
            if (ByteUtils.isZero(previousTransactionHash)) {
                outIndex++;
            }
        }
    }

    @NoArgsConstructor
    @Data
    @Entity
    public static class Output {
        private long value;
        private byte[] scriptPubKeyHash;

        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        @JsonIgnore
        private long dummyId;

        /**
         * @param value            amount of coins to be sent
         * @param scriptPubKeyHash hash of the public key of the recipient
         */
        public Output(long value, byte[] scriptPubKeyHash) {
            this.value = value;
            this.scriptPubKeyHash = scriptPubKeyHash;
        }
    }
}
