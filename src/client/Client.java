package client;

import client.UI.Login;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.Scanner;

public class Client implements Interact {
    private Socket socket;
    private DataOutputStream output;
    private DataInputStream input;

    public static void main(String[] args) {
        System.out.println("Start client...");
        Client client = new Client();
        if (client.init() != 0)
            System.exit(1);
        client.run();
    }

    private int init() {
        final int MAX_TRIAL = 10;
        Scanner s = new Scanner(System.in);

        System.out.print("Please input IP and PORT of the server:\n" +
                "default is `localhost:12409`, type `default` to use it;\n" +
                "or type `quit` to exit program.\n" +
                " >>");

        while (true) {
            // Default IP and port.
            int PORT = 12409;
            String IP_ADDR = "localhost";
            if (s.hasNextLine()) {
                String[] addr = s.nextLine().split(":");
                if ("quit".equals(addr[0]))
                    return 1;
                else if(!"default".equals(addr[0])) {
                    try {
                        IP_ADDR = addr[0];
                        PORT = Integer.parseInt(addr[1]);
                    } catch (Exception e) {
                        System.out.println("Invalid input.");
                    }
                }
            }
            for (int i = 1; i <= MAX_TRIAL; i++)
                try {
                    socket = new Socket(IP_ADDR, PORT);
                    // Read data from server.
                    input = new DataInputStream(socket.getInputStream());
                    // Send data to server.
                    output = new DataOutputStream(socket.getOutputStream());
                    return 0;
                } catch (Exception e) {
                    if (i == MAX_TRIAL)
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
        } catch (Exception e) {
//            e.printStackTrace();
            die(1);
        }
    }

    @Override
    public String recvMsg() {
        try {
            return input.readUTF();
        } catch (Exception e) {
//            e.printStackTrace();
            die(1);
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