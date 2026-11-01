package chatgameai.core;

import java.util.*;
import java.util.regex.*;

public final class ReverseSolver implements Solver {
    private static final Pattern QUOTED = Pattern.compile("[\"\u201C'`]([^\"\u201D'`]{1,64})[\"\u201D'`]");
    private static final Pattern COLON = Pattern.compile("[:\uFF1A]\\s*(\\S+)\\s*$");

    public String name() { return "reverse"; }

    public List<Candidate> solve(String text) {
        if (!text.toLowerCase(Locale.ROOT).contains("reverse")) return List.of();
        Matcher m = QUOTED.matcher(text);
        if (m.find()) return List.of(rev(m.group(1).trim(), 0.95));
        m = COLON.matcher(text);
        if (m.find()) return List.of(rev(m.group(1), 0.9));
        return List.of();
    }

    private Candidate rev(String s, double conf) {
        return new Candidate(new StringBuilder(s).reverse().toString(), conf, name());
    }
}
