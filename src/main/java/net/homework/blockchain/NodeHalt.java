package net.homework.blockchain;

import lombok.SneakyThrows;
import net.homework.blockchain.util.MsgUtils;
import net.homework.blockchain.util.NetworkUtils;

import java.net.InetAddress;

public class NodeHalt {
    @SneakyThrows
    public static void main(String[] args) {
        NetworkUtils.sendPacket(InetAddress.getLocalHost(), Config.PORT_LOCAL_HALT_OUT, new byte[]{MsgUtils.HALT}, Config.PORT_LOCAL_HALT_IN);
    }
}
