import java.util.ArrayList;

public class PokerPlayer extends Player {

    public int currentBet = 0;

    public PokerPlayer(String name, boolean bot) {
        super(name, bot); // 🔥 inheritance
    }

    public void resetForHand() {
        currentBet = 0;
        resetForRound(); // inherited method
    }

    public int placeBet(int amount) {
        int put = bet(amount); // inherited bet()
        currentBet += put;
        return put;
    }

  public ArrayList<PokerCard> getPokerHand() {
    ArrayList<PokerCard> cards = new ArrayList<>();

    Hand h = getHand(); // ← use accessor, not field

    for (int i = 0; i < h.getSize(); i++) {
        Card c = h.getCard(i);

        if (c instanceof PokerCard) {
            cards.add((PokerCard) c);
        }
    }
    return cards;
}


}
