package chatgameai.core;

import java.util.*;

/** Runs every solver, resolves conflicts, and gates the result by confidence. No Minecraft dependencies. */
public final class ChatGameEngine {
    private final List<Solver> solvers;
    private final Config cfg;
    private final GuessNumberSolver guess;
    private long lastSentAt = 0;
    private String lastText = "";
    private long lastTextAt = 0;

    public ChatGameEngine(Config cfg, List<Solver> solvers, GuessNumberSolver guess) {
        this.cfg = cfg;
        this.solvers = solvers;
        this.guess = guess;
    }

    public static ChatGameEngine createDefault(Config cfg) {
        WordList w = WordList.load(cfg.dataDir);
        GuessNumberSolver g = new GuessNumberSolver(cfg);
        return new ChatGameEngine(cfg, List.of(
                new TypeSolver(), new MathSolver(), new ReverseSolver(),
                new UnscrambleSolver(w, cfg), new FillBlankSolver(w, cfg), new TriviaSolver(cfg.dataDir), g), g);
    }

    /** Feed this every chat message the player sends, so manual number guesses are tracked too. */
    public void noteOutgoing(String msg) { guess.noteOutgoing(msg); }

    public Decision evaluate(String raw) { return evaluate(raw, System.currentTimeMillis()); }

    public Decision evaluate(String raw, long now) {
        String text = TextCleaner.clean(raw);
        if (text.isEmpty() || text.length() > 400) return Decision.IGNORE;
        // Identical hints ("Too low!") legitimately repeat while a guess game is running.
        if (!guess.hasPendingGuess() && text.equals(lastText) && now - lastTextAt < 5000) return Decision.IGNORE;
        lastText = text;
        lastTextAt = now;

        List<Candidate> all = new ArrayList<>();
        for (Solver s : solvers) {
            try { all.addAll(s.solve(text)); } catch (RuntimeException ignored) {}
        }
        all.removeIf(c -> !valid(c.answer()));
        if (all.isEmpty()) return Decision.IGNORE;
        all.sort(Comparator.comparingDouble(Candidate::confidence).reversed());

        Candidate best = all.get(0);
        double conf = best.confidence();
        for (int i = 1; i < all.size(); i++) {
            Candidate o = all.get(i);
            if (o.answer().equalsIgnoreCase(best.answer())) {
                if (!o.solver().equals(best.solver())) conf = Math.min(0.99, conf + 0.03); // independent agreement
            } else if (best.confidence() - o.confidence() < 0.1) {
                conf -= 0.15; // competing answers: don't gamble
            }
        }

        boolean stateful = best.solver().equals("guessnumber"); // paced by the server's replies, so no cooldown
        if (conf >= cfg.threshold && (stateful || now - lastSentAt >= cfg.cooldownMs)) {
            lastSentAt = now;
            if (stateful) guess.commit(best.answer());
            return new Decision(Decision.Action.SEND, best, conf);
        }
        if (cfg.suggestBelowThreshold && conf >= cfg.suggestFloor)
            return new Decision(Decision.Action.SUGGEST, best, conf);
        return Decision.IGNORE;
    }

    private static boolean valid(String a) {
        return a != null && !a.isBlank() && a.length() <= 100 && !a.startsWith("/");
    }
}
