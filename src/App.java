import processing.core.PApplet;

public class App extends PApplet {

    CardGame gamePlaying;

    public static void main(String[] args) {
        PApplet.main("App");
    }

    public void settings() {
        size(900, 650);
    }

    public void setup() {
       gamePlaying = new PokerGame(null);
        textFont(createFont("Arial", 16));
    }

    public void draw() {
        background(0, 120, 0);

        gamePlaying.update();
        gamePlaying.draw(this);
    }

    public void mousePressed() {
        gamePlaying.handleDrawButtonClick(mouseX, mouseY);
        gamePlaying.handleCardClick(mouseX, mouseY);
    }

    public void keyPressed() {
        gamePlaying.handleKey(key, keyCode);
    }
}
