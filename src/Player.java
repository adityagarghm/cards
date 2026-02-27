
public class Player {

    private String name;
    // 1. Change this from ArrayList<Card> to your custom Hand class
    private Hand hand = new Hand(); 
    private int chips = 1000;
    private int bet = 0;
    private boolean folded = false;
    private boolean bot;
    private int aggression; // 0–100, only used for bots


    public Player(String name, boolean bot) {
    this.name = name;
    this.bot = bot;

    if (bot) {
        aggression = (int)(Math.random() * 101); // 0–100
    } else {
        aggression = 0;
    }
}

    // 2. Updated to use the Hand's internal method
    public void addCard(Card c) {
        hand.addCard(c);
    }

    // 3. Changed return type to Hand to fix your "wrong return type" error
    public Hand getHand() {
        return hand;
    }
    public int getAggression() {
    return aggression;
}

    public int bet(int amount) {
        amount = Math.min(amount, chips);
        chips -= amount;
        bet += amount;
        return amount;
    }

    public void resetForRound() {
        bet = 0;
        folded = false;
        // 4. Reset the hand object for the next round
        hand = new Hand(); 
    }

    public void fold() {
        folded = true;
    }

    public boolean isFolded() {
        return folded;
    }

    public boolean isBot() {
        return bot;
    }

    public int getBet() {
        return bet;
    }

    public int getChips() {
        return chips;
    }

    public String getName() {
        return name;
    }
     public void win(int amount) {
        this.chips += amount;
    }

}