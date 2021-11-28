package net.homework.blockchain.util;

import net.homework.blockchain.Config;
import net.homework.blockchain.SpringContext;
import net.homework.blockchain.entity.Block;
import net.homework.blockchain.entity.Transaction;
import net.homework.blockchain.entity.WrappedTransaction;
import net.homework.blockchain.service.BlockchainService;

import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.*;
import java.util.stream.LongStream;

public class VerifyUtils {

    private static final BlockchainService blockchainService = SpringContext.getBean(BlockchainService.class);

    public static boolean isListEmpty(List<?> list) {
        return list == null || list.isEmpty();
    }

    public static boolean isCoinbaseTx(Transaction tx) {
        if (tx.getInputs().size() == 1) {
            return isCoinbaseInput(tx.getInputs().get(0));
        } else {
            return false;
        }
    }

    public static boolean isCoinbasePointedToLastBlock(Transaction tx, byte[] hashPrevBlock) {
        return Arrays.equals(tx.getInputs().get(0).getScriptSig(), hashPrevBlock);
    }

    public static boolean isCoinbaseInput(Transaction.Input input) {
        return ByteUtils.isZero(input.getPreviousTransactionHash()) && input.getOutIndex() == -1;
    }

    public static boolean isOutputPresentInTx(Transaction toCheck, byte[] refOut, int outIndex) {
        return Arrays.equals(toCheck.hashTransaction(), refOut) && toCheck.getOutputs().size() > outIndex;
    }

    public static boolean verifyInput(Transaction.Input input) {
        return CryptoUtils.verifyTransaction(input.getPreviousTransactionHash(), input.getScriptSig(), CryptoUtils.assemblePublicKey(input.getScriptPubKey()));
    }

    public static boolean isMoneyValueIllegal(long value) {
        return value < 0L;
    }

    /**
     * Convenience method for performing 2-4 checks (part of 4 is returned) on a transaction, since it's checked again in a block msg
     *
     * @param tx the transaction to perform checks on
     * @return the output sum, to be used later in checks
     */
    public static long basicTxCheck(Transaction tx) {
        // Make sure neither in or out lists are empty
        if (isListEmpty(tx.getInputs()) || isListEmpty(tx.getOutputs())) {
            return -1L;
        }
        // Size in bytes <= MAX_BLOCK_SIZE
        if (tx.toBytes().length > Config.MAX_BLOCK_SIZE) {
            return -1L;
        }
        // Each output value must be in legal money range
        long outputSum = 0;
        long outputValue;
        for (Transaction.Output output : tx.getOutputs()) {
            outputValue = output.getValue();
            if (isMoneyValueIllegal(outputValue)) {
                return -1L;
            } else {
                outputSum += outputValue;
            }
        }
        return outputSum;
    }

    public static synchronized boolean verifyTx(Transaction tx, Map<ByteBuffer, WrappedTransaction> txPool, Map<ByteBuffer, Transaction> orphanTxs, DatagramSocket socketOut, Set<InetAddress> peers) {
        // Check syntactic correctness
        if (tx == null) {
            return false;
        }

        // Total output value must be in legal money range
        long outputSum = basicTxCheck(tx);
        if (isMoneyValueIllegal(outputSum)) {
            return false;
        }

        // Make sure none of the inputs have hash=0, n=-1 (coinbase transactions)
        if (tx.getInputs().stream().anyMatch(VerifyUtils::isCoinbaseInput)) {
            return false;
        }
        // Reject if we already have matching tx in the pool, or in a block in the main branch
        byte[] txHash = tx.hashTransaction();
        // if it's a orphan who found parents, remove it from orphan pool
        orphanTxs.remove(ByteBuffer.wrap(txHash));
        if (txPool.containsKey(ByteBuffer.wrap(txHash)) || blockchainService.getTransactionOnChain(txHash) != null) {
            return false;
        }
        byte[] refOut;
        int outIndex;
        Transaction refOutTx;
        long inputSum = 0;
        long inputValue;
        // For each input,
        for (Transaction.Input input : tx.getInputs()) {
            refOut = input.getPreviousTransactionHash();
            outIndex = input.getOutIndex();
            refOutTx = null;
            for (WrappedTransaction wrappedTxInPool : txPool.values()) {
                Transaction txInPool = wrappedTxInPool.getTx();
                for (Transaction.Input inputInPool : txInPool.getInputs()) {
                    // if the referenced output is spent by any other transaction in the pool, reject this transaction.
                    if (Arrays.equals(inputInPool.getPreviousTransactionHash(), refOut) && inputInPool.getOutIndex() == outIndex) {
                        return false;
                    }
                }

                // find the referenced output transaction in pool, if present, this tx is not orphan
                if (isOutputPresentInTx(txInPool, refOut, outIndex)) {
                    refOutTx = txInPool;
                }
            }
            // if it's still orphan, try to find the referenced output on-chain, if found, it's not orphan
            if (refOutTx == null) {
                Transaction findOnChain = blockchainService.getTransactionOnChain(refOut);
                refOutTx = findOnChain != null && isOutputPresentInTx(findOnChain, refOut, outIndex) ? findOnChain : null;
            }
            // add to the orphan txs if is orphan
            if (refOutTx == null) {
                orphanTxs.putIfAbsent(ByteBuffer.wrap(tx.hashTransaction()), tx);
                // TODO: remove orphanTxs who stayed too long?
                return true;
            } else {
                // if the referenced output is coinbase, it must be matured, or we reject it
                if (isCoinbaseTx(refOutTx) && blockchainService.isCoinbaseTxImmature(refOutTx.hashTransaction())) {
                    return false;
                }
                // if the referenced output is spent on chain, reject it
                if (blockchainService.isOutputSpentOnChain(refOut, outIndex)) {
                    return false;
                }
                /*
                    Using the referenced output transactions to get input values,
                    check that each input value, as well as the sum, are in legal money range
                 */
                inputValue = refOutTx.getOutputs().get(outIndex).getValue();
                if (inputValue < 0L) {
                    return false;
                } else {
                    inputSum += inputValue;
                }
            }
        }
        // Reject if the sum of input values < sum of output values
        // Reject if transaction fee (defined as sum of input values minus sum of output values) would be too low to get into an empty block
        if (isMoneyValueIllegal(inputSum) || inputSum <= outputSum) {
            return false;
        }

        // Verify the scriptPubKey accepts for each input; reject if any are bad
        if (!tx.getInputs().stream().allMatch(VerifyUtils::verifyInput)) {
            return false;
        }

        // Add to transaction pool
        WrappedTransaction wrapped = WrappedTransaction.wrap(tx, inputSum - outputSum);
        txPool.put(ByteBuffer.wrap(txHash), wrapped);

        // Broadcast transaction to our peers
        NetworkUtils.multicastToPeers(socketOut, tx.toMsg(), peers, Config.PORT_NODE_IN);

        // send new tx in pool (txHash/tx) to miners
        NetworkUtils.broadcast(socketOut, wrapped.toMsg(), Config.PORT_MINER_IN);

        /*
            For each orphan transaction that uses this one as one of its inputs,
            run all these steps (including this one) recursively on that orphan.
         */
        for (Transaction orphan : orphanTxs.values()) {
            if (orphan.getInputs().stream().anyMatch(input -> isTxSpentByInput(tx, input))) {
                verifyTx(orphan, txPool, orphanTxs, socketOut, peers);
            }
        }
        return true;
    }

    public static boolean isTxSpentByInput(Transaction tx, Transaction.Input input) {
        return Arrays.equals(input.getPreviousTransactionHash(), tx.hashTransaction()) && tx.getOutputs().size() > input.getOutIndex();
    }

    public static synchronized boolean verifyBlock(Block block, InetAddress fromPeer, Map<ByteBuffer, Block> orphanBlocks, Map<ByteBuffer, WrappedTransaction> txPool, DatagramSocket socketOut, Set<InetAddress> peers) {
        // Check syntactic correctness
        if (block == null) {
            return false;
        }
        byte[] headerHash = block.hashHeader();
        // if it's a orphan who found parents, remove it from orphan pool
        orphanBlocks.remove(ByteBuffer.wrap(headerHash));
        List<Transaction> txs = block.getTransactions();
        // Reject if block is duplicated
        // Transaction list must be non-empty
        // Block hash must satisfy claimed nBits proof of work, matching the difficulty
        if (blockchainService.getBlockOnChainByHash(headerHash) != null || isListEmpty(txs) || !ByteUtils.isZero(Arrays.copyOf(headerHash, Config.DIFFICULTY))) {
            return false;
        }
        Block.Header header = block.getHeader();
        // Block timestamp must not be more than two hours in the future
        if (header.getTime() > System.currentTimeMillis() + 7200000L) {
            return false;
        }
        // First transaction must be coinbase (i.e. only 1 input, with hash=0, n=-1), the rest must not be
        if (!isCoinbaseTx(txs.get(0))) {
            return false;
        }
        // coinbase tx's scriptSig must point to hashPrevBlock (to preserve unique hash)
        if (!isCoinbasePointedToLastBlock(txs.get(0), header.getHashPrevBlock())) {
            return false;
        }
        if (txs.subList(1, txs.size()).stream().anyMatch(VerifyUtils::isCoinbaseTx)) {
            return false;
        }
        List<Long> outputSums = new ArrayList<>(txs.size());
        // For each transaction, apply "tx" checks 2-4
        if (txs.stream().anyMatch(tx -> {
            long outputSum = basicTxCheck(tx);
            outputSums.add(outputSum);
            return isMoneyValueIllegal(outputSum);
        })) {
            return false;
        }
        // Verify Merkle hash
        byte[] merkleHash = header.getHashMerkleRoot();
        block.reconstructMerkleTree();
        byte[] calculatedMerkleHash = header.getMerkleTree().hashMerkleTree();
        if (!Arrays.equals(merkleHash, calculatedMerkleHash)) {
            return false;
        }
        // Check if prev block is on-chain.
        Block prevBlock = blockchainService.getBlockOnChainByHash(header.getHashPrevBlock());
        if (prevBlock == null) {
            // orphan block, add this to orphan blocks
            orphanBlocks.put(ByteBuffer.wrap(headerHash), block);
            // then query peer we got this from for orphan's parent
            NetworkUtils.sendPacket(socketOut, MsgUtils.toBlockRequestMsg(headerHash), fromPeer, Config.PORT_NODE_IN);
        } else {
            // if prevBlock already has a son, we reject this block completely (no multi-branch implementation for simplicity reasons)
            if (!blockchainService.isSonPresentForParentBlock(prevBlock.hashHeader())) {
                // if prevBlock has no son, continue
                byte[] refOut;
                Transaction refOutTx;
                int outIndex;
                long inputValue;
                Iterator<Long> outSumIter = outputSums.iterator();
                // skips coinbase transaction
                outSumIter.next();
                long minerFeeSum = 0L;
                long minerFee;
                Map<ByteBuffer, List<Integer>> spentOutputInBlock = new HashMap<>();
                // For all but the coinbase transaction, apply the following:
                for (Transaction tx : txs.subList(1, txs.size())) {
                    // For each input, look in the main branch to find the referenced output transaction.
                    long inputSum = 0L;
                    for (Transaction.Input input : tx.getInputs()) {
                        refOut = input.getPreviousTransactionHash();
                        refOutTx = blockchainService.getTransactionOnChain(refOut);
                        outIndex = input.getOutIndex();
                        // Reject if the output transaction is missing for any input.
                        if ((refOutTx == null) ||
                                // if we are using the nth (index) output of the earlier transaction, but it has fewer than n+1 outputs, reject.
                                (refOutTx.getOutputs().size() <= outIndex) ||
                                // if the referenced output transaction is coinbase (i.e. only 1 input, with hash=0, n=-1),
                                // it must have at least COINBASE_MATURITY (10) confirmations; else reject.
                                (isCoinbaseTx(refOutTx) && blockchainService.isCoinbaseTxImmature(refOutTx.hashTransaction())) ||
                                // Verify crypto signatures for each input; reject if any are bad
                                (!verifyInput(input)) ||
                                // if the referenced output is spent on chain, reject it
                                (blockchainService.isOutputSpentOnChain(refOut, outIndex)) ||
                                // if the referenced output is spent by any other transaction in this block, reject this block
                                (spentOutputInBlock.containsKey(ByteBuffer.wrap(refOut)) && spentOutputInBlock.get(ByteBuffer.wrap(refOut)).contains(outIndex))) {
                            return false;
                        }
                        // Using the referenced output transactions to get input values,
                        // check that each input value, as well as the sum, are in legal money range
                        inputValue = refOutTx.getOutputs().get(outIndex).getValue();
                        if (isMoneyValueIllegal(inputValue)) {
                            return false;
                        } else {
                            inputSum += inputValue;
                        }
                        // update the spent list
                        if (spentOutputInBlock.containsKey(ByteBuffer.wrap(refOut))) {
                            // refOut duplicated, add the index to value list
                            spentOutputInBlock.get(ByteBuffer.wrap(refOut)).add(outIndex);
                        } else {
                            // refOut new, get a new list
                            spentOutputInBlock.put(ByteBuffer.wrap(refOut), Collections.singletonList(outIndex));
                        }
                    }
                    if (isMoneyValueIllegal(inputSum)) {
                        return false;
                    }
                    // Reject if the sum of input values <= sum of output values (must pay transaction fee)
                    minerFee = inputSum - outSumIter.next();
                    if (minerFee <= 0L) {
                        return false;
                    }
                    minerFeeSum += minerFee;
                }
                // Reject if coinbase value > sum of block creation fee and transaction fees
                if (getCoinbaseValue(txs.get(0)) > Config.BLOCK_FEE + minerFeeSum) {
                    return false;
                }
                // For each transaction in the block, delete any matching transaction from the transaction pool
                List<String> removedTxHashes = new ArrayList<>();
                txs.forEach(tx -> {
                    WrappedTransaction removed = txPool.remove(ByteBuffer.wrap(tx.hashTransaction()));
                    if (removed != null) {
                        removedTxHashes.add(removed.getTx().hashTransactionHex());
                    }
                });
                // send updated tx pool to miners (which txs are no longer in pool)
                NetworkUtils.broadcast(socketOut, MsgUtils.toRemoveMsg(removedTxHashes), Config.PORT_MINER_IN);

                // Add to chain
                blockchainService.addBlockToChain(block);
                // Broadcast block to our peers
                NetworkUtils.multicastToPeers(socketOut, block.toMsg(), peers, Config.PORT_NODE_IN);

                // For each orphan block for which this block is its prev, run all these steps (including this one) recursively on that orphan
                orphanBlocks.values().forEach(orphanBlock -> {
                    if (Arrays.equals(headerHash, orphanBlock.getHeader().getHashPrevBlock())) {
                        try {
                            verifyBlock(orphanBlock, InetAddress.getLocalHost(), orphanBlocks, txPool, socketOut, peers);
                        } catch (UnknownHostException e) {
                            e.printStackTrace();
                        }
                    }
                });
            }
        }
        return true;
    }

    public static long getCoinbaseValue(Transaction coinbaseTx) {
        return coinbaseTx.getOutputs().stream().flatMapToLong(output -> LongStream.of(output.getValue())).sum();
    }
}
