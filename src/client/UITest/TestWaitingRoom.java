package client.UITest;

import client.Interact;
import client.UI.WaitingRoom;

import java.util.ArrayList;
import java.util.List;

public class TestWaitingRoom implements Interact {
    private List<String> info;
    private int id = 0;

    private TestWaitingRoom() {
        info = new ArrayList<>();
        info.add("0,1");
        info.add("0,-1");
        info.add("stop");
        info.add("start");
    }

    @Override
    public void die(int status) {
        System.exit(status);
    }

    @Override
    public void sendMsg(String msg) {
        System.out.println(msg);
    }

    @Override
    public String recvMsg() {
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return info.get(id++%info.size());
    }

    public static void main(String[] args) {
        TestWaitingRoom test = new TestWaitingRoom();
        WaitingRoom waitingRoom = new WaitingRoom(test, "llq", 0, 1);
        waitingRoom.showAndReact();
    }
}
