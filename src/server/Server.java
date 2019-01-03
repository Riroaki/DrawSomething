package server;

import server.topic.Topic;
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
    private final List<ServerThread> threadList = new ArrayList<>();

    public static void main(String[] args) {
        Server server = new Server();
        if (server.init() != 0)
            System.exit(1);
        server.run();
    }

    // Initialize the server.
    private int init() {
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
                ServerThread st = new ServerThread(client, getConnect());
                st.start();
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

    private synchronized void sendAll(String msg, int status) {
        for (ServerThread thread : threadList) {
            if (thread.getPlayerState() == status)
                thread.sendMsg(msg);
        }
    }

    private final List<Integer> orderList = new ArrayList<>();
    private final List<Integer> scoreList = new ArrayList<>();

    private int getGameIndex(int index) {
        synchronized (orderList) {
            if (orderList.contains(index))
                return orderList.indexOf(index);
            orderList.add(index);
            scoreList.add(0);
            return orderList.size();
        }
    }

    private void removeFromGame(int gameIndex) {
        synchronized (orderList) {
            orderList.remove(gameIndex);
            scoreList.remove(gameIndex);
        }
    }

    private StringBuilder result;

    private String getResult() {
        synchronized (orderList) {
            if ("Undefined".equals(result.toString())) {
                result.delete(0, 8);
                for (int i = 0; i < orderList.size(); i++) {
                    int max = i;
                    for (int j = i + 1; j < orderList.size(); j++) {
                        if (scoreList.get(j) > scoreList.get(max))
                            max = j;
                    }
                    int tmp = scoreList.get(i);
                    scoreList.set(i, scoreList.get(max));
                    scoreList.set(max, tmp);
                    tmp = orderList.get(i);
                    orderList.set(i, orderList.get(max));
                    orderList.set(max, tmp);

                    // Set the result string.
                    int myIndex = orderList.get(i);
                    result.append(threadList.get(myIndex).myName);
                    result.append(":");
                    result.append(scoreList.get(i));
                    result.append(",");
                }
                result.deleteCharAt(result.length() - 1);
            }
            return result.toString();
        }
    }

    private Topic topic;

    private void updateTopic() {
        topic = topicGenerator.GiveATopic();
    }

    // The server thread for clients.
    public class ServerThread extends Thread {
        private Socket socket;
        private int playerState;
        private DataInputStream input;
        private DataOutputStream output;
        private String myName;
        private int myIndex;

        // Initialize the server thread.
        ServerThread(Socket client, int index) {
            socket = client;
            myIndex = index;

            // Entering room.
            setPlayerState(0);
            try {
                // Read data from client.
                input = new DataInputStream(socket.getInputStream());
                output = new DataOutputStream(socket.getOutputStream());
            } catch (IOException e) {
                e.printStackTrace();
                changeConnect(-1);
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

        void sendMsg(String msg) {
            try {
                output.writeUTF(msg);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        String recvMsg() {
            String res = "";
            try {
                res = input.readUTF();
            } catch (Exception e) {
                e.printStackTrace();
                die();
            }
            return res;
        }

        // Run the thread.
        public void run() {
            // If the player state is already -1 before starting, then IO is broken.
            if (getPlayerState() == 0)
                beforePlaying();
            if (getPlayerState() == 2)
                play();
            if (getPlayerState() == 3)
                showResults();
            setPlayerState(-1);
            die();
        }

        private int gameIndex;

        // Interactions before entering play room.
        private void beforePlaying() {
            while (true) {
                String raw = recvMsg();
                String[] msg = raw.split(",");
                if ("quit".equals(msg[0])) {
                    setPlayerState(-1);
                    break;
                }
                if ("name".equals(msg[0])) {
                    myName = msg[1];
                    sendMsg("enter," + myIndex + "," + getConnect());
                    sendAll("add", 1);
                    // This should be done after the message is sent,
                    // to avoid new player's current players being added twice.
                    setPlayerState(1);
                } else if ("start".equals(msg[0])) {
                    sendAll("start", 1);
                    updateTopic();
                } else if ("ok".equals(msg[0])) {
                    setPlayerState(2);
                    gameIndex = getGameIndex(myIndex);
                    break;
                }
            }
        }

        private boolean IAmRight;

        private boolean unsolved;

        // Interactions while playing.
        private void play() {
            IAmRight = false;
            unsolved = true;
            result = new StringBuilder("Undefined");
            // The game includes 5 rounds.
            for (int i = 0; i < 5; i++) {
                if (gameIndex == 0)
                    sendMsg("draw," + topic.getName());
                else
                    sendMsg("guess," + topic.getType() + "," + topic.getLength());
                while (true) {
                    String raw = recvMsg();
                    String[] msg = raw.split(",");
                    if ("quit".equals(msg[0])) {
                        setPlayerState(-1);
                        sendAll("quit," + myName, 2);
                        removeFromGame(gameIndex);
                        break;
                    } else if ("guess".equals(msg[0])) {
                        if (!IAmRight && topic.getName().equals(msg[1])) {
                            sendAll("right," + myName, 2);
                            if (unsolved) {
                                sendAll("time", 2);
                                scoreList.set(gameIndex, scoreList.get(gameIndex) + 3);
                            } else
                                scoreList.set(gameIndex, scoreList.get(gameIndex) + 1);
                            // The person who draws will gain 1 point too.
                            scoreList.set(0, scoreList.get(0) + 1);
                            // TODO: stop if everyone has figured out the puzzle.
                        } else
                            sendAll("wrong," + myName + "," + msg[1], 2);
                    } else if ("paint".equals(msg[0]) || "clear".equals(msg[0])) {
                        sendAll(raw, 2);
                    } else if ("end".equals(msg[0])) {
                        sendMsg("stop");
                        setPlayerState(3);
                        break;
                    }
                }
                if (getPlayerState() == -1)
                    break;
                gameIndex = getGameIndex(myIndex);
            }
        }

        // Show the results.
        private void showResults() {
            sendMsg(getResult());
            while (true)
                if ("restart".equals(recvMsg()))
                    break;
            die();
        }

        // Close the connection if the client quits.
        private void die() {
            // Update all players in the waiting room and playing room.
            sendAll("sub", 1);
            sendAll("quit," + myName, 2);
            try {
                input.close();
                output.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            System.out.println("Client dies.");
            System.exit(0);
        }
    }
}