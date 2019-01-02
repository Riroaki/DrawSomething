package client.UI;

import client.Interact;

import javax.swing.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.regex.Pattern;

public class Login extends UI {
    private JPanel panel;
    private JTextField userText;
    private JButton loginButton;
    private JLabel infoLabel;

    public Login(Interact client) {
        interact = client;
        width = 340;
        height = 120;
    }

    @Override
    void setUIComponents() {
        // Listen while the user types his name.
        userText.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) {
                String input = userText.getText();
                if (input.length() == 0) {
                    loginButton.setEnabled(false);
                    infoLabel.setText("");
                } else if (input.length() >= 10) {
                    loginButton.setEnabled(false);
                    infoLabel.setText("<html><font color='red'>这名字太长了，你觉得呢</font></html>");
                } else if (Pattern.matches(".*[-,;`'\">< ]+.*", input)) {
                    loginButton.setEnabled(false);
                    infoLabel.setText("<html><font color='red'>这名字不好吧，你觉得呢</font></html>");
                } else {
                    loginButton.setEnabled(true);
                    infoLabel.setText("");
                }
            }
        });

        // Go to waiting room when login button is clicked.
        loginButton.addActionListener(e -> nextStage());
    }

    @Override
    JPanel getPanel() {
        return panel;
    }

    @Override
    void listenToServer() {
        while (true)
            if ("stop".equals(interact.recvMsg()))
                break;
    }

    void nextStage() {
        String name = userText.getText(), msg = "";
        interact.sendMsg(name);
        try {
            msg = interact.recvMsg();
            String[] indexStrings = msg.split(",");
            System.out.println("Entering waiting room");
            WaitingRoom room = new WaitingRoom(interact, name,
                    Integer.parseInt(indexStrings[0]),
                    Integer.parseInt(indexStrings[1]));
            room.showAndReact();
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Something wrong with the msg from server:");
            System.out.println(msg);
        }
        disappear();
    }
}