package net.homework.blockchain.client;

import net.homework.blockchain.bean.Transaction;
import org.apache.commons.codec.DecoderException;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.math.BigInteger;
import java.net.UnknownHostException;
import java.security.InvalidAlgorithmParameterException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.spec.InvalidKeySpecException;
import java.util.Map;

public interface User {
    String generatePrivateKey() throws NoSuchAlgorithmException, InvalidAlgorithmParameterException, IOException;
    BigInteger loadPrivateKey() throws IOException, DecoderException;
    char[] getPublicKey(BigInteger privateKey) throws NoSuchAlgorithmException, NoSuchProviderException, InvalidKeySpecException;
    String getAddress(char[] publicKey) throws DecoderException;

    /**
     *
     * @param recipientsWithAmount publicKeyHash, value
     * @return
     */
    Transaction assembleTx(Map<byte[], byte[]> recipientsWithAmount) throws DecoderException, IOException, NoSuchAlgorithmException, InvalidKeySpecException, NoSuchProviderException;
    void broadcastTx(Transaction tx) throws IOException;
    /**
     * Get unspent transaction outputs.
     * @return transactionHash, outIndex
     */
    Map<Transaction, Integer> getUTXOs();
}
