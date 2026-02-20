
import processing.core.PApplet;

public class MonopolyCard extends Card {

    // all monopoly cards are money cards, so we can just return the value as an int
    public MonopolyCard(String value, String suit) {
        super(value, suit);
    }

    // Money, Property, Action.
    public int getMoneyNum() {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return 0; // Non-money cards have a value of 0
        }
    } // every card has a monetary value

    @Override
    public void drawFront(PApplet sketch) {
        super.drawFront(sketch);
        // set card color based on suit
        switch (suit) {
            case "Money":
                sketch.fill(255, 215, 0); // gold color for money cards
                break;
            case "Property":
                sketch.fill(255); // white for property cards
                break;
            case "Action":
                sketch.fill(255, 182, 193); // light pink for action cards
                break;
            default:
                sketch.fill(200);
                break;
        }
        sketch.rect(x, y, width, height);

        // amount in the upper left corner for all cards
        sketch.fill(0);
        sketch.textSize(14);
        sketch.text("$" + value, x + 10, y + 20);

        if (suit == "Money") {
            // draw a dollar sign in the center
            sketch.textSize(48);
            sketch.text("$" + value, x + width / 2 - 12, y + height / 2 + 16);
            sketch.textSize(14);
        }
    }
}

class PropertyCard extends MonopolyCard {
    // property cards also have a baserent value and a color
    boolean inCompleteSet = false;
    int baseRent;
    String color;

    public PropertyCard(String value, int baseRent, String color) {
        super(value, "Property");
        this.baseRent = baseRent;
        this.color = color;
    }

    public boolean isInCompleteSet() {
        return inCompleteSet;
    }

    @Override
    public void drawFront(PApplet sketch) {
        super.drawFront(sketch);
        // draw the color bar at the top of the card
        switch (color) {
            case "Brown":
                sketch.fill(150, 75, 0);
                break;
            case "Light Blue":
                sketch.fill(173, 216, 230);
                break;
            case "Pink":
                sketch.fill(255, 182, 193);
                break;
            case "Orange":
                sketch.fill(255, 165, 0);
                break;
            case "Red":
                sketch.fill(255, 0, 0);
                break;
            case "Yellow":
                sketch.fill(255, 255, 0);
                break;
            case "Green":
                sketch.fill(0, 128, 0);
                break;
            case "Dark Blue":
                sketch.fill(0, 0, 139);
                break;
            default:
                sketch.fill(200);
                break;
        }
        sketch.rect(x, y + 30, width, 20);

        sketch.textSize(20);
        sketch.textAlign(sketch.LEFT, sketch.CENTER);
        sketch.text("property", x, y + height / 2 + 16);
        sketch.textSize(14);
        // if the property is in a complete set, draw a checkmark in the upper right
        // corner
        if (inCompleteSet) {
            sketch.fill(0);
            sketch.textSize(16);
            sketch.text("✓", x + width - 20, y + 20);
        }
    }
}

class ActionCard extends MonopolyCard {
    MonopolyDeal game; // need to affect the game here
    // action cards have an action type, which is the value field.
    String action;
    public ActionCard(String value, String action, MonopolyDeal game) {
        super(value, "Action");
        this.action = action;
        this.game = game;
    }

    public void drawFront(PApplet sketch) {
        super.drawFront(sketch);
        sketch.textSize(20);
        sketch.textAlign(sketch.LEFT, sketch.CENTER);
        sketch.text(action, x, y + height / 2 + 16);
    }

    public void performAction() {
        if ("Pass Go".equals(value)) {
            // get 2 extra cards in hand
            game.getCurrentPlayerHand().addCard(game.deck.remove(0));
            game.getCurrentPlayerHand().addCard(game.deck.remove(0));
        } else if ("Sly Deal".equals(value)) {
            // steal a property from opponent
            ((MonopolyHand) game.getCurrentPlayerHand()).propertyPile.addCard(game.selectedCard);
            if (game.playerOneTurn) {
                ((MonopolyHand) game.playerTwoHand).propertyPile.removeCard(game.selectedCard);
            } else {
                ((MonopolyHand) game.playerOneHand).propertyPile.removeCard(game.selectedCard);
            }
        } else if ("Deal Breaker".equals(value)) {
            // steal a complete set from opponent

        } else if ("Just Say No".equals(value)) {
            // cancel an opponent's action

        } else if ("Debt Collector".equals(value)) {
            // force opponent to pay you $5
            // this is actually tricky, the computer needs to wait for the user to finish
            // paying
            // and we have to program the computer to make decisions
        } else if ("It's My Birthday".equals(value)) {
            // force opponent to pay you $2
        }
    }
}
