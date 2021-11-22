package net.homework.blockchain.repo;

import net.homework.blockchain.entity.Transaction;
import org.springframework.data.repository.CrudRepository;

import java.util.List;

public interface OutputRepository extends CrudRepository<Transaction.Output, Integer> {
    List<Transaction.Output> findOutputsByScriptPubKeyHash(byte[] scriptPubKeyHash);
}
