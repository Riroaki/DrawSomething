package client.UI;

import client.Interact;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

// An interface for parsing messages
interface MsgParser {
    void parse(String[] msg);
}

// Class to store information of a point.
// Including position (x,  y), color, stroke, and whether connected to last point.
class MyPoint extends Point {
    private Color color;
    private int strokeType;
    private boolean continuous;

    MyPoint(int x, int y, Color color, int stroke, boolean cont) {
        this.x = x;
        this.y = y;
        this.color = color;
        strokeType = stroke;
        continuous = cont;
    }

    Color getColor() {
        return color;
    }

    int getStroke() {
        int[] strokes = {2, 5, 15};
        return strokes[strokeType];
    }

    boolean getContinuous() {
        return continuous;
    }
}

// UI class of play room.
public class PlayRoom extends UI {
    private JPanel panel;
    private JTextArea msgText;
    private JTextArea scoreText;
    private JTextField commentText;
    private JButton sendButton;
    private JComboBox<String> colorSelector;
    private JComboBox<String> strokeSelector;
    private JButton cancelButton;
    private JButton clearButton;
    private JLabel timeLabel;
    private JLabel hintLabel;
    private JLabel roundLabel;

    // The member variables.
    private boolean gameOver, shouldDraw;
    private int round, timeLeft;
    private String myName;
    private Timer timer;
    private List<String> nameList, msgList;
    private List<Integer> scoreList;
    private List<Boolean> aliveList;
    private PaintBoard paintBoard;

    public PlayRoom(Interact client, String name) {
        gameOver = false;
        myName = name;
        interact = client;
        round = 0;

        nameList = new ArrayList<>();
        scoreList = new ArrayList<>();
        msgList = new ArrayList<>();
        aliveList = new ArrayList<>();
    }

    // Set up UI components.
    // Due to some problems I choose to hard-code the sizes of components...
    @Override
    void setUIComponents() {
        panel = new JPanel();
        panel.setLayout(null);
        width = 1020;
        height = 750;
        panel.setBounds(0, 0, width, height);

        paintBoard = new PaintBoard();
        paintBoard.setBounds(0, 0, 800, 700);
        panel.add(paintBoard);

        colorSelector = new JComboBox<>();
        colorSelector.setBounds(0, 700, 200, 30);
        colorSelector.setEditable(false);
        panel.add(colorSelector);

        strokeSelector = new JComboBox<>();
        strokeSelector.setBounds(200, 700, 200, 30);
        strokeSelector.setEditable(false);
        panel.add(strokeSelector);

        String[] colors = {"黑", "白", "红", "黄", "蓝", "绿", "粉"};
        String[] strokes = {"较细", "中等粗细", "较粗"};
        for (String s : colors)
            colorSelector.addItem(s);
        for (String s : strokes)
            strokeSelector.addItem(s);
        colorSelector.addActionListener(e -> paintBoard.setColor(colorSelector.getSelectedIndex()));
        strokeSelector.addActionListener(e -> paintBoard.setStroke(strokeSelector.getSelectedIndex()));

        cancelButton = new JButton("撤销");
        cancelButton.setBounds(400, 700, 200, 30);
        cancelButton.addActionListener(e -> {
            paintBoard.cancel();
            interact.sendMsg("cancel");
        });
        panel.add(cancelButton);

        clearButton = new JButton("清空画布");
        clearButton.setBounds(600, 700, 200, 30);
        clearButton.addActionListener(e -> {
            paintBoard.clear();
            updateMsg("清空了画布");
            interact.sendMsg("clear");
        });
        panel.add(clearButton);

        timeLabel = new JLabel("91s");
        timeLabel.setBounds(810, 0, 50, 30);
        setTimer();
        panel.add(timeLabel);

        hintLabel = new JLabel();
        hintLabel.setBounds(860, 0, 150, 30);
        panel.add(hintLabel);

        msgText = new JTextArea();
        msgText.setBounds(810, 30, 200, 320);
        msgText.setEditable(false);
        panel.add(msgText);

        roundLabel = new JLabel("第0轮，" + myName);
        roundLabel.setBounds(810, 350, 200, 30);
        panel.add(roundLabel);

        scoreText = new JTextArea();
        scoreText.setEditable(false);
        scoreText.setBounds(810, 380, 200, 320);
        panel.add(scoreText);

        commentText = new JTextField();
        commentText.setBounds(805, 700, 160, 30);
        commentText.setEditable(false);
        commentText.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) {
                sendButton.setEnabled(commentText.getText().length() > 0);
            }
        });
        panel.add(commentText);

        sendButton = new JButton("发送");
        sendButton.setBounds(960, 700, 60, 30);
        sendButton.setEnabled(false);
        sendButton.addActionListener(e -> {
            // If the content includes ',', then the message after the ',' will be ignored when parsing it.
            interact.sendMsg("guess," + commentText.getText().replaceAll(",", "，"));
            commentText.setText("");
        });
        panel.add(sendButton);
    }

    @Override
    JPanel getPanel() {
        return panel;
    }

    @Override
    void listenToServer() {
        // Put the parse function of each command into an hash map.
        // This is designed to avoid if-else and switches.
        HashMap<String, MsgParser> parserDict = new HashMap<>();
        parserDict.put("name", new NameParser());
        parserDict.put("draw", new DrawParser());
        parserDict.put("guess", new GuessParser());
        parserDict.put("quit", new QuitParser());
        parserDict.put("right", new RightParser());
        parserDict.put("wrong", new WrongParser());
        parserDict.put("paint", new PaintParser());
        parserDict.put("clear", new ClearParser());
        parserDict.put("time", new TimeParser());
        parserDict.put("cancel", new CancelParser());
        parserDict.put("finish", new FinishParser());

        // Tell the server that I am prepared to start the game.
        interact.sendMsg("ok");

        // Receive messages and parse them.
        while (true) {
            String raw = interact.recvMsg();
            try {
                String[] msg = raw.split(",");
                if ("stop".equals(msg[0]))
                    break;
                if (parserDict.containsKey(msg[0]))
                    parserDict.get(msg[0]).parse(msg);
            } catch (IndexOutOfBoundsException e) {
                e.printStackTrace();
                System.out.println("Something wrong with server's message");
                System.out.println(raw);
            }
        }
    }

    @Override
    void nextStage() {
        disappear();
        System.out.println("Going to leader board.");
        LeaderBoard leaderBoard = new LeaderBoard(myName, interact);
        leaderBoard.showAndReact();
    }

    // Set up the timer task.
    private void setTimer() {
        // Set the timer.
        // If every one has figured out the puzzle,
        // time left will be set to 0 to finish this round.
        timer = new Timer(1000, e -> {
            synchronized ((Integer) timeLeft) {
                timeLeft -= 1;
                if (timeLeft < 0) {
                    updateMsg("本轮游戏结束");
                    interact.sendMsg("end");
                    timer.stop();
                    try {
                        if (!gameOver) {
                            updateMsg("下一轮游戏即将开始...");
                            Thread.sleep(1000);
                        } else {
                            updateMsg("游戏结束");
                            paintBoard.setMouseDraw(false);
                            Thread.sleep(2000);
                            // Go to next stage.
                            timer.stop();
                        }
                    } catch (InterruptedException ie) {
                        ie.printStackTrace();
                    }
                    return;
                }
                String time = timeLeft + "s";
                if (timeLeft <= 10)
                    time = "<html><font color='red'>" + time + "</font></html>";
                timeLabel.setText(time);
            }
        });
    }

    private boolean unsolved;

    // Start a new round of game.
    private void startNewRound() {
        // Set the game round.
        round++;
        roundLabel.setText("第" + round + "/5轮，" + myName);

        // Clear the board and the message box.
        paintBoard.clear();
        updateMsg("");// Clear all messages.
        colorSelector.setSelectedIndex(0);
        strokeSelector.setSelectedIndex(1);

        // Initialize the content of score text.
        updateScore(0, 0);

        synchronized ((Integer) timeLeft) {
            timeLeft = 90;
        }
        timeLabel.setText("90s");
        timer.restart();

        unsolved = true;
    }

    // Repaint the message.
    // Each message in msgList ends with '\n'.
    private void updateMsg(String entry) {
        // Clear the messages.
        if ("".equals(entry)) {
            if (msgList.isEmpty()) {
                msgList.add("--游戏开始--\n\n");
            } else {
                msgList.remove(0);
                msgList.add("--以上为上一轮消息--\n\n");
            }

            // Insert a message.
        } else {
            if (msgList.size() >= 8)
                msgList.remove(0);
            msgList.add(entry + "\n\n");
        }

        // Show the contents of messages.
        StringBuilder msgTextContent = new StringBuilder();
        for (String msg : msgList)
            msgTextContent.append(msg);
        msgText.setText(msgTextContent.toString());
    }

    // Repaint the scores.
    private void updateScore(int index, int plusScore) {
        if (plusScore > 0)
            scoreList.set(index, scoreList.get(index) + plusScore);
        StringBuilder scoreTextContent = new StringBuilder();
        for (int i = 0; i < nameList.size(); i++) {
            StringBuilder temp = new StringBuilder();
            if (!aliveList.get(i))
                temp.append("(已退出)");
            temp.append(nameList.get(i));
            temp.append("\t:");
            temp.append(scoreList.get(i));
            temp.append("\n\n");
            scoreTextContent.append(temp);
        }
        scoreText.setText(scoreTextContent.toString());
    }

    // Class for painting board.
    class PaintBoard extends JPanel {
        private boolean mouseDraw = false;
        private Color color = Color.black;
        private int strokeType = 1, colorIndex = 0;
        private List<MyPoint> pointList;

        PaintBoard() {
            this.setBackground(Color.white);
            pointList = new ArrayList<>();
            MouseAdapter detector = new MouseAdapter() {
                @Override
                public void mousePressed(MouseEvent e) {
                    if (mouseDraw) {
                        addPoint(e.getX(), e.getY(), false);
                        interact.sendMsg("paint," + e.getX() + "," + e.getY() + "," + colorIndex + "," + strokeType + ",0");
                    }
                }

                @Override
                public void mouseDragged(MouseEvent e) {
                    if (mouseDraw) {
                        addPoint(e.getX(), e.getY(), true);
                        interact.sendMsg("paint," + e.getX() + "," + e.getY() + "," + colorIndex + "," + strokeType + ",1");
                    }
                }
            };
            this.addMouseListener(detector);
            this.addMouseMotionListener(detector);
        }

        @Override
        public void paint(Graphics g) {
            super.paint(g);
            Graphics2D g2d = (Graphics2D) g;
            for (int i = 0; i < pointList.size(); i++) {
                MyPoint current = pointList.get(i);
                MyPoint last = i == 0 ? null : pointList.get(i - 1);
                g2d.setColor(current.getColor());
                BasicStroke stroke = new BasicStroke(current.getStroke());
                g2d.setStroke(stroke);

                // Continuous is true means this point and last point is linked.
                if (current.getContinuous() && last != null) {
                    g2d.drawLine(last.x, last.y, current.x, current.y);
                } else {
                    g2d.drawLine(current.x, current.y, current.x, current.y);
                }
            }
        }

        // Clear the board.
        void clear() {
            pointList.clear();
            this.repaint();
        }

        void addPoint(int x, int y, boolean continuous) {
            MyPoint point = new MyPoint(x, y, color, strokeType, continuous);
            pointList.add(point);
            this.repaint();
        }

        // Cancel.
        void cancel() {
            int pointIndex = pointList.size() - 1;
            if (pointIndex == -1)
                return;
            while (true) {
                MyPoint tmp = pointList.remove(pointIndex--);
                if (!tmp.getContinuous())
                    break;
            }
            this.repaint();
        }

        void setColor(int index) {
            colorIndex = index;
            Color[] colors = {Color.black, Color.white, Color.red, Color.yellow, Color.blue, Color.green, Color.pink};
            color = colors[index];
        }

        void setStroke(int newStroke) {
            strokeType = newStroke;
        }

        void setMouseDraw(boolean status) {
            mouseDraw = status;
        }
    }

    // Class for parsing command "name".
    class NameParser implements MsgParser {
        @Override
        public void parse(String[] msg) {
            for (int i = 1; i < msg.length; i++) {
                nameList.add(msg[i]);
                scoreList.add(0);
                aliveList.add(true);
            }
            updateScore(0, 0);
        }
    }

    // Class for parsing command "draw".
    class DrawParser implements MsgParser {
        @Override
        public void parse(String[] msg) {
            // The drawing panel.
            cancelButton.setEnabled(true);
            colorSelector.setEnabled(true);
            strokeSelector.setEnabled(true);
            paintBoard.setMouseDraw(true);
            clearButton.setEnabled(true);

            // The message panel.
            sendButton.setEnabled(false);
            hintLabel.setText("我画：" + msg[1]);
            commentText.setEnabled(false);
            commentText.setText("轮到你画，不能发消息");

            shouldDraw = true;
            startNewRound();
        }
    }

    // Class for parsing command "guess".
    class GuessParser implements MsgParser {
        @Override
        public void parse(String[] msg) {
            // The drawing panel.
            cancelButton.setEnabled(false);
            colorSelector.setEnabled(false);
            strokeSelector.setEnabled(false);
            clearButton.setEnabled(false);
            paintBoard.setMouseDraw(false);

            // The message panel.
            hintLabel.setText("我猜：" + msg[1] + "，" + msg[2] + "个字");
            commentText.setEnabled(true);
            commentText.setText("在这里输入你的猜测");
            commentText.setEditable(true);

            shouldDraw = false;
            startNewRound();
        }
    }

    // Class for parsing command "quit".
    class QuitParser implements MsgParser {
        @Override
        public void parse(String[] msg) {
            updateMsg(msg[1] + "退出了房间");
            aliveList.set(nameList.indexOf(msg[1]), false);
            updateScore(0, 0);
        }
    }

    // Class for parsing command "right".
    class RightParser implements MsgParser {
        @Override
        public void parse(String[] msg) {
            int plusScore = 1;
            if (unsolved) {
                unsolved = false;
                plusScore = 3;
            }
            updateMsg(msg[1] + "猜对了答案，加" + plusScore + "分");
            // Add to the person who guesses.
            updateScore(nameList.indexOf(msg[1]), plusScore);
            // Add to the person who draws.
            updateScore(nameList.indexOf(msg[2]), 1);
        }
    }

    // Other classes to parse different commands...
    class WrongParser implements MsgParser {
        @Override
        public void parse(String[] msg) {
            updateMsg(msg[1] + ":" + msg[2]);
        }
    }

    class PaintParser implements MsgParser {
        @Override
        public void parse(String[] msg) {
            if (shouldDraw)
                return;
            // Format of message: x, y, color, stroke.
            // MUST assign color and stroke before adding points.
            paintBoard.setColor(Integer.parseInt(msg[3]));
            paintBoard.setStroke(Integer.parseInt(msg[4]));
            boolean continuous = Integer.parseInt(msg[5]) != 0;
            paintBoard.addPoint(Integer.parseInt(msg[1]), Integer.parseInt(msg[2]), continuous);
        }
    }

    class ClearParser implements MsgParser {
        @Override
        public void parse(String[] msg) {
            if (shouldDraw)
                return;
            paintBoard.clear();
            updateMsg("画布被清空了");
        }
    }

    class TimeParser implements MsgParser {
        @Override
        public void parse(String[] msg) {
            synchronized ((Integer) timeLeft) {
                if (timeLeft <= 30)
                    return;
                timeLeft = 30;
            }
            updateMsg("时间缩短为30秒");
        }
    }

    class CancelParser implements MsgParser {
        @Override
        public void parse(String[] msg) {
            if (shouldDraw)
                return;
            paintBoard.cancel();
        }
    }

    class FinishParser implements MsgParser {
        @Override
        public void parse(String[] msg) {
            synchronized ((Integer) timeLeft) {
                timeLeft = 1;
            }
        }
    }
}