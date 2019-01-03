package server;

import server.topic.Topic;
import server.topic.TopicGenerator;

import java.io.DataInputStream;
import java.io.DataOutputStream;
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
            topic = topicGenerator.GiveATopic();
            serverSocket = new ServerSocket(PORT);
        } catch (Exception e) {
            e.printStackTrace();
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
                ServerThread st = new ServerThread(client);
                st.start();
                threadList.add(st);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Update the number of connects.
    private void changeConnect(int diff) {
        synchronized ((Integer) connect) {
            connect += diff;
        }
    }

    // Get the number of connects.
    private int getConnect() {
        synchronized ((Integer) connect) {
            return connect;
        }
    }

    // Send the message to all players.
    private synchronized void sendAll(String msg, int status) {
        for (ServerThread thread : threadList) {
            if (thread.getPlayerState() == status)
                thread.sendMsg(msg);
        }
    }

    private Topic topic;

    private void updateTopic() {
        topic = topicGenerator.GiveATopic();
    }

    // The game index and score of every player.
    private final List<Integer> gameIndexList = new ArrayList<>();
    private final List<Integer> scoreList = new ArrayList<>();
    private final List<Boolean> hasFiguredOut = new ArrayList<>();

    // Get the game index for a server thread.
    private int getGameIndex(int index) {
        int res = gameIndexList.indexOf(index);
        if (res != -1)
            return res;
        gameIndexList.add(index);
        scoreList.add(0);
        hasFiguredOut.add(false);
        return gameIndexList.size() - 1;
    }

    // Remove the server thread from game.
    private void removeFromGame(int gameIndex) {
        gameIndexList.remove(gameIndex);
        scoreList.remove(gameIndex);
        hasFiguredOut.remove(gameIndex);
    }

    // The round count of server.
    private int roundOfServer = 0;

    // The name list string.
    private final StringBuilder names = new StringBuilder("name");

    private void appendName(String name) {
        synchronized (names) {
            names.append(',');
            names.append(name);
        }
    }

    private String getNames() {
        synchronized (names) {
            return names.toString();
        }
    }

    // This variable will be set to true each round.
    private boolean unsolved = true;

    // The result string.
    private final StringBuffer result = new StringBuffer("Undefined");

    private String getResult() {
        synchronized (result) {
            if ("Undefined".equals(result.toString())) {
                result.delete(0, 9);
                for (int i = 0; i < gameIndexList.size(); i++) {
                    int max = i;
                    for (int j = i + 1; j < gameIndexList.size(); j++) {
                        if (scoreList.get(j) > scoreList.get(max))
                            max = j;
                    }
                    int tmp = scoreList.get(i);
                    scoreList.set(i, scoreList.get(max));
                    scoreList.set(max, tmp);
                    tmp = gameIndexList.get(i);
                    gameIndexList.set(i, gameIndexList.get(max));
                    gameIndexList.set(max, tmp);

                    // Set the result string.
                    int myIndex = gameIndexList.get(i);
                    result.append(threadList.get(myIndex).myName);
                    result.append(':');
                    result.append(scoreList.get(i));
                    result.append(',');
                }
                result.deleteCharAt(result.length() - 1);
            }
            return result.toString();
        }
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
        ServerThread(Socket client) {
            socket = client;

            // Entering room.
            setPlayerState(0);
            try {
                // Read data from client.
                input = new DataInputStream(socket.getInputStream());
                output = new DataOutputStream(socket.getOutputStream());
            } catch (Exception e) {
                e.printStackTrace();
                die();
            }
        }

        // Run the thread.
        public void run() {
            roundOfServer = 0;
            // If the player state is already -1 before starting, then IO is broken.
            beforePlaying();
            // Play session.
            play();
            // Result session.
            showResults();
            // Exit session.
            setPlayerState(-1);
            die();
        }

        private int gameIndex;

        // Interactions before entering play room.
        private void beforePlaying() {
            // Exit if status equals -1.
            if (getPlayerState() != 0)
                return;
            myIndex = threadList.indexOf(this);

            while (true) {
                String raw = recvMsg();
                String[] msg = raw.split(",");
                if ("quit".equals(msg[0])) {
                    setPlayerState(-1);
                    break;
                } else if ("name".equals(msg[0])) {
                    myName = msg[1];
                    changeConnect(1);
                    sendMsg("enter," + myIndex + "," + getConnect());
                    sendAll("add", 1);
                    // This should be done after the message is sent,
                    // to avoid new player's current players being added twice.
                    setPlayerState(1);
                } else if ("start".equals(msg[0])) {
                    sendAll("start", 1);
                } else if ("ok".equals(msg[0])) {
                    setPlayerState(2);
                    // Get my game index for the first round.
                    gameIndex = getGameIndex(myIndex);
                    appendName(myName);
                    break;
                }
            }
        }

        private boolean IAmRight;

        // Interactions while playing.
        private void play() {
            // Exit if status equals -1.
            if (getPlayerState() != 2)
                return;
            // Sleep for a while and wait till everyone is added to the names.
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
                die();
                return;
            }
            // Send the name list to the player.
            sendMsg(getNames());
            try {
                hasFiguredOut.set(0, true);
            } catch (Exception e) {
                die();
                return;
            }

            // The game includes 5 rounds.
            for (int round = 1; round <= 5; round++) {
                // Reset the result of each round.
                IAmRight = false;
                String drawerName = threadList.get(gameIndexList.get(0)).myName;

                if (gameIndex == 0)
                    sendMsg("draw," + topic.getName());
                else
                    sendMsg("guess," + topic.getType() + "," + topic.getLength() + "," + drawerName);

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
                            sendAll("right," + myName + "," + drawerName, 2);
                            IAmRight = true;
                            // Add 3 points to the player if he is the first to solve the problem.
                            synchronized ((Boolean) unsolved) {
                                if (unsolved) {
                                    sendAll("time", 2);
                                    scoreList.set(gameIndex, scoreList.get(gameIndex) + 3);
                                    unsolved = false;
                                } else
                                    // Add 1 points if he is not the first clever man.
                                    scoreList.set(gameIndex, scoreList.get(gameIndex) + 1);
                                hasFiguredOut.set(gameIndex, true);
                                if (hasFiguredOut.indexOf(false) == -1)
                                    sendAll("finish", 2);
                            }
                            // The person who draws will gain 1 point too.
                            scoreList.set(0, scoreList.get(0) + 1);
                        } else
                            sendAll("wrong," + myName + "," + msg[1], 2);

                    } else if ("paint".equals(msg[0]) || "clear".equals(msg[0]) || "cancel".equals(msg[0])) {
                        sendAll(raw, 2);

                    } else if ("end".equals(msg[0])) {
                        // Prepare for a new round.
                        synchronized ((Integer) roundOfServer) {
                            if (roundOfServer != round) {
                                roundOfServer++;
                                unsolved = true;
                                // Shift the list.
                                int tmp = gameIndexList.remove(0);
                                gameIndexList.add(tmp);
                                // Get the drawer name of new round.
                                updateTopic();
                                // Set everyone to be 'unknown' of the puzzle.
                                for (int i = 1; i < hasFiguredOut.size(); i++)
                                    hasFiguredOut.set(i, false);
                                // The one who draws is always aware of the puzzle, so set it to be true.
                                hasFiguredOut.set(0, true);
                            }
                            gameIndex = getGameIndex(myIndex);
                        }
                        break;// Go to next round.
                    }
                }
                if (getPlayerState() != 2)
                    break;
            }
            if (getPlayerState() == 2) {
                sendMsg("stop");
                setPlayerState(3);
            }
        }

        // Show the results.
        private void showResults() {
            // Exit if state equals -1.
            if (getPlayerState() != 3)
                return;
            System.out.println(getResult());
            sendMsg(getResult());
            // TODO: @function restart.
            while (true)
                if ("quit".equals(recvMsg()))
                    break;
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
            } catch (Exception e) {
//                e.printStackTrace();
                die();
            }
        }

        String recvMsg() {
            String res = "";
            try {
                res = input.readUTF();
            } catch (Exception e) {
//                e.printStackTrace();
                die();
            }
            return res;
        }

        // Close the connection if the client quits.
        private void die() {
            threadList.remove(this);
            // Update all players in the waiting room and playing room.
            sendAll("sub", 1);
            sendAll("quit," + myName, 2);
            changeConnect(-1);
            try {
                input.close();
                output.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
            System.out.println("Client dies.");
        }
    }
}