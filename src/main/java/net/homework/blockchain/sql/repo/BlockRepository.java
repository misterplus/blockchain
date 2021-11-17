package net.homework.blockchain.sql.repo;

import net.homework.blockchain.bean.Block;
import org.springframework.data.repository.CrudRepository;

import java.util.Optional;

public interface BlockRepository extends CrudRepository<Block, String> {
    Optional<Block> findBlockByHeader_HashPrevBlock(byte[] hashPrevBlock);
}