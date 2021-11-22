package net.homework.blockchain.entity;

public interface IMessage {
    byte msgType();
    byte[] toBytes();
    default byte[] toMsg() {
        byte[] part = this.toBytes();
        byte[] msg = new byte[part.length + 1];
        msg[0] = msgType();
        System.arraycopy(part, 0, msg, 1, part.length);
        return msg;
    }
}
