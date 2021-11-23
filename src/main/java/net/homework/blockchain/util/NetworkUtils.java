package net.homework.blockchain.util;

import net.homework.blockchain.Config;

import java.io.IOException;
import java.net.*;

public class NetworkUtils {
    public synchronized static void sendPacket(DatagramSocket socket, byte[] data, InetAddress address) {
        DatagramPacket packet = new DatagramPacket(data, 0, data.length, address, Config.PORT_IN);
        try {
            socket.send(packet);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public synchronized static void broadcast(DatagramSocket socket, byte[] data) {
        try {
            DatagramPacket packet = new DatagramPacket(data, 0, data.length, InetAddress.getByName("255.255.255.255"), Config.PORT_IN);
            socket.setBroadcast(true);
            socket.send(packet);
            socket.setBroadcast(false);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
