import processing.core.PApplet;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Random;

public class PokerGame extends CardGame {

    private final PApplet app;
    private static final long BOT_ACTION_DELAY_MS = 600L; // 600 ms delay between bot actions
    private long lastBotActionTime = 0;

    private final ArrayList<Player> pPlayers = new ArrayList<>();
    private final ArrayList<PokerCard> communityCards = new ArrayList<>();
    private int dealerIndex = 0;
    private int currentPlayerIndex = 0;

    // money & tracking
    private int pot = 0;
    private int currentBet = 0;           
    private int minRaise = 0;
    private boolean roundActive = false;

    // per-player current bet for this round (separate from Player.bet)
    private int[] playerCurrentBets = new int[0];
    private boolean hideTableCards = false;


    // blinds
    private final int SMALL_BLIND = 10;
    private final int BIG_BLIND = 20;

    // betting control
    private int lastAggressorIndex = -1;
    private int raisesThisRound = 0;
    private final int MAX_RAISES_PER_ROUND = 6;
    private long[] lastActionTime = new long[0];
    private static final long ACTION_DISPLAY_MS = 2500L;

    // per-round per-player action tracker
    private boolean[] hasActed = new boolean[0];

    // game stages

        private static final int PREFLOP = 0;
        private static final int FLOP    = 1;
        private static final int TURN    = 2;
        private static final int RIVER   = 3;
        private static final int SHOWDOWN= 4;
        private int stage = PREFLOP;
private String[] lastAction = new String[0];

        private String stageName() {
            switch(stage) {
                case PREFLOP:  return "PREFLOP";
                case FLOP:     return "FLOP";
                case TURN:     return "TURN";
                case RIVER:    return "RIVER";
                case SHOWDOWN: return "SHOWDOWN";
                default:       return "UNKNOWN";
            }
        }

    // raise typing
    private boolean enteringRaise = false;
    private int typedRaise = 0;

    // debounce
    private long lastClickTime = 0;
    private static final long CLICK_DEBOUNCE_MS = 120L;

    private final Random rnd = new Random();

    private static class Button { int x,y,w,h; String label; 
    Button(int x,int y,int w,int h,String label)
    {this.x=x;
    this.y=y;
    this.w=w;
    this.h=h;
    this.label=label;
    } 
    boolean contains(int mx,int my)
    {return mx>=x && mx<=x+w && my>=y && my<=y+h;} 
    }
    private final Button btnCall  = new Button(50,520,140,40,"CALL / CHECK");
    private final Button btnRaise = new Button(220,520,140,40,"RAISE");
    private final Button btnFold  = new Button(390,520,140,40,"FOLD");
    private final Button btnNewHand= new Button(560,520,140,40,"NEW HAND");
    private final Button btnEndGame = new Button(720, 520, 140, 40, "END GAME");


    // action log
    private final ArrayList<String> actionLog = new ArrayList<>();
    private static final int MAX_LOG = 5;
    private Player gameWinner = null;

    public PokerGame(PApplet app) {
        super();
        this.app = app;
        pPlayers.clear();
        pPlayers.add(new Player("You", false));
        pPlayers.add(new Player("Bot 1", true));
        pPlayers.add(new Player("Bot 2", true));
        pPlayers.add(new Player("Bot 3", true));
        createDeck();
        startNewHand();
    }

    // ---------- deck ----------
    @Override
    protected void createDeck() {
        deck = new ArrayList<>();
        for (String s : PokerCard.SUITS) for (String v : PokerCard.VALUES) deck.add(new PokerCard(v, s));
        Collections.shuffle(deck);//shuffle deck
    }

    // ---------- new hand (with blinds) ----------
    private void startNewHand() {
        gameWinner = null;
        hideTableCards = false; // show cards again      
        for (Player p : pPlayers) p.resetForRound();
        int n = pPlayers.size();
        hasActed = new boolean[n];
        playerCurrentBets = new int[n];
        for (int i=0;i<n;i++){ hasActed[i] = false; playerCurrentBets[i] = 0; }
        lastAction = new String[n];
        lastActionTime = new long[n];
        for (int i=0;i<n;i++) { lastAction[i] = ""; lastActionTime[i] = 0L; }


        communityCards.clear();
        pot = 0;
        currentBet = 0;
        minRaise = BIG_BLIND;
        lastAggressorIndex = -1;
        raisesThisRound = 0;
        enteringRaise = false;
        typedRaise = 0;
        stage = PREFLOP;
        roundActive = true;
        actionLog.clear();

        if (deck.size() < 20) createDeck();

        // deal hole cards
        for (int r=0;r<2;r++){
            for (Player p : pPlayers) {
                if (deck.isEmpty()) createDeck();
                Card c = deck.remove(0);
                p.addCard(c);
            }
        }

        // blinds: small = dealer+1, big = dealer+2
        int sbIndex = (dealerIndex + 1) % pPlayers.size();
        int bbIndex = (dealerIndex + 2) % pPlayers.size();

        Player sbPlayer = pPlayers.get(sbIndex);
        Player bbPlayer = pPlayers.get(bbIndex);

        int sbPosted = sbPlayer.bet(Math.min(SMALL_BLIND, sbPlayer.getChips()));
        pot += sbPosted;
        playerCurrentBets[sbIndex] += sbPosted;

        int bbPosted = bbPlayer.bet(Math.min(BIG_BLIND, bbPlayer.getChips()));
        pot += bbPosted;
        playerCurrentBets[bbIndex] += bbPosted;

        currentBet = bbPosted;
        minRaise = BIG_BLIND;

        // first to act = player after big blind
        currentPlayerIndex = (bbIndex + 1) % pPlayers.size();

        pushAction("New hand dealt. " + pPlayers.get(currentPlayerIndex).getName() + " to act. Blinds posted: SB=" + sbPosted + " BB=" + bbPosted);
    }

    // ---------- community ----------
    private void dealCommunity(int count) {
        for (int i=0;i<count;i++){
            if (deck.isEmpty()) createDeck();
            communityCards.add((PokerCard) deck.remove(0));
        }
    }

    // ---------- update (auto-bots) ----------
    @Override
 public void update() {
    if (!roundActive) return;
    if (allActivePlayersAllIn()) {
        stage = SHOWDOWN;
        endRoundAndPayout();
        return;
    }

    Player cur = pPlayers.get(currentPlayerIndex);
    if (cur == null) return;

    long now = System.currentTimeMillis();

    if (!cur.isFolded() && cur.getChips() == 0) {
        if (now - lastBotActionTime < BOT_ACTION_DELAY_MS) return; 
        lastBotActionTime = now;
        advanceTurn();
        return;
    }
    if (cur.isBot() && !cur.isFolded() && cur.getChips() > 0) {
        if (now - lastBotActionTime < BOT_ACTION_DELAY_MS) return;
        botAction(cur);
        lastBotActionTime = now;
        if (isBettingRoundComplete()) proceedStageOrShowdown();
        else advanceTurn();
        checkEndConditions();
        return;
    }
}


    // ---------- bot logic (uses Player) ----------
    private void botAction(Player bot) {
        
        int idx = pPlayers.indexOf(bot);
        if (idx < 0 || bot.isFolded()) return;

        int need = Math.max(0, currentBet - playerCurrentBets[idx]);
        int strength = PokerHandEvaluator.evaluate(
            getPokerHandAsList(bot),
            new ArrayList<>(communityCards)
        );
        int aggr = bot.getAggression();      
        int roll = rnd.nextInt(100);     
        if (strength < PokerHandEvaluator.PAIR) {
            if (need == 0 && currentBet == 0) {
                if (Math.random() < 0.5 && bot.getChips() >= BIG_BLIND) {
                    int raiseAmt = BIG_BLIND;
                    int put = bot.bet(raiseAmt);
                    pot += put;
                    playerCurrentBets[idx] += put;
                    currentBet = playerCurrentBets[idx];
                    lastAggressorIndex = idx;
                    raisesThisRound++;
                    pushAction(bot.getName() + " raises for " + raiseAmt + ".");
                    for (int i = 0; i < pPlayers.size(); i++) {
                        if (pPlayers.get(i).isFolded() || pPlayers.get(i).getChips() == 0)
                            hasActed[i] = true;
                        else
                            hasActed[i] = (i == idx);
                    }

                    return;
                }

                // default check
                int put = bot.bet(need);
                pot += put;
                playerCurrentBets[idx] += put;

                pushAction(bot.getName() + (need > 0 ? " called " + put + "." : " checked."));
                hasActed[idx] = true;
                return;
            }
            if (need > bot.getChips() / 6 && roll > aggr) {
            bot.fold();
            pushAction(bot.getName() + " folded.");
            lastAction[idx] = "folded";
            lastActionTime[idx] = System.currentTimeMillis();
            hasActed[idx] = true;

            // after fold, check if betting round should advance
            if (isBettingRoundComplete()) {
                proceedStageOrShowdown();
            } else {
                advanceTurn();
            }
        }
            int put = bot.bet(need);
            pot += put;
            playerCurrentBets[idx] += put;
            dumpPlayerBetsDebug();
            checkEndConditions();            
            pushAction(bot.getName() + " called " + put + ".");
            lastAction[idx] = "checked";
            lastActionTime[idx] = System.currentTimeMillis();
            hasActed[idx] = true;
            return;
        }

        // MEDIUM HAND
        if (strength < PokerHandEvaluator.THREE_OF_KIND) {

            if (need == 0 && currentBet == 0) {
                // 50% chance to open betting
                if (roll < 0.5 && bot.getChips() >= BIG_BLIND) {

                    int raiseAmt = BIG_BLIND;
                    int put = bot.bet(raiseAmt);
                    pot += put;
                    playerCurrentBets[idx] += put;
                    currentBet = playerCurrentBets[idx];
                    lastAggressorIndex = idx;
                    raisesThisRound++;
                    dumpPlayerBetsDebug();
                    checkEndConditions();
                    pushAction(bot.getName() + " opens for " + raiseAmt + ".");

                    for (int i = 0; i < pPlayers.size(); i++) {
                        if (pPlayers.get(i).isFolded() || pPlayers.get(i).getChips()==0)
                            hasActed[i] = true;
                        else
                            hasActed[i] = (i == idx);
                    }

                    return;
                }
            }

            if (need == 0 || roll > aggr) {
                int put = bot.bet(need);
                pot += put;
                playerCurrentBets[idx] += put;
                dumpPlayerBetsDebug();
                checkEndConditions();
                pushAction(bot.getName() + (need > 0 ? " called " + put + "." : " checked."));
                lastAction[idx] = "checked";
                lastActionTime[idx] = System.currentTimeMillis();
                hasActed[idx] = true;
                return;
            }
        }

        // STRONG HAND -> MAY RAISE
        if (raisesThisRound < MAX_RAISES_PER_ROUND && roll < aggr && bot.getChips() > need + minRaise) {
            int raiseAmt = minRaise + rnd.nextInt(Math.min(40, bot.getChips() / 6 + 1));
            raiseAmt = Math.min(raiseAmt, bot.getChips() - need);
            if (raiseAmt >= minRaise) {
                int put = bot.bet(need + raiseAmt);
                pot += put;
                playerCurrentBets[idx] += put;
                currentBet = Math.max(currentBet, playerCurrentBets[idx]);
                lastAggressorIndex = idx;
                raisesThisRound++;
                minRaise = Math.max(minRaise, raiseAmt);
                dumpPlayerBetsDebug();
                checkEndConditions();
                pushAction(bot.getName() + " raised " + raiseAmt + ".");
                for (int i = 0; i < pPlayers.size(); i++) {
                    hasActed[i] = (i == idx) || pPlayers.get(i).isFolded() || pPlayers.get(i).getChips()==0;
                }
                return;
            }
        }

        // DEFAULT -> CALL / CHECK
        int put = bot.bet(need);
        pot += put;
        playerCurrentBets[idx] += put;
        dumpPlayerBetsDebug();
        checkEndConditions();
        pushAction(bot.getName() + (need > 0 ? " called " + put + "." : " checked."));
        lastAction[idx] = "checked";
        lastActionTime[idx] = System.currentTimeMillis();
        hasActed[idx] = true;
    }

    private ArrayList<PokerCard> getPokerHandAsList(Player p) {
        ArrayList<PokerCard> cards = new ArrayList<>();
        Hand h = p.getHand();
        for (int i = 0; i < h.getSize(); i++) {
            Card c = h.getCard(i);
            if (c instanceof PokerCard) cards.add((PokerCard)c);//check - again looked online for what instanceof does
        }
        return cards;
    }

    // ---------- human actions --------
    public void humanCall() {
        if (!roundActive) return;
        Player cur = pPlayers.get(currentPlayerIndex);
        if (cur == null || cur.isFolded() || cur.isBot()) return;
        int need = Math.max(0, currentBet - playerCurrentBets[currentPlayerIndex]);
        int put = cur.bet(need);
        pot += put;
        playerCurrentBets[currentPlayerIndex] += put;
        dumpPlayerBetsDebug();
        checkEndConditions();
        pushAction(cur.getName() + (need>0 ? " called " + put + "." : " checked."));
        lastAction[currentPlayerIndex] = (need>0 ? "called " + put : "checked");
        lastActionTime[currentPlayerIndex] = System.currentTimeMillis();
        hasActed[currentPlayerIndex] = true;
        if (isBettingRoundComplete()) proceedStageOrShowdown();
        else advanceTurn();
    }

    public void beginRaiseTyping() {
        if (!roundActive) return;
        Player cur = pPlayers.get(currentPlayerIndex);
        if (cur == null || cur.isFolded() || cur.isBot()) return;
        enteringRaise = true;
        typedRaise = 0;
    }

    public void confirmRaise() {
        if (!roundActive) { 
            enteringRaise=false;
             typedRaise=0; return; 
            }
        Player cur = pPlayers.get(currentPlayerIndex);
        if (cur == null || cur.isFolded() || cur.isBot()) {
             enteringRaise=false; 
             typedRaise=0; return;
            }

        int need = Math.max(0, currentBet - playerCurrentBets[currentPlayerIndex]);
        int raiseAmt = typedRaise; 
        if (raiseAmt < minRaise) {
            if (raiseAmt == 0) {
                int put = cur.bet(need);
                pot += put;
                playerCurrentBets[currentPlayerIndex] += put;
                dumpPlayerBetsDebug();
                checkEndConditions();
                pushAction(cur.getName() + (need>0 ? " called " + put + "." : " checked."));
                hasActed[currentPlayerIndex] = true;
            } else {
                pushAction("Raise must be at least " + minRaise + ". Raise canceled.");
            }
            enteringRaise = false; typedRaise = 0;
            if (isBettingRoundComplete()) proceedStageOrShowdown(); else advanceTurn();
            return;
        }

        int maxRaisePossible = Math.max(0, cur.getChips() - need);
        raiseAmt = Math.min(raiseAmt, maxRaisePossible);
        if (raiseAmt <= 0) {
            int put = cur.bet(need);
            pot += put;
            playerCurrentBets[currentPlayerIndex] += put;
            dumpPlayerBetsDebug();
            checkEndConditions();
            pushAction(cur.getName() + " called " + put + ".");
            enteringRaise = false; typedRaise = 0;
            hasActed[currentPlayerIndex] = true;
            if (isBettingRoundComplete()) proceedStageOrShowdown(); else advanceTurn();
            return;
        }

        int put = cur.bet(need + raiseAmt);
        pot += put;
        playerCurrentBets[currentPlayerIndex] += put;
        currentBet = Math.max(currentBet, playerCurrentBets[currentPlayerIndex]);
        lastAggressorIndex = currentPlayerIndex;
        raisesThisRound++;
        minRaise = Math.max(minRaise, raiseAmt);                
        dumpPlayerBetsDebug();
        checkEndConditions();
        pushAction(cur.getName() + " raised " + (need + raiseAmt) + " (raise=" + raiseAmt + ").");
        lastAction[currentPlayerIndex] = "raised " + (need + raiseAmt);
        lastActionTime[currentPlayerIndex] = System.currentTimeMillis();

        for (int i=0;i<pPlayers.size();i++){
            if (pPlayers.get(i).isFolded() || pPlayers.get(i).getChips()==0) hasActed[i] = true;
            else hasActed[i] = (i==currentPlayerIndex);
        }

        enteringRaise = false; typedRaise = 0;

        if (isBettingRoundComplete()) proceedStageOrShowdown();
        else advanceTurn();
    }

    public void humanFold() {
        if (!roundActive) return;
        Player cur = pPlayers.get(currentPlayerIndex);
        if (cur == null || cur.isBot()) return;
        cur.fold();
        pushAction(cur.getName() + " folded.");
        lastAction[currentPlayerIndex] = "folded";
        lastActionTime[currentPlayerIndex] = System.currentTimeMillis();
        hasActed[currentPlayerIndex] = true;
        if (isBettingRoundComplete()) proceedStageOrShowdown();
        else advanceTurn();
    }

    // ---------- betting round helpers ----------
    private boolean isBettingRoundComplete() {
        // count active players
        int active = 0;
            for (Player p : pPlayers) {
                if (!p.isFolded()) active++;
            }
        for (int i = 0; i < pPlayers.size(); i++) {
            Player p = pPlayers.get(i);
            if (p.isFolded()) continue;
            if (p.getChips() == 0) continue; 
            if (!hasActed[i]) return false;
            // if player still has chips, they must match current bet
            if (playerCurrentBets[i] < currentBet) return false;

        }
        return true;
    }

        private void proceedStageOrShowdown() {

        if (stage == RIVER) {
            stage = SHOWDOWN;
            endRoundAndPayout();
            return;
        }
        if (stage == PREFLOP) {
            stage = FLOP;
            dealCommunity(3);
            pushAction("Flop dealt.");
        }
        else if (stage == FLOP) {
            stage = TURN;
            dealCommunity(1);
            pushAction("Turn dealt.");
        }
        else if (stage == TURN) {
            stage = RIVER;
            dealCommunity(1);
            pushAction("River dealt.");
        }

        for (int i = 0; i < pPlayers.size(); i++) {
            playerCurrentBets[i] = 0;
            hasActed[i] = pPlayers.get(i).isFolded() || pPlayers.get(i).getChips() == 0;
        }

        currentBet = 0;
        raisesThisRound = 0;
        minRaise = BIG_BLIND;
        lastAggressorIndex = -1;

        for (int i = 0; i < pPlayers.size(); i++) {
            hasActed[i] = pPlayers.get(i).isFolded() || pPlayers.get(i).getChips() == 0;
        }

        // set first active player to act
        resetNextActor();
    }

    private void advanceTurn() {
    int attempts = 0;
    do {
        currentPlayerIndex = (currentPlayerIndex + 1) % pPlayers.size();
        attempts++;
        if (attempts > pPlayers.size()*2) break;
    } while (pPlayers.get(currentPlayerIndex).isFolded() || pPlayers.get(currentPlayerIndex).getChips() == 0);
}
    private void checkEndConditions() {
        int alive = 0;
        Player last = null;

        for (Player p : pPlayers) {
            if (!p.isFolded()) {
                alive++;
                last = p;
            }
        }

        if (alive == 1) {
            if (last != null) {
                last.win(pot);
                pushAction(last.getName() + " wins by fold!");
            }
            pot = 0;
            roundActive = false;
        }
    }


    // ---------- showdown / payout ----------
    private void endRoundAndPayout() {
        roundActive = false;
        ArrayList<Player> cont = new ArrayList<>();
        for (Player p : pPlayers) if (!p.isFolded()) cont.add(p);
        if (cont.size() == 0) return;

        Player best = cont.get(0);
        int bestRank = PokerHandEvaluator.evaluate(getPokerHandAsList(best), new ArrayList<>(communityCards));
        for (Player pl : cont) {
            int r = PokerHandEvaluator.evaluate(getPokerHandAsList(pl), new ArrayList<>(communityCards));
            if (r > bestRank) { best = pl; bestRank = r; }
        }
        best.win(pot);
        pushAction(best.getName() + " wins " + pot + " chips at showdown!");
        pot = 0;
        checkGameWinCondition();
    }

    // ---------- drawing ----------
    @Override
    public void draw(PApplet g) {
        // stage & turn display (top-center)
        g.fill(255);
        g.textAlign(PApplet.CENTER, PApplet.TOP);
        String stageTxt = "Stage: " + stageName();
        String turnTxt = "Turn: " + pPlayers.get(currentPlayerIndex).getName() + (roundActive ? "" : " (round over)");
        g.text(stageTxt + "    " + turnTxt, g.width/2f, 8);

        // action log top-left
        g.textAlign(PApplet.LEFT, PApplet.TOP);
        int msgY = 28;
        for (int i = 0; i < actionLog.size(); i++) {
            g.fill(255);
            g.text(actionLog.get(i), 10, msgY + i*16);
        }

        // community cards
       
        float cx = 300, cy = 140;
        float cardW = 70, cardH = 100;
        if (!hideTableCards) {
        for (int i = 0; i < communityCards.size(); i++) {
            PokerCard c = communityCards.get(i);
            c.setSize((int)cardW, (int)cardH);
            c.setPosition((int)(cx + (i - (communityCards.size()-1)/2.0f) * (cardW + 8)), (int)cy);
            c.setTurned(false);
            c.draw(g);
        }
    }

        // players area (cards, name, chips, bet)
        float baseY = 320;
            for (int i = 0; i < pPlayers.size(); i++) {
                Player pl = pPlayers.get(i);
                float px = 80 + i * 200;

                Hand h = pl.getHand();
                float drawX = px;
            for (int j = 0; j < h.getSize(); j++) {
        Card cc = h.getCard(j);
        PokerCard c = (cc instanceof PokerCard) ? (PokerCard) cc : null;

        if (c != null) {
            c.setSize((int)cardW, (int)cardH);
            c.setPosition((int)(drawX + j * (cardW + 6)), (int)baseY);

            if (pl.isBot()) {
                // bots: hide cards unless showdown
                if (stage == SHOWDOWN) {
                    c.setTurned(false); 
                } else {
                    c.setTurned(true);  
                }
            } else {
                c.setTurned(false);
            }
            c.draw(g);
        } else {
            g.fill(80);
            g.rect(
                (int)(drawX + j * (cardW + 6)),
                (int)baseY,
                (int)cardW,
                (int)cardH,
                6
            );
        }
           
    }
            g.fill(pl.isFolded() ? 150 : 255);
            g.textAlign(PApplet.CENTER, PApplet.TOP);
            g.text(pl.getName(), px + cardW, baseY + cardH + 6);

            g.fill(255);
            g.textAlign(PApplet.CENTER, PApplet.TOP);
            g.text("$" + pl.getChips(), px + cardW, baseY + cardH + 24);

            g.fill(200);
            g.textAlign(PApplet.CENTER, PApplet.TOP);
            g.text("Bet: " + playerCurrentBets[i], px + cardW, baseY + cardH + 42);
            long t = lastActionTime[i];
            if (t > 0 && System.currentTimeMillis() - t <= ACTION_DISPLAY_MS) {
                g.fill(180);
                g.textAlign(PApplet.CENTER, PApplet.TOP);
                g.text(lastAction[i], px + cardW, baseY + cardH + 60);
            }
            if (pl.getChips() == 0 && !pl.isFolded()) {
                g.fill(255,200,0);
                g.textAlign(PApplet.CENTER, PApplet.TOP);
                g.text("ALL-IN", px + cardW, baseY + cardH + 58);
            }
        }

        // pot top-right
        g.textAlign(PApplet.RIGHT, PApplet.TOP);
        g.fill(255);
        g.text("Pot: " + pot, g.width - 10, 8);
        drawUI(g);
    }

    private void drawUI(PApplet g) {
        Player cur = pPlayers.get(currentPlayerIndex);
        if (roundActive && cur != null && !cur.isBot() && !cur.isFolded()) {
            drawButton(g, btnCall);
            drawButton(g, btnRaise);
            drawButton(g, btnFold);
            if (enteringRaise) {
                g.fill(0);
                g.rect(200,470,300,35,8);
                g.fill(255);
                g.textAlign(PApplet.CENTER, PApplet.CENTER);
                g.text("Raise: $" + typedRaise + " (ENTER)", 200+150, 470+35/2);
            }
        }

        // always draw end-game button
        drawButton(g, btnEndGame);

        // only draw new-hand if round over
        if (!roundActive) drawButton(g, btnNewHand);
         if (gameWinner != null) {
                g.fill(24, 60, 155); // bright yellow
                g.textAlign(PApplet.CENTER, PApplet.CENTER);
                g.textSize(48);       // big text
                g.text("GAME WINNER: " + gameWinner.getName(), g.width/2f, g.height/2f);
                g.textSize(16);       // reset text size for normal drawing later
            }

    }

    private void drawButton(PApplet g, Button b) {
        g.fill(200);
        g.rect(b.x, b.y, b.w, b.h, 8);
        g.fill(0);
        g.textAlign(PApplet.CENTER, PApplet.CENTER);
        g.text(b.label, b.x + b.w/2, b.y + b.h/2);
    }

    // ---------- input ----------
    @Override
    public void handleCardClick(int mx, int my) {
        long now = System.currentTimeMillis();
        if (now - lastClickTime < CLICK_DEBOUNCE_MS) return;
        lastClickTime = now;

        if (enteringRaise) return;

        if (!roundActive && btnNewHand.contains(mx,my)) {
            dealerIndex = (dealerIndex + 1) % pPlayers.size();
            createDeck();
            startNewHand();
            return;
        }

        Player cur = pPlayers.get(currentPlayerIndex);
        if (cur == null) return;
        if (cur.isBot() || cur.isFolded()) return;

        if (btnCall.contains(mx,my)) { humanCall(); return; }
        if (btnRaise.contains(mx,my)) { beginRaiseTyping(); return; }
        if (btnFold.contains(mx,my)) { humanFold(); return; }    
        if (btnEndGame.contains(mx,my)) { endGame(null);return;}
}
    @Override
    public void handleKey(char keyChar, int keyCode) {
        if (!enteringRaise) return;
        if (Character.isDigit(keyChar)) {
            if (typedRaise < 1_000_000) typedRaise = typedRaise * 10 + (keyChar - '0');
        } else if (keyCode == PApplet.BACKSPACE) typedRaise = typedRaise / 10;
        else if (keyCode == PApplet.ENTER || keyCode == PApplet.RETURN) confirmRaise();
        else if (keyCode == PApplet.ESC) { enteringRaise = false; typedRaise = 0; }
    }

    // ---------- helpers ----------
    private void pushAction(String s) {
        actionLog.add(0, s);
        while (actionLog.size() > MAX_LOG) actionLog.remove(actionLog.size()-1);
        System.out.println(s);
    }

    // external getters
    public boolean isRoundActive() { return roundActive; }
    public int getPot() { return pot; }
    public ArrayList<PokerCard> getCommunityCards() { return communityCards; }
    private boolean allActivePlayersAllIn() {
        int activeWithChips = 0;
        for (Player p : pPlayers) {
            if (p.isFolded()) continue;
            if (p.getChips() > 0) activeWithChips++;
        }
        return activeWithChips <= 1;
    }
    private void dumpPlayerBetsDebug() {//debugger
    StringBuilder sb = new StringBuilder("BETS:");
    for (int i = 0; i < pPlayers.size(); i++) {
        sb.append(" [").append(pPlayers.get(i).getName()).append(":").append(playerCurrentBets[i]).append("]");
    }
    System.out.println(sb.toString());
}
    @Override
    public  void checkWinCondition() {
        return;
    }
        private void resetNextActor() {
    for (int i = 0; i < pPlayers.size(); i++) {
        int idx = (dealerIndex + 1 + i) % pPlayers.size();
        if (!pPlayers.get(idx).isFolded() && pPlayers.get(idx).getChips() > 0) {
            currentPlayerIndex = idx;
            return;
        }
    }
}
// game ending methods
    private void checkGameWinCondition() {
    int playersWithMoney = 0;
    Player richest = null;
    for (Player p : pPlayers) {
        if (p.getChips() > 0) {
            playersWithMoney++;
            if (richest == null || p.getChips() > richest.getChips()) richest = p;
        }
    }

    if (playersWithMoney <= 1) {
        endGame(richest); 
    }
}

    // ---------- End Game ----------
    public void endGame(Player winner) {
    if (winner == null) {
        winner = pPlayers.get(0);
        int maxChips = winner.getChips();
        for (Player p : pPlayers) {
            if (p.getChips() > maxChips) {
                winner = p;
                maxChips = p.getChips();
            }
        }
    }

    gameWinner = winner;
    pushAction("GAME ENDED! " + gameWinner.getName() + " win(s) with $" + gameWinner.getChips());

    roundActive = false;
    hideTableCards = true;
    communityCards.clear();

    // reset hands
    for (Player p : pPlayers) p.resetForRound();

    currentBet = 0;
    raisesThisRound = 0;
    minRaise = BIG_BLIND;
    lastAggressorIndex = -1;
    for (int i = 0; i < pPlayers.size(); i++) {
        hasActed[i] = true;
        playerCurrentBets[i] = 0;
        lastAction[i] = "";
    }
    for (Player p : pPlayers) {
        p.resetForRound();       
        p.resetChips(1000);       
    }
}


}

