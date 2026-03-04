
import java.util.ArrayList;

import processing.core.PApplet;

public class Hand {
    private ArrayList<Card> cards;
    
    public Hand() {
        cards = new ArrayList<>();
    }
    
    public void addCard(Card card) {
        cards.add(card);
    }
    
    public void removeCard(Card card) {
        cards.remove(card);
    }
    
    public Card getCard(int index) {
        if (index >= 0 && index < cards.size()) {
            return cards.get(index);
        }
        return null;
    }
    
    public int getSize() {
        return cards.size();
    }
    
    public ArrayList<Card> getCards() {
        return cards;
    }
    
    public void positionCards(int startX, int startY, int cardWidth, int cardHeight, int spacing) {
        for (int i = 0; i < cards.size(); i++) {
            int x = startX + (i * spacing);
            cards.get(i).setPosition(x, startY, cardWidth, cardHeight);
        }
    }
    
    public void positionCardsInGrid(int startX, int startY, int cardWidth, int cardHeight, int spacing, int cardsPerRow) {
        for (int i = 0; i < cards.size(); i++) {
            int row = i / cardsPerRow;
            int col = i % cardsPerRow;
            int x = startX + (col * spacing);
            int y = startY + (row * spacing);
            cards.get(i).setPosition(x, y, cardWidth, cardHeight);
        }
    }

    void draw(PApplet sketch) {
        for (Card card : cards) {
            card.draw(sketch);
        }
    }
}
