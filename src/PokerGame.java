import java.util.ArrayList;
import java.util.Collections;

public class PokerGame extends CardGame {
private Player[] players;
private int currentPlayerIndex;
private final int NUM_PLAYERS = 4;

    public PokerGame() {
        super();
         players = new Player[4];
        players[0] = new Player("You", true);
        players[1] = new Player("Bot 1", false);
        players[2] = new Player("Bot 2", false);
        players[3] = new Player("Bot 3", false);
        currentPlayerIndex = 0;
    }
    @Override
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
    public void dealInitialCards() {
    for (Player p : players) {
        p.clearHand();
        p.addCard(deck.remove(0));
        p.addCard(deck.remove(0));
    }
    }
    public void startGame() {
        dealInitialCards();
        System.out.println(getCurrentPlayer());
        nextTurn();
    }
    @Override
    public String getCurrentPlayer() {
        return "Player " + (currentPlayerIndex + 1);
    }

    public void nextTurn() {
        currentPlayerIndex = (currentPlayerIndex + 1) % NUM_PLAYERS;
    }
}