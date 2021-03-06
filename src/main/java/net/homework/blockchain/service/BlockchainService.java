package net.homework.blockchain.service;

import lombok.SneakyThrows;
import net.homework.blockchain.Config;
import net.homework.blockchain.client.Node;
import net.homework.blockchain.client.NodeImpl;
import net.homework.blockchain.entity.Block;
import net.homework.blockchain.entity.Transaction;
import net.homework.blockchain.repo.BlockRepository;
import net.homework.blockchain.repo.InputRepository;
import net.homework.blockchain.repo.OutputRepository;
import net.homework.blockchain.repo.TransactionRepository;
import net.homework.blockchain.util.CryptoUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

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

    private Thread node;

    public Block getBlockOnChainByHash(byte[] headerHash) {
        return blockRepository.findBlockByHashBlock(headerHash).orElse(null);
    }

    public Block getBlockOnChainByHeight(long height) {
        return blockRepository.findById(height).orElse(null);
    }

    public boolean isSonPresentForParentBlock(byte[] hashPrevBlock) {
        return blockRepository.existsBlockByHeader_HashPrevBlock(hashPrevBlock);
    }

    public void addBlockToChain(Block block) {
        blockRepository.save(block);
    }

    public boolean isOutputSpentOnChain(byte[] refOut, int outIndex) {
        return inputRepository.findInputByPreviousTransactionHashAndOutIndex(refOut, outIndex).isPresent();
    }

    public Transaction getTransactionOnChain(byte[] txHash) {
        return transactionRepository.findTransactionByHashTx(txHash).orElse(null);
    }

    public boolean isCoinbaseTxImmature(byte[] coinbaseTxHash) {
        Optional<Transaction> optTx = transactionRepository.findTransactionByHashTx(coinbaseTxHash);
        // if coinbase is on chain
        if (optTx.isPresent()) {
            Optional<Block> optBlock = blockRepository.findByTransactions_HashTxEquals(optTx.get().hashTransaction());
            if (optBlock.isPresent()) {
                Block block = optBlock.get();
                long height = block.getHeight();
                return !blockRepository.findById(height + Config.COINBASE_MATURITY).isPresent();
            }
        }
        // coinbase not on chain, immature
        return true;
    }

    @Deprecated
    public Map<ByteBuffer, List<Integer>> getUTXOs(byte[] publicKeyHash) {
        // better implementation probably possible, but this will work for now
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

    public Map<ByteBuffer, List<Integer>> getUTXOsNEW(byte[] publicKey) {
        byte[] publicKeyHash = CryptoUtils.hashPublicKeyBytes(publicKey);
        Map<ByteBuffer, List<Integer>> utxos = new HashMap<>();
        // 0: find all txs containing outputs that this address has ever received
        Set<Transaction> txs = transactionRepository.findUniqueTransactionsByOutputs_ScriptPubKeyHash(publicKeyHash);
        txs.forEach(tx -> {
            ByteBuffer key = ByteBuffer.wrap(tx.hashTransaction());
            // 1: for each output in each tx
            for (int i = 0; i < tx.getOutputs().size(); i++) {
                // 2: if this output belongs to this address, and it's not spent, map it
                if (Arrays.equals(tx.getOutputs().get(i).getScriptPubKeyHash(), publicKeyHash)) {
                    if (!isOutputSpentOnChain(key.array(), i)) {
                        if (!utxos.containsKey(key)) {
                            utxos.put(key, new ArrayList<>());
                        }
                        utxos.get(key).add(i);
                    }
                }
            }
        });
        return utxos;
    }

    public long getBalance(byte[] publicKey) {
        Map<ByteBuffer, List<Integer>> utxos = getUTXOsNEW(publicKey);
        AtomicLong bal = new AtomicLong();
        utxos.forEach((txHash, indexes) -> {
            List<Transaction.Output> outputs = getTransactionOnChain(txHash.array()).getOutputs();
            indexes.forEach(index -> bal.addAndGet(outputs.get(index).getValue()));
        });
        return bal.get();
    }

    public List<Transaction> getTransactionsByPublicKey(byte[] publicKey) {
        return transactionRepository.findUniqueTransactionsByInputs_ScriptPubKeyEqualsOrOutputs_ScriptPubKeyHashEqualsOrderByDummyIdDesc(publicKey, CryptoUtils.hashPublicKeyBytes(publicKey));
    }

    @SneakyThrows
    @PostConstruct
    // hard-coded genesis block to avoid split branches
    public void initGenesisBlock() {
        if (!blockRepository.findById(1L).isPresent()) {
            Transaction.Input input = new Transaction.Input(new byte[]{0}, -1, new byte[]{0}, new byte[]{0});
            Transaction.Output output = new Transaction.Output(Config.BLOCK_FEE, CryptoUtils.getPublicKeyHashFromAddress("16SChybffW7NEM7L9Nq78K2PQTV2NPCEFn"));
            Transaction tx = new Transaction(Collections.singletonList(input), Collections.singletonList(output));
            Block genesis = new Block(new byte[]{0}, Collections.singletonList(tx));
            genesis.getHeader().setNonce(4913801);
            genesis.getHeader().setTime(1637148832393L);
            addBlockToChain(genesis);
        }
    }

    @PostConstruct
    public void startNode() {
        node = new Thread(() -> {
            synchronized (this) {
                Node node = null;
                try {
                    node = new NodeImpl();
                    node.init();
                    wait();
                } catch (SocketException e) {
                    e.printStackTrace();
                } catch (InterruptedException e) {
                    node.gracefulShutdown();
                }
            }
        });
        node.start();
    }

    @PreDestroy
    public void stopNode() {
        node.interrupt();
        try {
            node.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public long getCurrentBlockHeight() {
        return blockRepository.count();
    }

    public byte[] getLatestBlockHash() {
        return blockRepository.findById(getCurrentBlockHeight()).get().getHashBlock();
    }
}
