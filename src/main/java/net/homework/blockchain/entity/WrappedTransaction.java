package net.homework.blockchain.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import net.homework.blockchain.util.ByteUtils;
import net.homework.blockchain.util.MsgUtils;

@NoArgsConstructor
@Data
@AllArgsConstructor
public class WrappedTransaction implements Comparable<Long>, IMessage {

    private Transaction tx;
    private long fee;

    public static WrappedTransaction wrap(Transaction tx, long fee) {
        return new WrappedTransaction(tx, fee);
    }

    @Override
    public int compareTo(Long o) {
        return Long.compare(this.fee, o);
    }

    @Override
    public byte msgType() {
        return MsgUtils.TX_POOL_ADD;
    }

    @Override
    public byte[] toBytes() {
        return ByteUtils.toBytes(this);
    }
}
