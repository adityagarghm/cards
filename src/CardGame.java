import java.util.ArrayList;
import java.util.Collections;

import processing.core.PApplet;

public abstract class CardGame {
    protected ArrayList<Card> deck = new ArrayList<>();
    Hand playerOneHand;
    Hand playerTwoHand;
    ArrayList<Card> discardPile = new ArrayList<>();
    Card selectedCard;
    int selectedCardRaiseAmount = 15;
    boolean playerOneTurn = true;
    Card lastPlayedCard;
    boolean gameActive;

    ClickableRectangle drawButton;
    int drawButtonX = 250;
    int drawButtonY = 400;
    int drawButtonWidth = 100;
    int drawButtonHeight = 35;
    ClickableRectangle playAgain;


    protected ArrayList<Player> players = new ArrayList<>();
    protected int turn = 0;

    public CardGame() {
        initializeGame();
    }

    protected void initializeGame() {
        // Initialize draw button
        drawButton = new ClickableRectangle();
        drawButton.x = drawButtonX;
        drawButton.y = drawButtonY;
        drawButton.width = drawButtonWidth;
        drawButton.height = drawButtonHeight;
        //drawButton.text = "Draw";

        // Initialize decks and hands
        deck = new ArrayList<>();
        playerOneHand = new Hand();
        playerTwoHand = new Hand();
        gameActive = true;
        players = new ArrayList<>();

        createDeck();
        dealCards(6);
    }
    protected void createDeck() {
        String[] suits = { "Hearts", "Diamonds", "Clubs", "Spades" };
        String[] values = { "2", "3", "4", "5", "6", "7", "8", "9", "10", "J", "Q", "K", "A" };
        deck.clear();
        for (String suit : suits) {
            for (String value : values) {
                deck.add(new Card(value, suit));
            }
        }
        playerOneHand.positionCards(50, 450, 80, 120, 20);
        playerTwoHand.positionCards(50, 50, 80, 120, 20);
    }

    protected void dealCards(int numCards) {
        Collections.shuffle(deck);
        for (int i = 0; i < numCards; i++) {
            if (!deck.isEmpty()) playerOneHand.addCard(deck.remove(0));
            if (!deck.isEmpty()) {
                Card card = deck.remove(0);
                card.setTurned(true);
                playerTwoHand.addCard(card);
            }
        }

        playerOneHand.positionCards(50, 450, 80, 120, 20);
        playerTwoHand.positionCards(50, 50, 80, 120, 20);
    }

    protected boolean isValidPlay(Card card) {
        return true;
    }

    public Player getCurrentPlayerObj() {
        if (players == null || players.isEmpty()) return null;
        return players.get(turn);
    }

    public void nextTurn() {
        if (players == null || players.isEmpty()) {

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
            switchTurns();
        }
    }
    public void handlePlayAgainClick(int mouseX, int mouseY) {
        if (playAgain.isClicked(mouseX, mouseY) && !gameActive) {
            initializeGame();
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
        card.setSelected(false, selectedCardRaiseAmount);
        discardPile.add(card);
        card.setTurned(false);
        lastPlayedCard = card;
        checkWinCondition();
        if (!gameActive) {
            return true;
        }
        // Switch turns
        switchTurns();
        return true;
    }
        public void checkWinCondition() {
        if (playerOneHand.getSize() == 0) {
            System.out.println("Player One wins!");
            gameActive = false;
        } else if (playerTwoHand.getSize() == 0) {
            System.out.println("Player Two wins!");
            gameActive = false;
        }
    }

    public void switchTurns() {
        playerOneTurn = !playerOneTurn;
        playerOneHand.positionCards(50, 450, 80, 120, 20);
        playerTwoHand.positionCards(50, 50, 80, 120, 20);
    }

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
        selectedCard.setSelected(false, selectedCardRaiseAmount);
        selectedCard = clickedCard;
        selectedCard.setSelected(true, selectedCardRaiseAmount);
    }

    public Card getClickedCard(int mouseX, int mouseY) {
        for (int i = playerOneHand.getSize() - 1; i >= 0; i--) {
            Card card = playerOneHand.getCard(i);
            if (card != null && card.isClicked(mouseX, mouseY)) {
                return card;
            }
        }
        return null;
    }

    public void drawChoices(PApplet app) {
    }
    public void handleKey(char key, int keyNumber){

    }
    public void update(){}
    public void draw(PApplet g) {}
}
