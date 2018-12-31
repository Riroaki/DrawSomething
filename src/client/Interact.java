package client;

public interface Interact {
    void die(int status);

    void sendMsg(String msg);

    String recvMsg();
}
