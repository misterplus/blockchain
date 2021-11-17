package net.homework.blockchain.repo;

import net.homework.blockchain.entity.Transaction;
import org.springframework.data.repository.CrudRepository;

import java.util.Optional;

public interface TransactionRepository extends CrudRepository<Transaction, String> {
}
