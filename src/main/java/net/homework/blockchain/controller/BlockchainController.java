package net.homework.blockchain.controller;

import net.homework.blockchain.entity.Block;
import net.homework.blockchain.entity.Transaction;
import net.homework.blockchain.service.BlockchainService;
import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.json.GsonJsonParser;
import org.springframework.web.bind.annotation.*;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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

    @RequestMapping("latestBlockHash")
    public String getLatestBlockHash() {
        return Hex.encodeHexString(blockchainService.getLatestBlockHash(), false);
    }

    @RequestMapping("wallet/utxo")
    public Map<String, String> getUTXOs(@RequestParam String publicKey) {
        try {
            Map<ByteBuffer, List<Integer>> utxos = blockchainService.getUTXOsNEW(Hex.decodeHex(publicKey));
            Map<String, String> ret = new HashMap<>();
            for (ByteBuffer key : utxos.keySet()) {
                ret.put(Hex.encodeHexString(key.array(), false),
                        utxos.get(key).stream()
                        .map(Object::toString)
                        .collect(Collectors.joining(",")));
            }
            return ret;
        } catch (DecoderException e) {
            return null;
        }
    }

    @RequestMapping("totalInput")
    public long getTotalInput(@RequestBody Map<String, String> map) {
        Map<String, List<Integer>> arg = new HashMap<>();
        for (String key : map.keySet()) {
            arg.put(key, Arrays.stream(map.get(key).split(",")).map(Integer::valueOf).collect(Collectors.toList()));
        }
        return blockchainService.getTotalInput(arg);
    }
}
