package net.homework.blockchain.sql.repo;

import net.homework.blockchain.bean.Transaction;
import org.springframework.data.repository.CrudRepository;

import java.util.Optional;

public interface InputRepository extends CrudRepository<Transaction.Input, Integer> {
    Optional<Transaction.Input> findInputByPreviousTransactionHashAndOutIndex(byte[] previousTransactionHash, int outIndex);
}
