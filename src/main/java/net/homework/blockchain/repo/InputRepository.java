package net.homework.blockchain.repo;

import net.homework.blockchain.entity.Transaction;
import org.springframework.data.repository.CrudRepository;

import java.util.Optional;

public interface InputRepository extends CrudRepository<Transaction.Input, Integer> {
    Optional<Transaction.Input> findInputByPreviousTransactionHashAndOutIndex(byte[] previousTransactionHash, int outIndex);
}
