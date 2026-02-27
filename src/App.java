import processing.core.PApplet;

public class App extends PApplet {

    PokerGame poker;

    public static void main(String[] args) {
        PApplet.main("App");
    }

    public void settings() {
        size(900, 650);
    }

    public void setup() {
        poker = new PokerGame(this);
        textFont(createFont("Arial", 16));
    }

    public void draw() {
        background(0, 120, 0);

        poker.update();
        poker.drawGame(this);
    }

    public void mousePressed() {
        poker.handleMouse(mouseX, mouseY);
    }

    public void keyPressed() {
        poker.handleKey(key, keyCode);
    }
}
