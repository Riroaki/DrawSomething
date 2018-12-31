package client.UI;

import client.Client;
import client.Interact;

import javax.swing.*;

public class WaitingRoom extends UI {
    private JPanel panel;
    private JLabel people;
    private JLabel whoAmI;
    private JButton startButton;
    private JLabel noteLabel;

    private boolean isHost = false;
    private String myName;

    public JPanel getPanel() {
        return panel;
    }

    WaitingRoom(Interact client, String name, int index, int current) {
        width = 330;
        height = 70;
        interact = client;
        myName = name;
        if (index == 0)
            isHost = true;
        whoAmI.setText("我是:" + name + "，第" + index + "位玩家");
        updateMsg(current);
        if (isHost && current >= 2)
            startButton.setEnabled(true);
        else
            startButton.setEnabled(false);
        startButton.addActionListener(e -> client.sendMsg("start"));

        appear();
        listenForStart();
    }

    private void listenForStart() {
        String msg;
        while (true) {
            msg = interact.recvMsg();
            if ("start".equals(msg))
                break;
            if ("quit".equals(msg)) {
                disappear();
                interact.die(1);
            }
            try {
                updateMsg(Integer.parseInt(msg));
            } catch (Exception e) {
                e.printStackTrace();
                System.out.println("Something wrong with msg from server:");
                System.out.println(msg);
            }
        }
        nextStage();
    }

    private synchronized void updateMsg(int current) {
        people.setText("房间人数:" + current);
        if (isHost && current >= 2)
            startButton.setEnabled(true);
        else
            startButton.setEnabled(false);
    }

    void nextStage() {
        disappear();
        System.out.println("Entering playground");
        PlayRoom playroom = new PlayRoom(interact, myName);
    }

    public static void main(String[] args) {
        Client c = new Client();
        WaitingRoom room = new WaitingRoom(c, "test", 0, 4);
    }
}
