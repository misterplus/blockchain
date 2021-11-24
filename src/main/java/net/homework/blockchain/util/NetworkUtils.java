package net.homework.blockchain.util;

import java.io.IOException;
import java.net.*;
import java.util.Set;

public class NetworkUtils {
    public synchronized static void sendPacket(DatagramSocket socket, byte[] data, InetAddress address, int port) {
        DatagramPacket packet = new DatagramPacket(data, 0, data.length, address, port);
        try {
            socket.send(packet);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public synchronized static void multicastToPeers(DatagramSocket socket, byte[] data, Set<InetAddress> peers, int port) {
        peers.forEach(peer -> sendPacket(socket, data, peer, port));
    }

    public synchronized static void broadcast(DatagramSocket socket, byte[] data, int port) {
        try {
            DatagramPacket packet = new DatagramPacket(data, 0, data.length, InetAddress.getByName("255.255.255.255"), port);
            socket.setBroadcast(true);
            socket.send(packet);
            socket.setBroadcast(false);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
