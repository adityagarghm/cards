import processing.core.PApplet;

public class PokerCard extends Card {

    public static final String[] SUITS = {
        "Hearts", "Diamonds", "Clubs", "Spades"
    };

    public static final String[] VALUES = {"2", "3", "4", "5", "6", "7", "8", "9", "10", "J", "Q", "K", "A"};

    public PokerCard(String value, String suit) {
        super(value, suit);  
    }
    @Override
    public void drawFront(PApplet sketch) {
        if (img != null) {
            sketch.image(img, x, y, width, height);
        } else {
            sketch.fill(255);
            sketch.rect(x, y, width, height);
            sketch.fill(0);
            sketch.text(value, x + 10, y + 10);
            sketch.text(suit, x+10, y+40);
        }
    }

}