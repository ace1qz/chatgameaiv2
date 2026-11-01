package chatgameai.core;

import java.util.*;
import java.util.regex.*;

public final class UnscrambleSolver implements Solver {
    private static final Pattern TRIGGER = Pattern.compile(
            "(?i)unscramble|descramble|unjumble|rearrange|scrambled|jumbled|re-?order|decode the word");
    private static final Pattern WORD = Pattern.compile("[A-Za-z]{3,}");
    private static final Pattern AFTER_COLON = Pattern.compile("[:\uFF1A]\\s*[\"'`]?([A-Za-z]{3,})[\"'`]?\\s*$");
    private final WordList words;
    private final Config cfg;

    public UnscrambleSolver(WordList words, Config cfg) { this.words = words; this.cfg = cfg; }

    public String name() { return "unscramble"; }

    public List<Candidate> solve(String text) {
        if (!TRIGGER.matcher(text).find()) return List.of();
        List<Candidate> out = new ArrayList<>();
        Matcher m = WORD.matcher(text);
        while (m.find()) {
            String shown = m.group();
            String tok = shown.toLowerCase(Locale.ROOT);
            if (words.contains(tok)) continue; // instruction words ("the", "word") are real words
            List<String> hits = words.anagrams(tok);
            if (!hits.isEmpty()) out.add(new Candidate(
                    CaseRule.apply(hits.get(0), text, shown, cfg.scrambleCase), hits.size() == 1 ? 0.96 : 0.7, name()));
        }
        if (out.isEmpty()) { // the scrambled string may itself be a valid word
            Matcher c = AFTER_COLON.matcher(text);
            if (c.find()) {
                String shown = c.group(1);
                String tok = shown.toLowerCase(Locale.ROOT);
                List<String> hits = new ArrayList<>(words.anagrams(tok));
                hits.remove(tok);
                if (!hits.isEmpty()) out.add(new Candidate(
                        CaseRule.apply(hits.get(0), text, shown, cfg.scrambleCase), hits.size() == 1 ? 0.75 : 0.55, name()));
            }
        }
        return out;
    }
}
