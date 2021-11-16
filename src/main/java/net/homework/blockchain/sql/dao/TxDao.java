package net.homework.blockchain.sql.dao;

import net.homework.blockchain.bean.Transaction;

public interface TxDao {
    Transaction getTx(byte[] txHash);
    boolean isCoinbaseTxMature(byte[] coinbaseTxHash);
}
