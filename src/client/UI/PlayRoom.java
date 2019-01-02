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

interface MsgParser {
    void parser(String[] msg);
}

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

public class PlayRoom extends UI {
    private JPanel panel;
    private JTextArea msgText;
    private JTextArea scoreText;
    private JTextField commentText;
    private JButton sendButton;
    private JComboBox<String> colorSelector;
    private JComboBox<String> strokeSelector;
    private JButton clearButton;
    private JLabel timeLabel;
    private JLabel hintLabel;
    private JLabel roundLabel;

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
        colorSelector.setBounds(0, 700, 300, 30);
        colorSelector.setEditable(false);
        panel.add(colorSelector);

        strokeSelector = new JComboBox<>();
        strokeSelector.setBounds(300, 700, 300, 30);
        strokeSelector.setEditable(false);
        panel.add(strokeSelector);

        String[] colors = {"black", "white", "red", "yellow", "blue", "green", "pink"};
        String[] strokes = {"light", "medium", "heavy"};
        for (String s : colors)
            colorSelector.addItem(s);
        for (String s : strokes)
            strokeSelector.addItem(s);
        colorSelector.addActionListener(e -> paintBoard.setColor(colorSelector.getSelectedIndex()));
        strokeSelector.addActionListener(e -> paintBoard.setStroke(strokeSelector.getSelectedIndex()));

        clearButton = new JButton();
        clearButton.setBounds(600, 700, 200, 30);
        clearButton.setText("清空画布");
        panel.add(clearButton);

        clearButton.addActionListener(e -> {
            paintBoard.clear();
            interact.sendMsg("clear");
        });

        timeLabel = new JLabel("91s");
        timeLabel.setBounds(810, 0, 50, 30);
        panel.add(timeLabel);

        hintLabel = new JLabel();
        hintLabel.setBounds(860, 0, 150, 30);
        panel.add(hintLabel);

        msgText = new JTextArea();
        msgText.setBounds(810, 30, 200, 320);
        msgText.setEditable(false);
        panel.add(msgText);

        roundLabel = new JLabel("第0轮");
        roundLabel.setBounds(810, 350, 200, 30);
        panel.add(roundLabel);

        scoreText = new JTextArea();
        scoreText.setEditable(false);
        scoreText.setBounds(810, 380, 200, 320);
        panel.add(scoreText);

        commentText = new JTextField();
        commentText.setBounds(805, 700, 160, 30);
        commentText.setEditable(false);
        panel.add(commentText);

        commentText.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) {
                sendButton.setEnabled(commentText.getText().length() > 0);
            }
        });

        sendButton = new JButton();
        sendButton.setBounds(960, 700, 60, 30);
        sendButton.setText("发送");
        panel.add(sendButton);

        sendButton.setEnabled(false);
        sendButton.addActionListener(e -> {
            // If the content includes ',', then the message after the ',' will be ignored when parsing it.
            interact.sendMsg("guess," + commentText.getText().replaceAll(",", "，"));
            commentText.setText("");
        });

        setTimer();
    }

    @Override
    JPanel getPanel() {
        return panel;
    }

    @Override
    void listenToServer() {
        // Put the parser function of each command into an hash map.
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

        // Receive messages and parse them.
        while (true) {
            String raw = interact.recvMsg();
            try {
                String[] msg = raw.split(",");
                if ("stop".equals(msg[0]))
                    break;
                if (parserDict.containsKey(msg[0]))
                    parserDict.get(msg[0]).parser(msg);
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
        });
    }

    // Start a new round of game.
    private void startNewRound() {
        // Set the game round.
        round++;
        roundLabel.setText("第" + round + "轮");

        // Clear the board and the message box.
        paintBoard.clear();
        updateMsg("");// Clear all messages.
        colorSelector.setSelectedIndex(0);
        strokeSelector.setSelectedIndex(1);

        // Initialize the content of score text.
        updateScore(0, 0);

        timeLeft = 90;
        timeLabel.setText("90s");
        timer.restart();
    }

    // Repaint the message.
    // Each message in msgList ends with '\n'.
    private void updateMsg(String entry) {
        // Clear the messages.
        if ("".equals(entry)) {
            if (msgList.isEmpty()) {
                msgList.add("--游戏即将开始--\n\n");
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
                    if (mouseDraw)
                        addPoint(e.getX(), e.getY(), false);
                }

                @Override
                public void mouseDragged(MouseEvent e) {
                    if (mouseDraw) {
                        addPoint(e.getX(), e.getY(), true);
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
            String isContinuous = continuous ? "1" : "0";
            interact.sendMsg("paint," + x + "," + y + "," + colorIndex + "," + strokeType + "," + isContinuous);
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
        public void parser(String[] msg) {
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
        public void parser(String[] msg) {
            sendButton.setEnabled(false);
            hintLabel.setText("我画" + msg[2]);
            commentText.setEnabled(false);
            commentText.setText("轮到你画，不能发消息");
            paintBoard.setMouseDraw(true);
            shouldDraw = true;
            startNewRound();
        }
    }

    // Class for parsing command "guess".
    class GuessParser implements MsgParser {
        @Override
        public void parser(String[] msg) {
            hintLabel.setText("我猜" + msg[2] + ";" + msg[3] + "个字");
            commentText.setEnabled(true);
            commentText.setText("在这里输入你的猜测");
            paintBoard.setMouseDraw(false);
            shouldDraw = false;
            startNewRound();
        }
    }

    // Class for parsing command "quit".
    class QuitParser implements MsgParser {
        @Override
        public void parser(String[] msg) {
            updateMsg(msg[1] + "退出了房间");
            aliveList.set(nameList.indexOf(msg[1]), false);
            updateScore(0, 0);
        }
    }

    class RightParser implements MsgParser {
        @Override
        public void parser(String[] msg) {
            int plusScore = 3;
            if (timeLeft < 30)
                plusScore = 1;
            updateMsg(msg[1] + "猜对了答案，加" + plusScore + "分");
            updateScore(nameList.indexOf(msg[1]), plusScore);
        }
    }

    class WrongParser implements MsgParser {

        @Override
        public void parser(String[] msg) {
            updateMsg(msg[1] + ":" + msg[2]);
        }
    }

    class PaintParser implements MsgParser {

        @Override
        public void parser(String[] msg) {
            if (shouldDraw)
                return;
            // Format of message: x, y, color, stroke.
            // MUST assign color and stroke before adding points.
            paintBoard.setColor(Integer.parseInt(msg[4]));
            paintBoard.setStroke(Integer.parseInt(msg[5]));
            boolean continuous = Integer.parseInt(msg[6]) != 0;
            paintBoard.addPoint(Integer.parseInt(msg[2]), Integer.parseInt(msg[3]), continuous);
        }
    }

    class ClearParser implements MsgParser {

        @Override
        public void parser(String[] msg) {
            if (shouldDraw)
                return;
            paintBoard.clear();
            updateMsg("画布被清空了");
        }
    }

    class TimeParser implements MsgParser {

        @Override
        public void parser(String[] msg) {
            if (timeLeft <= 30)
                return;
            timeLeft = 30;
            updateMsg("时间缩短为30秒");
        }
    }
}