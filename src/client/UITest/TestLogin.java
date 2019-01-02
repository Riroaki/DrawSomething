package client.UITest;

import client.Interact;
import client.UI.Login;

public class TestLogin implements Interact {

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
        return "0,1";
    }

    public static void main(String[] args) {
        TestLogin test = new TestLogin();
        Login login = new Login(test);
        login.showAndReact();
    }
}
