package net.homework.blockchain.util;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

public class NetworkUtils {
    public static void broadcastAsync(int portOut, byte[] data, int portIn) {
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

    public static void sendPacket(InetAddress address, int portOut, byte[] data, int portIn) {
        try {
            DatagramSocket socket = new DatagramSocket(portOut, InetAddress.getLocalHost());
            DatagramPacket packet = new DatagramPacket(data, data.length, address, portIn);
            socket.send(packet);
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void sendPacketAsync(InetAddress address, int portOut, byte[] data, int portIn) {
        new Thread(() -> {
            sendPacket(address, portOut, data, portIn);
        }).start();
    }
}
