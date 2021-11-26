package net.homework.blockchain.client;

import lombok.SneakyThrows;
import org.bouncycastle.jce.interfaces.ECPublicKey;

import static net.homework.blockchain.util.CryptoUtils.generatePublicKey;
import static net.homework.blockchain.util.CryptoUtils.getAddressFromPublicKey;


public class GenPubKey {
    @SneakyThrows
    public static void main(String[] args) {
        UserImpl userimpl = new UserImpl();
        ECPublicKey publicKey = generatePublicKey(userimpl.generatePrivateKey());
        System.out.println(getAddressFromPublicKey(publicKey));
    }
}
