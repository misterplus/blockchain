package net.homework.blockchain.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class WrappedBlock {
    private Block block;
    private List<String> txHashes;

    private WrappedBlock(Block block) {
        this.block = block;
        if (block != null) {
            List<String> txHashes = new ArrayList<>();
            for (Transaction tx : block.getTransactions()) {
                txHashes.add(tx.hashTransactionHex());
            }
            this.txHashes = txHashes;
        }
    }

    public static WrappedBlock wrap(Block block) {
        return new WrappedBlock(block);
    }
}
