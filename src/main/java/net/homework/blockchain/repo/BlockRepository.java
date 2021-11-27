package net.homework.blockchain.repo;

import net.homework.blockchain.entity.Block;
import org.springframework.data.repository.CrudRepository;

import java.util.Optional;

public interface BlockRepository extends CrudRepository<Block, Long> {

    boolean existsBlockByHeader_HashPrevBlock(byte[] hashPrevBlock);

    Optional<Block> findBlockByHashBlock(byte[] hashBlock);

    // find block which tx is in
    Optional<Block> findByTransactions_HashTxEquals(byte[] hashTx);

}