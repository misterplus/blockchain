package net.homework.blockchain.util;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

public class NetworkUtils {
    public static void broadcast(int portOut, byte[] data, int portIn) {
        new Thread(() -> {
            try {
                DatagramSocket socket = new DatagramSocket(portOut, InetAddress.getLocalHost());
                socket.setBroadcast(true);
                DatagramPacket packet = new DatagramPacket(data, data.length, InetAddress.getByName("255.255.255.255"), portIn);
                socket.send(packet);
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();
    }
}
