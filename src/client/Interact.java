package client;

// An interface that regulates the client's callbacks.
public interface Interact {
    void die(int status);

    void sendMsg(String msg);

    String recvMsg();
}
