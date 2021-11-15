package net.homework.blockchain.sql.dao;

import net.homework.blockchain.bean.Block;

public interface BlockDao {
    Block getBlock(byte[] headerHash);
    boolean isSonPresentForParentBlock(byte[] parentBlockHash);
}
