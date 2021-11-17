package net.homework.blockchain.sql.repo;

import net.homework.blockchain.bean.Transaction;
import org.springframework.data.repository.CrudRepository;

public interface TransactionRepository extends CrudRepository<Transaction, String> {
}
