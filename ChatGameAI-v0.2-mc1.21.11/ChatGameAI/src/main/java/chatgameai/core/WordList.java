package chatgameai.core;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

/** Local dictionary. Put a bigger frequency-ordered words.txt in the config folder to improve accuracy. */
public final class WordList {
    private final Set<String> all = new HashSet<>();
    private final List<String> ordered = new ArrayList<>();
    private final Map<String, List<String>> byKey = new HashMap<>();

    public static String key(String w) {
        char[] c = w.toLowerCase(Locale.ROOT).toCharArray();
        Arrays.sort(c);
        return new String(c);
    }

    public void add(String line) {
        String s = line.trim().toLowerCase(Locale.ROOT);
        if (s.length() < 3 || !s.matches("[a-z]+") || !all.add(s)) return;
        ordered.add(s);
        byKey.computeIfAbsent(key(s), k -> new ArrayList<>()).add(s);
    }

    public boolean contains(String w) { return all.contains(w.toLowerCase(Locale.ROOT)); }

    public List<String> anagrams(String w) {
        return byKey.getOrDefault(key(w), List.of());
    }

    /** Pattern uses '_' or '*' as a single-letter wildcard. */
    public List<String> matchPattern(String pattern) {
        String p = pattern.toLowerCase(Locale.ROOT);
        List<String> out = new ArrayList<>();
        for (String w : ordered) {
            if (w.length() != p.length()) continue;
            boolean ok = true;
            for (int i = 0; i < p.length() && ok; i++) {
                char c = p.charAt(i);
                if (c != '_' && c != '*' && c != w.charAt(i)) ok = false;
            }
            if (ok) out.add(w);
        }
        return out;
    }

    public static WordList load(Path dir) {
        WordList w = new WordList();
        try {
            Path p = dir == null ? null : dir.resolve("words.txt");
            if (p != null && Files.isRegularFile(p)) {
                for (String l : Files.readAllLines(p, StandardCharsets.UTF_8)) w.add(l);
                return w;
            }
            try (InputStream in = WordList.class.getResourceAsStream("/words.txt")) {
                if (in != null) new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8)).lines().forEach(w::add);
            }
        } catch (IOException ignored) {}
        return w;
    }
}
