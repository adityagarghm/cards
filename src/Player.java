import java.util.ArrayList;

public class Player {

    private ArrayList<Card> hand;
    private boolean isHuman;
    private String name;

    public Player(String name, boolean isHuman) {
        this.name = name;
        this.isHuman = isHuman;
        hand = new ArrayList<>();
    }

    public void addCard(Card c) {
        hand.add(c);
    }

    public ArrayList<Card> getHand() {
        return hand;
    }

    public boolean isHuman() {
        return isHuman;
    }

    public String getName() {
        return name;
    }

    public void clearHand() {
        hand.clear();
    }
}
