package net.homework.blockchain.controller;

import net.homework.blockchain.entity.Block;
import net.homework.blockchain.entity.Transaction;
import net.homework.blockchain.service.BlockchainService;
import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/")
public class BlockchainController {

    @Autowired
    private BlockchainService blockchainService;

    @RequestMapping("block/{height}")
    public Block getBlockByHeight(@PathVariable long height) {
        return height < 1L ? null : blockchainService.getBlockOnChainByHeight(height);
    }

    @RequestMapping("block")
    public Block getBlockByHash(@RequestParam String headerHash) {
        try {
            byte[] headerBytes = Hex.decodeHex(headerHash);
            return blockchainService.getBlockOnChainByHash(headerBytes);
        } catch (DecoderException e) {
            return null;
        }
    }

    @RequestMapping("tx")
    public Transaction getTransactionByHash(@RequestParam String txHash) {
        try {
            return blockchainService.getTransactionOnChain(Hex.decodeHex(txHash));
        } catch (DecoderException e) {
            return null;
        }
    }

    @RequestMapping("wallet/txs")
    public List<Transaction> getTransactionsByPublicKey(@RequestParam String publicKey) {
        try {
            return blockchainService.getTransactionsByPublicKey(Hex.decodeHex(publicKey));
        } catch (DecoderException e) {
            return null;
        }
    }

    @RequestMapping("wallet/bal")
    public long getBalanceByPublicKey(@RequestParam String publicKey) {
        try {
            return blockchainService.getBalance(Hex.decodeHex(publicKey));
        } catch (DecoderException e) {
            return -1;
        }
    }
}
