package chatgameai.core;

import java.util.*;
import java.util.regex.*;

/**
 * Guess-the-number games with "too low / too high" hints. Keeps a [lo, hi] range and binary-searches.
 * It learns your last guess from what it sends and from numbers you type yourself (noteOutgoing).
 */
public final class GuessNumberSolver implements Solver {
    private static final Pattern TOO_LOW = Pattern.compile(
            "(?i)too (?:low|small|little)|\\b(?:higher|bigger|larger|greater)\\b|\\bgo up\\b");
    private static final Pattern TOO_HIGH = Pattern.compile(
            "(?i)too (?:high|big|large)|\\b(?:lower|smaller)\\b|\\bgo down\\b");
    private static final Pattern CORRECT = Pattern.compile(
            "(?i)\\b(?:correct|congrat\\w*|you (?:got|won|win)|right answer|guessed it)\\b");
    private static final Pattern PROMPT = Pattern.compile(
            "(?i)(?:guess|thinking of|pick(?:ed)?|choose|chosen).{0,40}number|number.{0,40}\\b(?:guess|between)\\b");
    private static final Pattern RANGE = Pattern.compile(
            "(\\d{1,9})\\s*(?:and|to|-|\u2013|\u2014)\\s*(\\d{1,9})", Pattern.CASE_INSENSITIVE);

    private final Config cfg;
    private int lo, hi;
    private Integer lastGuess;

    public GuessNumberSolver(Config cfg) {
        this.cfg = cfg;
        resetBounds();
    }

    public String name() { return "guessnumber"; }

    private void resetBounds() { lo = cfg.guessMin; hi = cfg.guessMax; }

    private int mid() { return lo + (hi - lo) / 2; }

    public synchronized boolean hasPendingGuess() { return lastGuess != null; }

    /** Call with every chat message the player sends (including our own answers). */
    public synchronized void noteOutgoing(String msg) {
        if (msg != null && msg.trim().matches("\\d{1,9}")) lastGuess = Integer.parseInt(msg.trim());
    }

    public synchronized void commit(String answer) { noteOutgoing(answer); }

    public synchronized List<Candidate> solve(String text) {
        boolean low = TOO_LOW.matcher(text).find();
        boolean high = TOO_HIGH.matcher(text).find();
        if (low && high) return List.of();
        if (low || high) {
            if (lastGuess == null) return List.of();
            int g = lastGuess;
            lastGuess = null;
            if (low) lo = Math.max(lo, g + 1); else hi = Math.min(hi, g - 1);
            if (lo > hi) { resetBounds(); return List.of(); } // hints disagreed with our bounds: start over
            return List.of(new Candidate(Integer.toString(mid()), 0.92, name()));
        }
        if (CORRECT.matcher(text).find()) {
            resetBounds();
            lastGuess = null;
            return List.of();
        }
        if (PROMPT.matcher(text).find()) {
            lastGuess = null;
            Matcher m = RANGE.matcher(text);
            if (m.find()) {
                int a = Integer.parseInt(m.group(1)), b = Integer.parseInt(m.group(2));
                lo = Math.min(a, b);
                hi = Math.max(a, b);
                return List.of(new Candidate(Integer.toString(mid()), 0.9, name()));
            }
            resetBounds();
            return List.of(new Candidate(Integer.toString(mid()), 0.6, name())); // range unknown: suggest only
        }
        return List.of();
    }
}
