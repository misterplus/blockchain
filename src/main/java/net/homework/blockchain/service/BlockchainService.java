package net.homework.blockchain.service;

import lombok.SneakyThrows;
import net.homework.blockchain.Config;
import net.homework.blockchain.entity.Block;
import net.homework.blockchain.entity.Transaction;
import net.homework.blockchain.repo.BlockRepository;
import net.homework.blockchain.repo.InputRepository;
import net.homework.blockchain.repo.OutputRepository;
import net.homework.blockchain.repo.TransactionRepository;
import net.homework.blockchain.util.CryptoUtils;
import org.apache.commons.codec.binary.Hex;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.nio.ByteBuffer;
import java.util.*;

@Service
public class BlockchainService {
    @Autowired
    private BlockRepository blockRepository;
    @Autowired
    private TransactionRepository transactionRepository;
    @Autowired
    private InputRepository inputRepository;
    @Autowired
    private OutputRepository outputRepository;

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
        return transactionRepository.findTransactionByHashTx(txHash).orElse(null);
    }

    public boolean isCoinbaseTxMature(byte[] coinbaseTxHash) {
        Block block = blockRepository.findBlockByTransactionsContains(transactionRepository.findTransactionByHashTx(coinbaseTxHash).get()).get();
        long height = block.getHeight();
        return blockRepository.findBlockByHeight(height + Config.COINBASE_MATURITY).isPresent();
    }

    public Map<ByteBuffer, List<Integer>> getUTXOs(byte[] publicKeyHash) {
        // TODO: better implementation probably possible
        // 0: find all outputs (spent or not) using this publicKeyHash
        List<Transaction.Output> historicalOutputs = outputRepository.findOutputsByScriptPubKeyHash(publicKeyHash);

        // 1: find all txs containing these outputs
        Set<Transaction> txs = new HashSet<>();
        for (Transaction.Output output : historicalOutputs) {
            txs.add(transactionRepository.findTransactionByOutputsContains(output));
        }

        // 2: look through these txs, map out outIndexes for these outputs
        Map<ByteBuffer, List<Integer>> utxo = new HashMap<>();
        txs.forEach(tx -> utxo.put(ByteBuffer.wrap(tx.hashTransaction()), new ArrayList<>()));
        txs.forEach(tx -> historicalOutputs.forEach(output -> {
            int outIndex = tx.getOutputs().indexOf(output);
            if (outIndex != -1) {
                List<Integer> indexList = utxo.get(ByteBuffer.wrap(tx.hashTransaction()));
                indexList.add(outIndex);
            }
        }));
        // 3: look through all inputs using publicKey, filter out spent txs
        utxo.forEach((txHash, outIndexes) -> outIndexes.removeIf(integer -> isOutputSpentOnChain(txHash.array(), integer)));
        return utxo;
    }

    @PostConstruct
    // hard-coded genesis block to avoid split branches
    public void initGenesisBlock() {
        if (!blockRepository.findBlockByHeight(0L).isPresent()) {
            Transaction.Input input = new Transaction.Input(new byte[]{0}, -1, new byte[]{0}, new byte[]{0});
            Transaction.Output output = new Transaction.Output(Config.BLOCK_FEE, CryptoUtils.getPublicKeyHashFromAddress("16SChybffW7NEM7L9Nq78K2PQTV2NPCEFn"));
            Transaction tx = new Transaction(Collections.singletonList(input), Collections.singletonList(output));
            Block genesis = new Block(new byte[]{0}, Collections.singletonList(tx));
            genesis.getHeader().setNonce(4913801);
            genesis.getHeader().setTime(1637148832393L);
            addBlock(genesis);
        }

//        Map<ByteBuffer, List<Integer>> test = getUTXOs(CryptoUtils.getPublicKeyHashFromAddress("16SChybffW7NEM7L9Nq78K2PQTV2NPCEFn"));
//        System.out.println("Test");
    }
}
