package client.UI;

import client.Interact;

import javax.swing.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.regex.Pattern;

// The class for login UI.
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

    // Setting up UI components.
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
        loginButton.addActionListener(e -> interact.sendMsg("name," + userText.getText()));
    }

    @Override
    JPanel getPanel() {
        return panel;
    }

    @Override
    void listenToServer() {
        while (true) {
            String raw = interact.recvMsg();
            String[] msg = raw.split(",");
            if ("stop".equals(msg[0])) {
                disappear();
                interact.die(1);
                break;
            } else if ("enter".equals(msg[0])) {
                myIndex = Integer.parseInt(msg[1]);
                currentPlayers = Integer.parseInt(msg[2]);
                break;
            }
        }
    }

    private int myIndex, currentPlayers;

    void nextStage() {
        disappear();// Should vanish before entering a new room, as the showAndReact function won't end.
        System.out.println("Entering waiting room");
        WaitingRoom room = new WaitingRoom(interact, userText.getText(),
                myIndex, currentPlayers);
        room.showAndReact();
    }
}