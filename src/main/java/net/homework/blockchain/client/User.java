package net.homework.blockchain.client;

import net.homework.blockchain.bean.Transaction;

public interface User {
    // 私钥生成
    String generatePriKey();
    // 构造转账
    Transaction assembleTx();
    // 广播转账
    void broadcastTx(Transaction tx);
}
