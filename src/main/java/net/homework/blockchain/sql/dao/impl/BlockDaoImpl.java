package net.homework.blockchain.sql.dao.impl;

import net.homework.blockchain.bean.Block;
import net.homework.blockchain.sql.dao.BlockDao;

public class BlockDaoImpl implements BlockDao {
    @Override
    public Block getBlock(byte[] headerHash) {
        return null;
    }

    @Override
    public boolean isSonPresentForParentBlock(byte[] parentBlockHash) {
        return false;
    }

    @Override
    public boolean addBlock(Block block) {
        return false;
    }
}
