import java.util.HashMap;

public class UnoComputer {
    /**
     * AI logic to decide which card to play from the hand.
     * Priorities: 
     * 1. Match the current card's suit or value.
     * 2. Play a Wild card if no normal match is found.
     */
    public UnoCard playCard(Hand h, UnoCard current) {
        if (h == null || current == null) {
            return null;
        }
        
        UnoCard wildCandidate = null;
        
        for (int i = 0; i < h.getSize(); i++) {
            UnoCard card = (UnoCard) h.getCard(i);
            if (card == null) {
                continue;
            }
            
            // Save a wild card as a backup, but try to find a color match first
            if ("Wild".equals(card.suit)) {
                if (wildCandidate == null) {
                    wildCandidate = card;
                }
                continue;
            }
            
            // Check for a standard match
            if (card.suit.equals(current.suit) || card.value.equals(current.value)) {
                return card;
            }
        }

        // If no regular match, play the wild card if we have one
        return wildCandidate;
    }

    /**
     * When a bot plays a wild card, it chooses the color 
     * it currently has the most of in its hand.
     */
    public String chooseComputerWildColor(Hand hand) {
        HashMap<String, Integer> colorCount = new HashMap<>();

        for (int i = 0; i < hand.getSize(); i++) {
            Card card = hand.getCard(i);
            if (card == null || "Wild".equals(card.suit)) {
                continue;
            }
            colorCount.put(card.suit, colorCount.getOrDefault(card.suit, 0) + 1);
        }

        String best = "Red"; // Default choice
        int maxCount = -1;
        
        for (String color : colorCount.keySet()) {
            if (colorCount.get(color) > maxCount) {
                maxCount = colorCount.get(color);
                best = color;
            }
        }
        return best;
    }
}