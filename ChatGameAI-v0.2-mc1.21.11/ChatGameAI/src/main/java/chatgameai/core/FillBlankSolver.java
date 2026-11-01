package chatgameai.core;

import java.util.*;
import java.util.regex.*;

public final class FillBlankSolver implements Solver {
    private static final Pattern TRIGGER = Pattern.compile("(?i)complete|fill|missing|finish|guess the word|blank|hangman");
    private static final Pattern TOKEN = Pattern.compile("(?<![A-Za-z0-9_*])[A-Za-z_*]{3,}(?![A-Za-z0-9_*])");
    private final WordList words;

    private final Config cfg;

    public FillBlankSolver(WordList words, Config cfg) { this.words = words; this.cfg = cfg; }

    public String name() { return "fillblank"; }

    public List<Candidate> solve(String text) {
        if (!TRIGGER.matcher(text).find()) return List.of();
        List<Candidate> out = new ArrayList<>();
        Matcher m = TOKEN.matcher(text);
        while (m.find()) {
            String t = m.group();
            if (!t.matches(".*[_*].*") || !t.matches(".*[A-Za-z].*")) continue;
            List<String> hits = words.matchPattern(t);
            if (hits.size() == 1) out.add(new Candidate(CaseRule.apply(hits.get(0), text, t.replace("_", "").replace("*", "").isEmpty() ? null : (t.equals(t.toUpperCase(java.util.Locale.ROOT)) ? t : null), cfg.scrambleCase), 0.93, name()));
            else if (hits.size() <= 3 && !hits.isEmpty()) out.add(new Candidate(CaseRule.apply(hits.get(0), text, t.equals(t.toUpperCase(java.util.Locale.ROOT)) ? t : null, cfg.scrambleCase), 0.6, name()));
        }
        return out;
    }
}
