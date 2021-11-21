package net.homework.blockchain.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@Data
@AllArgsConstructor
public class WrappedTransaction implements Comparable<Long> {

    private Transaction tx;
    private long fee;

    @Override
    public int compareTo(Long o) {
        return Long.compare(this.fee, o);
    }
}
