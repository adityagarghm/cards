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

    // blinds
    private final int SMALL_BLIND = 10;
    private final int BIG_BLIND = 20;

    // betting control
    private int lastAggressorIndex = -1;
    private int raisesThisRound = 0;
    private final int MAX_RAISES_PER_ROUND = 6;

    // per-round per-player action tracker
    private boolean[] hasActed = new boolean[0];

    // game stages

        private static final int PREFLOP = 0;
        private static final int FLOP    = 1;
        private static final int TURN    = 2;
        private static final int RIVER   = 3;
        private static final int SHOWDOWN= 4;
        private int stage = PREFLOP;

        // helper to show stage text (used in drawGame)
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

    // action log
    private final ArrayList<String> actionLog = new ArrayList<>();
    private static final int MAX_LOG = 8;

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
        // reset Players for new hand
        for (Player p : pPlayers) p.resetForRound();
        int n = pPlayers.size();
        hasActed = new boolean[n];
        playerCurrentBets = new int[n];
        for (int i=0;i<n;i++){ hasActed[i] = false; playerCurrentBets[i] = 0; }

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
   public void update() {
    if (!roundActive) return;
    Player cur = pPlayers.get(currentPlayerIndex);
    if (cur == null) return;

    long now = System.currentTimeMillis();

    // If current player is human but all-in (no chips), auto-advance so bots play out
    if (!cur.isBot() && cur.getChips() == 0 && !cur.isFolded()) {
        if (now - lastBotActionTime < BOT_ACTION_DELAY_MS) return;
        lastBotActionTime = now;
        advanceTurn();
        return;
    }

    if (cur.isBot() && !cur.isFolded() && cur.getChips() > 0) {
        if (now - lastBotActionTime < BOT_ACTION_DELAY_MS) 
            return; // delay 
        botAction(cur);
        lastBotActionTime = now;
        if (isBettingRoundComplete()) proceedStageOrShowdown();
        else advanceTurn();
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

        // WEAK HAND
        if (strength < PokerHandEvaluator.PAIR) {
            if (need == 0) {
                pushAction(bot.getName() + " checked.");
                hasActed[idx] = true;
                return;
            }
            if (need > bot.getChips() / 6 && roll > aggr) {
                bot.fold();
                pushAction(bot.getName() + " folded.");
                hasActed[idx] = true;
                return;
            }
            int put = bot.bet(need);
            pot += put;
            playerCurrentBets[idx] += put;
            pushAction(bot.getName() + " called " + put + ".");
            hasActed[idx] = true;
            return;
        }

        // MEDIUM HAND
        if (strength < PokerHandEvaluator.THREE_OF_KIND) {
            if (need == 0 || roll > aggr) {
                int put = bot.bet(need);
                pot += put;
                playerCurrentBets[idx] += put;
                pushAction(bot.getName() + (need > 0 ? " called " + put + "." : " checked."));
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

                pushAction(bot.getName() + " raised " + raiseAmt + ".");
                // reset action tracking: only raiser considered acted
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
        pushAction(bot.getName() + (need > 0 ? " called " + put + "." : " checked."));
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
        pushAction(cur.getName() + (need>0 ? " called " + put + "." : " checked."));
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
        pushAction(cur.getName() + " raised " + (need + raiseAmt) + " (raise=" + raiseAmt + ").");

        // reset hasActed for all active players except raiser
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

        // require every active player to have acted and to have matched currentBet (or be all-in)
        for (int i = 0; i < pPlayers.size(); i++) {
            Player p = pPlayers.get(i);
            if (p.isFolded()) continue;
            if (p.getChips() == 0) continue; // all-in players are done
            if (!hasActed[i]) return false;
            // if player still has chips, they must match current bet
            if (playerCurrentBets[i] < currentBet) return false;

        }
        return true;
    }

    private void proceedStageOrShowdown() {
        // reset per-round state (keep player hands)
        for (int i = 0; i < pPlayers.size(); i++) {
            hasActed[i] = pPlayers.get(i).isFolded() || pPlayers.get(i).getChips() == 0;
            playerCurrentBets[i] = 0;
        }
        currentBet = 0;
        lastAggressorIndex = -1;
        raisesThisRound = 0;
        minRaise = BIG_BLIND;

        if (stage == PREFLOP) {
            stage = FLOP;
            dealCommunity(3);
            pushAction("Flop dealt.");

        } else if (stage == FLOP) {
            stage = TURN;
            dealCommunity(1);
            pushAction("Turn dealt.");

        } else if (stage == TURN) {
            stage = RIVER;
            dealCommunity(1);
            pushAction("River dealt.");

        } else if (stage == RIVER) {
            stage = SHOWDOWN;
            endRoundAndPayout();
            return;
        }

        // set first to act after stage: player after dealer
        currentPlayerIndex = (dealerIndex + 1) % pPlayers.size();
        advanceTurn();
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
        int alive = 0; Player last = null;
        for (Player p : pPlayers) {
            if (!p.isFolded() && (p.getChips() > 0 || playerCurrentBets[pPlayers.indexOf(p)] > 0)) { alive++; last = p; }
        }
        if (alive <= 1) {
            if (last != null) { last.win(pot); pushAction(last.getName() + " wins by fold!"); }
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
    }

    // ---------- drawing ----------
    public void drawGame(PApplet g) {
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
        for (int i = 0; i < communityCards.size(); i++) {
            PokerCard c = communityCards.get(i);
            c.setSize((int)cardW, (int)cardH);
            c.setPosition((int)(cx + (i - (communityCards.size()-1)/2.0f) * (cardW + 8)), (int)cy);
            c.setTurned(false);
            c.draw(g);
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

        // buttons & raise UI
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
        if (!roundActive) drawButton(g, btnNewHand);
    }

    private void drawButton(PApplet g, Button b) {
        g.fill(200);
        g.rect(b.x, b.y, b.w, b.h, 8);
        g.fill(0);
        g.textAlign(PApplet.CENTER, PApplet.CENTER);
        g.text(b.label, b.x + b.w/2, b.y + b.h/2);
    }

    // ---------- input ----------
    public void handleMouse(int mx, int my) {
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
        if (btnFold.contains(mx,my)) { cur.fold(); pushAction(cur.getName() + " folded."); hasActed[currentPlayerIndex] = true; if (isBettingRoundComplete()) proceedStageOrShowdown(); else advanceTurn(); return; }
    }

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
}
