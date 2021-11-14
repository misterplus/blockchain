package net.homework.blockchain.client;

import net.homework.blockchain.bean.Transaction;

import java.util.Map;

public interface User {
    String generatePrivateKey();
    String loadPrivateKey();
    String getPublicKey(String privateKey);
    String getAddress(String publicKey);

    /**
     *
     * @param recipientsWithAmount publicKeyHash, value
     * @return
     */
    Transaction assembleTx(Map<byte[], byte[]> recipientsWithAmount);
    void broadcastTx(Transaction tx);
    /**
     * Get unspent transaction outputs.
     * @return transactionHash, outIndex
     */
    Map<Transaction, Integer> getUTXOs();
}
