package client.UI;

import client.Client;
import client.Interact;

import javax.swing.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;

public class Login extends UI {
    private JPanel panel;
    private JTextField userText;
    private JButton loginButton;
    private JLabel infoLabel;

    @Override
    JPanel getPanel() {
        return panel;
    }

    public Login(Interact client) {
        interact = client;
        width = 340;
        height = 120;

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
                    infoLabel.setText("<html><font color='red'>这名字太长了</font></html>");
                } else if (input.contains("-")
                        || input.contains(";")
                        || input.contains("`")
                        || input.contains("'")
                        || input.contains("\"")
                        || input.contains(" ")
                        || "quit".equals(input)) {
                    loginButton.setEnabled(false);
                    infoLabel.setText("<html><font color='red'>这名字我不喜欢</font></html>");
                } else {
                    loginButton.setEnabled(true);
                    infoLabel.setText("");
                }
            }
        });

        // Go to waiting room when login button is clicked.
        loginButton.addActionListener(e -> nextStage());

        appear();
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
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Something wrong with the msg from server:");
            System.out.println(msg);
        }
        disappear();
    }

    public static void main(String[] args) {
        Client c = new Client();
        Login login = new Login(c);
    }
}