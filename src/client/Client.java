package client;

import client.UI.Login;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

public class Client implements Interact {
    private static final String IP_ADDR = "localhost";
    private static final int PORT = 12409;
    private Socket socket;
    private DataOutputStream output;
    private DataInputStream input;

    public static void main(String[] args) {
        System.out.println("Start client...");
        Client client = new Client();
        client.init();
        client.run();
    }

    private void init() {
        int reconnectTimes = 0;
        final int MAX_TRIAL = 10;
        while (true) {
            try {
                socket = new Socket(IP_ADDR, PORT);
                // Read data from server.
                input = new DataInputStream(socket.getInputStream());
                // Send data to server.
                output = new DataOutputStream(socket.getOutputStream());
                break;
            } catch (Exception e) {
                if (reconnectTimes++ == MAX_TRIAL)
                    die(1);
                System.out.println("Fail to connect server, retry in 2 sec... ");
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException ie) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void run() {
        // Enter the room.
        Login login = new Login(this);
        login.showAndReact();
    }

    @Override
    public void sendMsg(String msg) {
        try {
            output.writeUTF(msg);
        } catch (IOException e) {
            e.printStackTrace();
            die(1);
        }
    }

    @Override
    public String recvMsg() {
        try {
            return input.readUTF();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return "";
    }

    // Client dies.
    @Override
    public void die(int status) {
        sendMsg("quit");
        try {
            input.close();
            output.close();
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println("Client died.");
        System.exit(status);
    }
}