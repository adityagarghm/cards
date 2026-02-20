import java.util.ArrayList;
import java.util.Collections;

public class PokerGame extends CardGame {

    public PokerGame() {
        super();
    }

    public void createDeck() {
        deck = new ArrayList<>();  
        ArrayList<PokerCard> extradeck = new ArrayList<>(); //poker played with an extra deck when the main deck runs out of cards
        for (String suit : PokerCard.SUITS) {
            for (String value : PokerCard.VALUES) {
                deck.add(new PokerCard(value, suit));
                extradeck.add(new PokerCard(value,suit));
            }
        }
        Collections.shuffle(extradeck);
        Collections.shuffle(deck);
    }
}