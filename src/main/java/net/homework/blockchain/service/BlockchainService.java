package net.homework.blockchain.service;

import net.homework.blockchain.Config;
import net.homework.blockchain.entity.Block;
import net.homework.blockchain.entity.Transaction;
import net.homework.blockchain.repo.BlockRepository;
import net.homework.blockchain.repo.InputRepository;
import net.homework.blockchain.repo.TransactionRepository;
import org.apache.commons.codec.binary.Hex;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class BlockchainService {
    @Autowired
    private BlockRepository blockRepository;
    @Autowired
    private TransactionRepository transactionRepository;
    @Autowired
    private InputRepository inputRepository;

    public Block getBlock(byte[] headerHash) {
        return blockRepository.findById(Hex.encodeHexString(headerHash, false)).orElse(null);
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

    public Transaction getTransaction(byte[] txHash) {
        return transactionRepository.findById(Hex.encodeHexString(txHash, false)).orElse(null);
    }

    public boolean isCoinbaseTxMature(byte[] coinbaseTxHash) {
        Block block = blockRepository.findBlockByTransactionsContains(transactionRepository.findById(Hex.encodeHexString(coinbaseTxHash, false)).get()).get();
        long height = block.getHeight();
        return blockRepository.findBlockByHeight(height + Config.COINBASE_MATURITY).isPresent();
    }
}
