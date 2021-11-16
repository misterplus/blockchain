package net.homework.blockchain;

import net.homework.blockchain.client.User;
import net.homework.blockchain.client.UserImpl;

public class Test {
    public static void main(String[] args) {
        User user = new UserImpl();
        user.generatePrivateKey();
    }
}
