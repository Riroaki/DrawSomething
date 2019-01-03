package client.UI;

import client.Interact;

import javax.swing.*;

public class WaitingRoom extends UI {
    private JPanel panel;
    private JLabel people;
    private JLabel whoAmI;
    private JButton startButton;
    private JLabel noteLabel;

    private boolean isHost;
    private String myName;
    private int myIndex, currentPlayers;

    public WaitingRoom(Interact client, String name, int index, int current) {
        width = 330;
        height = 70;
        interact = client;
        myName = name;
        myIndex = index;
        isHost = index == 0;
        currentPlayers = current;
    }

    @Override
    void setUIComponents() {
        whoAmI.setText("我是:" + myName + "，第" + (myIndex + 1) + "位玩家");
        startButton.setText(isHost ? "等待更多人加入游戏" : "等待房主开始游戏");
        startButton.setEnabled(isHost && currentPlayers >= 2);
        startButton.addActionListener(e -> interact.sendMsg("start"));
        updateMsg(0);
    }

    @Override
    public JPanel getPanel() {
        return panel;
    }

    @Override
    void listenToServer() {
        while (true) {
            String[] msg = interact.recvMsg().split(",");
            if ("start".equals(msg[0]))
                break;
            if ("stop".equals(msg[0])) {
                disappear();
                interact.die(1);
            } else if ("add".equals(msg[0]))
                updateMsg(1);
            else if ("sub".equals(msg[0]))
                updateMsg(-1);
        }
    }

    @Override
    void nextStage() {
        disappear();
        System.out.println("Entering play room...");
        PlayRoom playroom = new PlayRoom(interact, myName);
        playroom.showAndReact();
    }

    private void updateMsg(int diff) {
        currentPlayers += diff;
        people.setText("房间人数:" + currentPlayers);
        if (isHost && currentPlayers >= 2) {
            startButton.setEnabled(true);
            startButton.setText("开始游戏");
        }
    }
}