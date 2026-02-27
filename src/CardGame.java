import java.util.ArrayList;
import java.util.Collections;

import processing.core.PApplet;

public abstract class CardGame {
    // Core game components (teacher-provided fields)
    protected static ArrayList<Card> deck = new ArrayList<>();
    Hand playerOneHand;
    Hand playerTwoHand;
    ArrayList<Card> discardPile = new ArrayList<>();
    Card selectedCard;
    int selectedCardRaiseAmount = 15;

    // Game state (teacher-provided)
    boolean playerOneTurn = true;
    Card lastPlayedCard;
    boolean gameActive;

    // UI (teacher-provided)
    ClickableRectangle drawButton;
    int drawButtonX = 250;
    int drawButtonY = 400;
    int drawButtonWidth = 100;
    int drawButtonHeight = 35;

    // new: generic player list & turn index for games that want many players (Poker will not require modifying teacher methods)
    protected ArrayList<Player> players = new ArrayList<>();
    protected int turn = 0;

    // Single constructor (keeps teacher behavior: initializeGame + dealCards)
    public CardGame() {
        initializeGame();
        dealCards(6);
    }

    protected void initializeGame() {
        // Initialize draw button
        drawButton = new ClickableRectangle();
        drawButton.x = drawButtonX;
        drawButton.y = drawButtonY;
        drawButton.width = drawButtonWidth;
        drawButton.height = drawButtonHeight;

        // Initialize decks and hands
        deck = new ArrayList<>();
        discardPile = new ArrayList<>();
        playerOneHand = new Hand();
        playerTwoHand = new Hand();
        gameActive = true;

        // players list exists but teacher didn't populate it here; subclasses or App can
        players = new ArrayList<>();

        createDeck();
    }

    /**
     * Default deck creator: base Card deck.
     * Subclasses (PokerGame, Uno) should override this method to create appropriate cards.
     */
    protected void createDeck() {
        // Basic fallback: create standard 52-card deck of Card objects
        String[] suits = { "Hearts", "Diamonds", "Clubs", "Spades" };
        String[] values = { "2", "3", "4", "5", "6", "7", "8", "9", "10", "J", "Q", "K", "A" };
        deck.clear();
        for (String suit : suits) {
            for (String value : values) {
                deck.add(new Card(value, suit));
            }
        }
    }

    protected void dealCards(int numCards) {
        Collections.shuffle(deck);
        for (int i = 0; i < numCards; i++) {
            // give one to player one
            if (!deck.isEmpty()) playerOneHand.addCard(deck.remove(0));
            // give one (face-down) to player two
            if (!deck.isEmpty()) {
                Card card = deck.remove(0);
                card.setTurned(true);
                playerTwoHand.addCard(card);
            }
        }

        // position cards (teacher layout)
        playerOneHand.positionCards(50, 450, 80, 120, 20);
        playerTwoHand.positionCards(50, 50, 80, 120, 20);
    }

    protected boolean isValidPlay(Card card) {
        // Default: allow everything (subclasses should override if different rules needed)
        return true;
    }

    // Generic player helpers (for games that want player objects)
    public Player getCurrentPlayerObj() {
        if (players == null || players.isEmpty()) return null;
        return players.get(turn);
    }

    public void nextTurn() {
        if (players == null || players.isEmpty()) {
            // fallback to 2-player toggle using teacher boolean
            playerOneTurn = !playerOneTurn;
            return;
        }
        do {
            turn = (turn + 1) % players.size();
        } while (players.get(turn).isFolded());
    }
    public void drawCard(Hand hand) {
        if (deck != null && !deck.isEmpty()) {
            hand.addCard(deck.remove(0));
        } else if (discardPile != null && discardPile.size() > 1) {
            // Reshuffle discard pile into deck if deck is empty
            lastPlayedCard = discardPile.remove(discardPile.size() - 1);
            deck.addAll(discardPile);
            discardPile.clear();
            discardPile.add(lastPlayedCard);
            Collections.shuffle(deck);

            if (!deck.isEmpty()) {
                hand.addCard(deck.remove(0));
            }
        }
    }

    public void handleDrawButtonClick(int mouseX, int mouseY) {
        if (drawButton.isClicked(mouseX, mouseY) && playerOneTurn) {
            drawCard(playerOneHand);
            // Switch turns after drawing
            switchTurns();
        }
    }

    public boolean playCard(Card card, Hand hand) {
        // Check if card is valid to play
        if (!isValidPlay(card)) {
            System.out.println("Invalid play: " + card.value + " of " + card.suit);
            return false;
        }
        // Remove card from hand
        hand.removeCard(card);
        card.setTurned(false);
        // Add to discard pile
        discardPile.add(card);
        lastPlayedCard = card;
        // Switch turns
        switchTurns();
        return true;
    }

    public void switchTurns() {
        playerOneTurn = !playerOneTurn;
        playerOneHand.positionCards(50, 450, 80, 120, 20);
        playerTwoHand.positionCards(50, 50, 80, 120, 20);
    }

    // Teacher method: keep same behavior (2-player string)
    public String getCurrentPlayer() {
        return playerOneTurn ? "Player One" : "Player Two";
    }

    public Card getLastPlayedCard() {
        return lastPlayedCard;
    }

    public int getDeckSize() {
        return deck != null ? deck.size() : 0;
    }

    public Hand getPlayerOneHand() {
        return playerOneHand;
    }

    public Hand getPlayerTwoHand() {
        return playerTwoHand;
    }

    public void handleComputerTurn() {
        drawCard(playerTwoHand);
        switchTurns();
    }

    public void handleCardClick(int mouseX, int mouseY) {
        if (!playerOneTurn) {
            return;
        }
        Card clickedCard = getClickedCard(mouseX, mouseY);
        if (clickedCard == null) {
            return;
        }
        // this is for the first time
        if (selectedCard == null) {
            selectedCard = clickedCard;
            selectedCard.setSelected(true, selectedCardRaiseAmount);
            return;
        }

        if (selectedCard == clickedCard) {
            System.out.println("playing card: " + selectedCard.value + " of " + selectedCard.suit);
            if (playCard(selectedCard, playerOneHand)) {
                selectedCard.setSelected(false, selectedCardRaiseAmount);
                selectedCard = null;
            }
            return;
        }
        // change selection
        selectedCard.setSelected(false, selectedCardRaiseAmount);
        selectedCard = clickedCard;
        selectedCard.setSelected(true, selectedCardRaiseAmount);
    }

    // return the card that is clicked (teacher helper)
    public Card getClickedCard(int mouseX, int mouseY) {
        for (int i = playerOneHand.getSize() - 1; i >= 0; i--) {
            Card card = playerOneHand.getCard(i);
            if (card != null && card.isClicked(mouseX, mouseY)) {
                return card;
            }
        }
        return null;
    }

    // method intended to be optionally overridden by subclasses
    public void drawChoices(PApplet app) {
        // default: nothing
    }
}
