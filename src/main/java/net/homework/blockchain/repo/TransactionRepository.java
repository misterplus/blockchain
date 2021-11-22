package net.homework.blockchain.repo;

import net.homework.blockchain.entity.Transaction;
import org.springframework.data.repository.CrudRepository;

import java.util.Optional;
import java.util.Set;

public interface TransactionRepository extends CrudRepository<Transaction, Integer> {
    Optional<Transaction> findTransactionByHashTx(byte[] hashTx);

    Transaction findTransactionByOutputsContains(Transaction.Output output);

    // find transactions whose outputs contain scriptPubKeyHash (find txs received by an address)
    Set<Transaction> findUniqueTransactionsByOutputs_ScriptPubKeyHash(byte[] scriptPubKeyHash);

    // find transactions whose inputs contain scriptPubKey (find txs spent by an address)
    Set<Transaction> findUniqueTransactionsByInputs_ScriptPubKey(byte[] scriptPubKey);
}
