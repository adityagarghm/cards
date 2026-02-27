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
    // Card background
    sketch.fill(255);
    sketch.stroke(0);
    sketch.rect(x, y, width, height, 6);
    boolean red = suit.equals("Hearts") || suit.equals("Diamonds");
    sketch.fill(red ? sketch.color(200, 0, 0) : sketch.color(0));
    sketch.textAlign(PApplet.LEFT, PApplet.TOP);
    sketch.textSize(height * 0.18f);
    sketch.text(value, x + width * 0.08f, y + height * 0.05f);//far too much trial and error 
    sketch.pushMatrix();
    sketch.translate(x + width / 2f, y + height / 2f);
    sketch.fill(red ? sketch.color(200, 0, 0) : sketch.color(0));
    sketch.noStroke();

    float s = Math.min(width, height) * 0.25f;

    if (suit.equals("Hearts")) drawHeart(sketch, s);
    else if (suit.equals("Diamonds")) drawDiamond(sketch, s);
    else if (suit.equals("Clubs")) drawClub(sketch, s);
    else if (suit.equals("Spades")) drawSpade(sketch, s);

    sketch.popMatrix();//found this online, learned that pop and push essentially move the matrix origin, so much easier for me now 
}

private void drawHeart(PApplet p, float s) {
    p.ellipse(-s/2, -s/4, s, s);
    p.ellipse(s/2, -s/4, s, s);
    p.triangle(-s, -s/4, s, -s/4, 0, s);
}
private void drawDiamond(PApplet p, float s) {
    p.quad(0, -s, s, 0, 0, s, -s, 0);
}
private void drawClub(PApplet p, float s) {
    p.ellipse(0, -s/2, s, s);
    p.ellipse(-s/2, s/4, s, s);
    p.ellipse(s/2, s/4, s, s);
    p.rect(-s/6, s/2, s/3, s);
}
private void drawSpade(PApplet p, float s) {
    p.ellipse(-s/2, -s/4, s, s);
    p.ellipse(s/2, -s/4, s, s);
    p.triangle(-s, -s/4, s, -s/4, 0, -s);
    p.rect(-s/6, 0, s/3, s);
}
}