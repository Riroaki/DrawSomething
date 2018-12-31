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
    private int connect;
    private ServerSocket serverSocket;
    private List<ServerThread> threadList;
    private ThreadsController controller;

    public static void main(String[] args) {
        Server server = new Server();
        if (server.init() != 0)
            System.exit(1);
        server.run();
    }

    // Initialize the server.
    private int init() {
        connect = 0;
        threadList = new ArrayList<>();
        controller = new ThreadsController();
        try {
            topicGenerator = new TopicGenerator();
            serverSocket = new ServerSocket(PORT);
        } catch (Exception e) {
            System.out.println("Fail to initialize the server due to some exception(s).");
            return 1;
        }
        new Thread(controller).start();
        return 0;
    }

    // Run the server.
    private void run() {
        try {
            // Waiting for players to enter the room.
            while (connect < MAX_CONNECT) {
                Socket client = serverSocket.accept();
                ServerThread st = new ServerThread(client, connect);
                threadList.add(st);
                changeConnect(1);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private synchronized void changeConnect(int diff) {
        connect += diff;
    }

    private synchronized int getConnect() {
        return connect;
    }

    private class ThreadsController extends Thread {
        @Override
        public void run() {
            int currentConnect = getConnect();
            // TODO: needs improvement for checking connections.
            while (true) {
                try {
                    if (currentConnect != connect) {
                        currentConnect = connect;
                        sendAll(Integer.toString(currentConnect));
                        Thread.sleep(200);
                    }
                }catch (InterruptedException e) {
                    e.printStackTrace();
                    die(1);
                }
            }
        }

        void die(int status) {
            System.exit(status);
        }

        void sendAll(String msg) {
            for (ServerThread thread : threadList) {
                try {
                    if (thread.getPlayerState() == 1) {
                        thread.output.writeUTF(msg);
                        thread.setPlayerState(2);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
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

        synchronized int getPlayerState() {
            return playerState;
        }

        synchronized void setPlayerState(int state) {
            playerState = state;
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
                                controller.sendAll("start");
                                controller.die(0);
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