package net.homework.blockchain.client;

import net.homework.blockchain.entity.Transaction;

import java.nio.ByteBuffer;
import java.util.List;
import java.util.Map;

public interface User {
    String generatePrivateKey();

    String loadPrivateKey();

    String getPublicKey(String privateKey);

    String getAddress(String publicKey);

    /**
     * @param recipientsWithAmount publicKeyHash, value
     * @return the assembled transaction
     */
    Transaction assembleTx(Map<ByteBuffer, byte[]> recipientsWithAmount);

    void broadcastTx(Transaction tx);

    /**
     * Get unspent transaction outputs.
     *
     * @return transactionHash, outIndex
     */
    Map<ByteBuffer, List<Integer>> getUTXOs();
}
