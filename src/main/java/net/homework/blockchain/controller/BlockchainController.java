package net.homework.blockchain.controller;

import net.homework.blockchain.bean.Block;
import net.homework.blockchain.sql.repo.BlockRepository;
import net.homework.blockchain.sql.repo.InputRepository;
import net.homework.blockchain.sql.repo.TransactionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

@Controller
public class BlockchainController {
    @Autowired
    private BlockRepository blockRepository;
    @Autowired
    private TransactionRepository transactionRepository;
    @Autowired
    private InputRepository inputRepository;

    public Block getBlock(String headerHash) {
        return blockRepository.findById(headerHash).orElse(null);
    }

    public boolean isSonPresentForParentBlock(byte[] hashPrevBlock) {
        return blockRepository.findBlockByHeader_HashPrevBlock(hashPrevBlock).isPresent();
    }

    public void addBlock(Block block) {
       blockRepository.save(block);
    }

    public boolean isOutputSpentOnChain(byte[] refOut, int outIndex) {
        return inputRepository.findInputByPreviousTransactionHashAndOutIndex(refOut, outIndex).isPresent();
    }
}
