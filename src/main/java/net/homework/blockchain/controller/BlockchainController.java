package net.homework.blockchain.controller;

import net.homework.blockchain.entity.Block;
import net.homework.blockchain.service.BlockchainService;
import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

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
            e.printStackTrace();
            return null;
        }
    }
}
