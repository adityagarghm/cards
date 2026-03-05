import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

public class PokerHandEvaluator {

    // Hand rankings (higher = stronger)
    public static final int HIGH_CARD = 1;
    public static final int PAIR = 2;
    public static final int TWO_PAIR = 3;
    public static final int THREE_OF_KIND = 4;
    public static final int STRAIGHT = 5;
    public static final int FLUSH = 6;
    public static final int FULL_HOUSE = 7;
    public static final int FOUR_OF_KIND = 8;
    public static final int STRAIGHT_FLUSH = 9;

    // MAIN METHOD PokerGame should call
    public static int evaluate(ArrayList<PokerCard> hand,ArrayList<PokerCard> community) {

        ArrayList<PokerCard> all = new ArrayList<>();
        all.addAll(hand);
        all.addAll(community);
        return evaluateHand(all);
    }

    // ================= HAND CHECKS =================

    private static int evaluateHand(ArrayList<PokerCard> cards) {
        if (isStraightFlush(cards)) return STRAIGHT_FLUSH;
        if (isFourOfKind(cards)) return FOUR_OF_KIND;
        if (isFullHouse(cards)) return FULL_HOUSE;
        if (isFlush(cards)) return FLUSH;
        if (isStraight(cards)) return STRAIGHT;
        if (isThreeOfKind(cards)) return THREE_OF_KIND;
        if (isTwoPair(cards)) return TWO_PAIR;
        if (isPair(cards)) return PAIR;
        return HIGH_CARD;
    }

    private static boolean isPair(ArrayList<PokerCard> cards) {
        return hasDuplicates(cards, 2);
    }

    private static boolean isTwoPair(ArrayList<PokerCard> cards) {
        HashMap<Integer, Integer> counts = valueCounts(cards);
        int pairs = 0;
        for (int v : counts.values()) {
            if (v == 2) pairs++;
        }
        return pairs >= 2;
    }

    private static boolean isThreeOfKind(ArrayList<PokerCard> cards) {
        return hasDuplicates(cards, 3);
    }

    private static boolean isFourOfKind(ArrayList<PokerCard> cards) {
        return hasDuplicates(cards, 4);
    }

    private static boolean isFullHouse(ArrayList<PokerCard> cards) {
        HashMap<Integer, Integer> counts = valueCounts(cards);
        boolean three = false;
        boolean two = false;

        for (int v : counts.values()) {
            if (v == 3) three = true;
            if (v == 2) two = true;
        }
        return three && two;
    }

    private static boolean isFlush(ArrayList<PokerCard> cards) {
        HashMap<String, Integer> suits = new HashMap<>();
        for (PokerCard c : cards) {
            suits.put(c.suit, suits.getOrDefault(c.suit, 0) + 1);
        }
        for (int count : suits.values()) {
            if (count >= 5) return true;
        }
        return false;
    }

    private static boolean isStraight(ArrayList<PokerCard> cards) {
        ArrayList<Integer> values = new ArrayList<>();
        for (PokerCard c : cards) {
            values.add(cardValue(c.value));
        }

        Collections.sort(values);
        int streak = 1;

        for (int i = 1; i < values.size(); i++) {
            if (values.get(i) == values.get(i - 1) + 1) {
                streak++;
                if (streak >= 5) return true;
            } else if (values.get(i) != values.get(i - 1)) {
                streak = 1;
            }
        }
        return false;
    }

    private static boolean isStraightFlush(ArrayList<PokerCard> cards) {
        return isFlush(cards) && isStraight(cards);
    }

    // ================= HELPERS =================

    private static boolean hasDuplicates(ArrayList<PokerCard> cards, int target) {
        HashMap<Integer, Integer> counts = valueCounts(cards);
        for (int v : counts.values()) {
            if (v == target) return true;
        }
        return false;
    }

    private static HashMap<Integer, Integer> valueCounts(ArrayList<PokerCard> cards) {
        HashMap<Integer, Integer> map = new HashMap<>();
        for (PokerCard c : cards) {
            int val = cardValue(c.value);
            map.put(val, map.getOrDefault(val, 0) + 1);
        }
        return map;
    }

    private static int cardValue(String value) {
        switch (value) {
            case "A": return 14;
            case "K": return 13;
            case "Q": return 12;
            case "J": return 11;
            default: return Integer.parseInt(value);
        }
    }
}
