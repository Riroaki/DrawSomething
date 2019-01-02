package client.UI;

import client.Interact;

import javax.swing.*;
import java.util.ArrayList;
import java.util.List;

public class LeaderBoard extends UI {
    private JLabel headLabel;
    private JLabel scoreLabel;
    private JButton quitButton;
    private JButton playAgainButton;
    private JPanel panel;

    private String myName;
    private List<String> names;
    private List<Integer> scores;

    public LeaderBoard(String name, Interact client) {
        myName = name;
        width = 200;
        height = 300;
        interact = client;
        names = new ArrayList<>();
        scores = new ArrayList<>();
    }

    @Override
    void setUIComponents() {
        // Restart game.
        playAgainButton.addActionListener(e -> {
            disappear();
            interact.sendMsg("restart");
        });

        // Quit the game.
        quitButton.addActionListener(e -> {
            disappear();
            interact.die(0);
        });

        // Show the scores in the label.
        getScores();
        renderScores();
    }

    @Override
    void listenToServer() {
        while (true)
            if ("restart".equals(interact.recvMsg()))
                break;
    }

    private void getScores() {
        String[] res = interact.recvMsg().split(";");
        for (String s : res) {
            String[] pair = s.split(":");
            names.add(pair[0]);
            scores.add(Integer.parseInt(pair[1]));
        }
    }

    private void renderScores() {
        StringBuilder sb = new StringBuilder("<html>");
        for (int i = 0; i < scores.size(); i++) {
            StringBuilder tmp = new StringBuilder();
            tmp.append(names.get(i));
            tmp.append(":\t");
            tmp.append(scores.get(i));
            if (myName.equals(names.get(i))) {
                tmp.insert(0, "<font color='blue'>");
                tmp.append("</font>");
            }
            tmp.append("<br/><br/>");
            sb.append(tmp);
        }
        sb.append("</html>");
        scoreLabel.setText(sb.toString());
    }

    @Override
    JPanel getPanel() {
        return panel;
    }

    @Override
    void nextStage() {
        disappear();
        System.out.println("Playing again...entering play room...");
        PlayRoom playRoom = new PlayRoom(interact, myName);
        playRoom.showAndReact();
    }
}
