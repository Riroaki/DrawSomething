package client.UITest;

import client.Interact;
import client.UI.LeaderBoard;

public class TestLeaderBoard implements Interact {

    @Override
    public void die(int status) {
        System.out.println("Client died");
        System.exit(status);
    }

    @Override
    public void sendMsg(String msg) {
        System.out.println(msg);
    }

    @Override
    public String recvMsg() {
        return "jojo:0;dio:0;test:10;llq:15";
    }

    public static void main(String[] args) {
        TestLeaderBoard test = new TestLeaderBoard();
        LeaderBoard leaderBoard = new LeaderBoard("llq", test);
    }
}
