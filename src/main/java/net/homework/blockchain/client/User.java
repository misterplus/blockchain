package net.homework.blockchain.client;

import net.homework.blockchain.entity.Transaction;
import org.bouncycastle.jce.interfaces.ECPrivateKey;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.security.InvalidAlgorithmParameterException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.util.List;
import java.util.Map;

public interface User {
    ECPrivateKey generatePrivateKey() throws NoSuchAlgorithmException, InvalidAlgorithmParameterException, NoSuchProviderException;

    ECPrivateKey loadPrivateKey() throws IOException;

    /**
     * @param recipientsWithAmount publicKeyHash, value
     * @return the assembled transaction
     */
    Transaction assembleTx(Map<byte[], Long> recipientsWithAmount);

    void broadcastTx(Transaction tx);

    /**
     * Get unspent transaction outputs.
     *
     * @return transactionHash, outIndex
     */
    Map<ByteBuffer, List<Integer>> getUTXOs();
}