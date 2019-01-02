package client.UITest;

import client.Interact;
import client.UI.PlayRoom;

import java.util.ArrayList;
import java.util.List;

public class TestPlayRoom implements Interact {
    private int time = -1;
    private List<String> messages;

    private TestPlayRoom() {
        messages = new ArrayList<>();
        messages.add("names,all,dio,jojo,world,test,llq");
        messages.add("draw,llq,汽车");

        messages.add("wrong,world,sss");
        messages.add("right,test");
        messages.add("wrong,world,sss");
        messages.add("right,test");
        messages.add("wrong,world,sss");
        messages.add("right,test");
        messages.add("wrong,world,sss");
        messages.add("right,test");
        messages.add("wrong,world,sss");
        messages.add("right,test");

        messages.add("time,all");
        messages.add("quit,test");

        messages.add("stop,all");
    }

    @Override
    public String recvMsg() {
        if (time < messages.size() - 1)
            time++;
        else
            time = 1;
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return messages.get(time);
    }

    @Override
    public void sendMsg(String msg) {
        String[] words = msg.split(",");
        if ("end".equals(words[0])) {
            messages.add(time, "draw,llq");
        } else if ("quit".equals(words[0])) {
            messages.add(time, "quit,llq");
        } else if ("clear".equals(words[0])) {
            messages.add(time, "clear,all");
        } else if ("guess".equals(words[0])) {
            if ("汽车".equals(words[1]))
                messages.add(time, "right,llq");
            else
                messages.add(time, "wrong,llq," + words[2]);
        }
    }

    @Override
    public void die(int status) {
        System.out.println("Client died");
        System.exit(1);
    }

    public static void main(String[] args) {
        TestPlayRoom test = new TestPlayRoom();
        PlayRoom playRoom = new PlayRoom(test, "llq");
        playRoom.showAndReact();
    }
}
