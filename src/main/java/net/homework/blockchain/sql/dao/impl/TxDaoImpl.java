package net.homework.blockchain.sql.dao.impl;

import net.homework.blockchain.bean.Transaction;
import net.homework.blockchain.sql.dao.TxDao;

public class TxDaoImpl implements TxDao {
    @Override
    public Transaction getTx(byte[] txHash) {
        return null;
    }

    @Override
    public boolean isCoinbaseTxMature(byte[] coinbaseTxHash) {
        return false;
    }
}
