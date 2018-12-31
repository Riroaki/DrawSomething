package client.UI;

import client.Interact;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

abstract class UI {
    int width, height;
    private JFrame ui;
    Interact interact;

    abstract JPanel getPanel();

    abstract void nextStage();

    void appear() {
        ui = new JFrame("你画我猜");
        // Quit game.
        ui.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        ui.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                int i = JOptionPane.showConfirmDialog(null, "确定要退出游戏吗？", "退出游戏", JOptionPane.YES_NO_OPTION);
                if (i == JOptionPane.YES_OPTION) {
                    ui.dispose();
                    interact.sendMsg("quit");
                    interact.die(1);
                }
            }
        });
        // Set the ui at center of the screen.
        Dimension d = Toolkit.getDefaultToolkit().getScreenSize();
        ui.setBounds(d.width / 2 - width / 2,
                d.height / 2 - height / 2,
                width, height);
        ui.setContentPane(getPanel());
        ui.setVisible(true);
    }

    void disappear() {
        ui.dispose();
    }
}
