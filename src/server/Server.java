package server;

import server.topic.TopicGenerator;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class Server {
    private TopicGenerator topicGenerator;
    private final int PORT = 12409;
    private final int MAX_CONNECT = 8;
    private static int connect = 0;
    private ServerSocket serverSocket;
    private List<ServerThread> threadList;

    public static void main(String[] args) {
        Server server = new Server();
        if (server.init() != 0)
            System.exit(1);
        server.run();
    }

    // Initialize the server.
    private int init() {
        threadList = new ArrayList<>();
        try {
            topicGenerator = new TopicGenerator();
            serverSocket = new ServerSocket(PORT);
        } catch (Exception e) {
            System.out.println("Fail to initialize the server due to some exception(s).");
            return 1;
        }
        return 0;
    }

    // Run the server.
    private void run() {
        try {
            // Waiting for players to enter the room.
            while (getConnect() < MAX_CONNECT) {
                Socket client = serverSocket.accept();
                ServerThread st = new ServerThread(client, connect);
                threadList.add(st);
                changeConnect(1);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void changeConnect(int diff) {
        synchronized ((Integer) connect) {
            connect += diff;
        }
    }

    private int getConnect() {
        synchronized ((Integer) connect) {
            return connect;
        }
    }

    // The server thread for clients.
    public class ServerThread extends Thread {
        private Socket socket;
        private int playerState;
        private DataInputStream input;
        private DataOutputStream output;
        private String name;
        private int index;

        // Initialize the server thread.
        ServerThread(Socket client, int ind) {
            socket = client;
            index = ind;

            // Entering room.
            playerState = 0;
            new Thread(this).start();
            try {
                // Read data from client.
                input = new DataInputStream(socket.getInputStream());
                output = new DataOutputStream(socket.getOutputStream());
            } catch (IOException e) {
                e.printStackTrace();
                changeConnect(-1);
                die(1);
            }
        }

        int getPlayerState() {
            synchronized ((Integer) playerState) {
                return playerState;
            }
        }

        void setPlayerState(int state) {
            synchronized ((Integer) playerState) {
                playerState = state;
            }
        }

        // Run the thread.
        public void run() {
            try {
                // Read name from client, and tell him his index && current players.
                name = input.readUTF();
                output.writeUTF(index + "," + getConnect());

                // When client is ready, tell him the current players.
                String isOkay = input.readUTF();
                if ("ok".equals(isOkay)) {
                    setPlayerState(1);
                    // Update the current players if needed.
                    if (index == 0) {
                        try {
//                            output.writeUTF("Please start the game.");
                            String msg = input.readUTF();
                            if ("start".equals(msg)) {
                                // Send start message to all.
                            }
                        } catch (IOException e) {
                            e.printStackTrace();
                            die(1);
                        }
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
                System.out.println("Fail to read from client.");
            }

            // The playing session.

            die(0);
        }

        // Close the connection if the client quits.
        private void die(int status) {
            setPlayerState(-1);
            try {
                input.close();
                output.close();
            } catch (IOException e) {
                e.printStackTrace();
                System.out.println("Fail to close socket.");
            }
            System.out.println("Client dies.");
        }
    }
}