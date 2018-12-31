package server.topic;

class Topic {
    private String name, type;
    private int length;

    Topic(String n, String t, int l) {
        name = n;
        type = t;
        length = l;
    }

    public String getName() {
        return name;
    }

    public String getType() {
        return type;
    }

    public int getLength() {
        return length;
    }
}