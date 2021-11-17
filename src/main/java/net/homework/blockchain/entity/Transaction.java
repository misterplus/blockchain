package net.homework.blockchain.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import lombok.NoArgsConstructor;
import net.homework.blockchain.util.ByteUtils;
import net.homework.blockchain.util.CryptoUtils;
import org.apache.commons.codec.binary.Hex;
import org.hibernate.annotations.Immutable;

import javax.persistence.*;
import java.io.Serializable;
import java.util.List;

@NoArgsConstructor
@Data
@Entity
@Immutable
public class Transaction {


    private List<Input> inputs;
    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderColumn(name = "input_index_in_tx")
    public List<Input> getInputs() {
        return inputs;
    }
    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderColumn(name = "output_index_in_tx")
    public List<Output> getOutputs() {
        return outputs;
    }


    private List<Output> outputs;

    @JsonIgnore
    private String hashTx;

    @Id
    @Column(length = 64)
    public String getHashTx() {
        return Hex.encodeHexString(hashTransaction(), false);
    }

    /**
     * Construct a normal transaction.
     *
     * @param inputs
     * @param outputs
     */
    public Transaction(List<Input> inputs, List<Output> outputs) {
        this.inputs = inputs;
        this.outputs = outputs;
    }

    public byte[] hashTransaction() {
        return CryptoUtils.sha256(toBytes());
    }

    public byte[] toBytes() {
        return ByteUtils.toBytes(this);
    }

    @Data
    @Entity
    @Immutable
    public static class Input implements Serializable {
        private static final long serialVersionUID = 1L;

        private byte[] previousTransactionHash;
        private int outIndex;
        private byte[] scriptSig;
        private byte[] scriptPubKey;

        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        @JsonIgnore
        private int dummyId;

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
        public Input() {
            this.previousTransactionHash = new byte[]{0};
            this.outIndex = -1;
            this.scriptSig = new byte[]{0};
            this.scriptPubKey = new byte[]{0};
        }
    }

    @NoArgsConstructor
    @Data
    @Entity
    @Immutable
    public static class Output {
        private long value;
        private byte[] scriptPubKeyHash;

        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        @JsonIgnore
        private int dummyId;

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
