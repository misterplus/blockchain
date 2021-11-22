package net.homework.blockchain.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import net.homework.blockchain.util.ByteUtils;
import net.homework.blockchain.util.MsgUtils;

@NoArgsConstructor
@Data
@AllArgsConstructor
public class WrappedTransaction implements Comparable<Long> {

    private Transaction tx;
    private long fee;

    public static WrappedTransaction wrap(Transaction tx, long fee) {
        return new WrappedTransaction(tx, fee);
    }

    @Override
    public int compareTo(Long o) {
        return Long.compare(this.fee, o);
    }

    public byte[] toBytes() {
        return ByteUtils.toBytes(this);
    }

    public byte[] toMsg() {
        byte[] part = toBytes();
        byte[] msg = new byte[part.length + 1];
        msg[0] = MsgUtils.TX_POOL_ADD;
        System.arraycopy(part, 0, msg, 1, part.length);
        return msg;
    }
}
