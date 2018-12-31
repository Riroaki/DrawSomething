package client.UI;

import java.awt.*;

class MyPoint extends Point {
    private Color color;
    private int strokeType;
    private boolean continuous;

    MyPoint(int x, int y, Color color, int stroke, boolean cont) {
        this.x = x;
        this.y = y;
        this.color = color;
        strokeType = stroke;
        continuous = cont;
    }

    Color getColor() {
        return color;
    }

    int getStroke() {
        int[] strokes = {2, 5, 15};
        return strokes[strokeType];
    }

    boolean getContinuous() {
        return continuous;
    }
}
