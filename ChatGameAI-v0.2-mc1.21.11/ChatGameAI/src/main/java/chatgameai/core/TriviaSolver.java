package chatgameai.core;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

/** Local question -> answer lookup. Add your own lines to config/chatgameai/trivia.tsv (question TAB answer). */
public final class TriviaSolver implements Solver {
    private static final Set<String> STOP = Set.of("the", "a", "an", "of", "is", "what", "which", "who", "in", "to", "and", "for", "are", "was", "does", "do");
    private record Entry(Set<String> tokens, String answer) {}
    private final List<Entry> entries = new ArrayList<>();

    public TriviaSolver(Path dir) {
        try {
            Path p = dir == null ? null : dir.resolve("trivia.tsv");
            try (InputStream in = TriviaSolver.class.getResourceAsStream("/trivia.tsv")) {
                if (in != null) new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8)).lines().forEach(this::addLine);
            }
            if (p != null && Files.isRegularFile(p)) Files.readAllLines(p, StandardCharsets.UTF_8).forEach(this::addLine);
        } catch (IOException ignored) {}
    }

    private void addLine(String line) {
        String[] parts = line.split("\t", 2);
        if (parts.length < 2 || parts[0].startsWith("#")) return;
        Set<String> t = tokens(parts[0]);
        if (t.size() >= 3) entries.add(new Entry(t, parts[1].trim()));
    }

    private static Set<String> tokens(String s) {
        Set<String> out = new HashSet<>();
        for (String w : s.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9 ]", " ").split("\\s+"))
            if (!w.isEmpty() && !STOP.contains(w)) out.add(w);
        return out;
    }

    public String name() { return "trivia"; }

    public List<Candidate> solve(String text) {
        Set<String> msg = tokens(text);
        List<Candidate> out = new ArrayList<>();
        for (Entry e : entries) {
            int hit = 0;
            for (String t : e.tokens()) if (msg.contains(t)) hit++;
            double containment = hit / (double) e.tokens().size();
            if (containment >= 0.85) out.add(new Candidate(e.answer(), 0.5 + 0.46 * containment, name()));
        }
        return out;
    }
}
