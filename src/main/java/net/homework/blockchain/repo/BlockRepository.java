package net.homework.blockchain.repo;

import net.homework.blockchain.entity.Block;
import net.homework.blockchain.entity.Transaction;
import org.springframework.data.repository.CrudRepository;

import java.util.Optional;

public interface BlockRepository extends CrudRepository<Block, String> {
    // find block by hashPrevBlock
    Optional<Block> findBlockByHeader_HashPrevBlock(byte[] hashPrevBlock);
    // find block which tx is in
    Optional<Block> findBlockByTransactionsContains(Transaction tx);
    Optional<Block> findBlockByHeight(long height);
}