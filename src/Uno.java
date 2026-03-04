import processing.core.PApplet;
import java.util.Collections;

public class Uno extends CardGame {

    public Uno() {
        super(); // calls initializeGame() and dealCards(6) from CardGame
        // optionally create a UNO-style deck instead of CardGame's default deck
        createDeck();
        dealCards(6); // re-deal using UNO deck
    }

    // Create a UNO-like deck using Card(value,color) but using suit as color
    @Override
    public void createDeck() {
        deck.clear();
        String[] colors = {"Red", "Yellow", "Green", "Blue"};
        String[] values = {"0","1","2","3","4","5","6","7","8","9"};
        // 1 zero per color, two of 1-9 per color
        for (String c : colors) {
            deck.add(new Card("0", c));
            for (String v : values) {
                if (v.equals("0")) continue;
                deck.add(new Card(v, c));
                deck.add(new Card(v, c));
            }
        }
        // add 4 wilds
        for (int i = 0; i < 4; i++) deck.add(new Card("Wild", "Wild"));
        Collections.shuffle(deck);
    }

    @Override
    protected boolean isValidPlay(Card card) {
        // allow play if discard pile empty
        if (lastPlayedCard == null) return true;
        // Wild always allowed
        if (card.value.equals("Wild")) return true;
        // match value or suit/color
        if (card.value.equals(lastPlayedCard.value)) return true;
        if (card.suit.equals(lastPlayedCard.suit)) return true;
        return false;
    }

    // Optional: draw UNO UI (draw hands and discard pile)
    @Override
    public void draw(PApplet p) {
        // draw player two (top)
        Hand top = playerTwoHand;
        for (int i = 0; i < top.getSize(); i++) {
            Card c = top.getCard(i);
            c.draw(p);
        }
        // draw player one (bottom)
        Hand bottom = playerOneHand;
        for (int i = 0; i < bottom.getSize(); i++) {
            Card c = bottom.getCard(i);
            c.draw(p);
        }

        // draw discard top card
        if (lastPlayedCard != null) {
            lastPlayedCard.setPosition(400, 260);
            lastPlayedCard.setSize(80, 120);
            lastPlayedCard.setTurned(false);
            lastPlayedCard.draw(p);
        }

        // Draw small UI text
        p.fill(255);
        p.textAlign(PApplet.LEFT, PApplet.TOP);
        p.text("UNO - click a card to select, click again to play", 10, 10);
        p.text("Deck size: " + getDeckSize(), 10, 30);
    }
}
