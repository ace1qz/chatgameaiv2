package chatgameai.core;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.*;
import java.util.*;

public final class Config {
    public boolean enabled = true;
    public boolean listenToPlayerChat = false;
    public boolean suggestBelowThreshold = true;
    public double threshold = 0.85;
    public double suggestFloor = 0.5;
    public int minDelayMs = 900;
    public int maxDelayMs = 2200;
    public int cooldownMs = 3000;
    public int guessMin = 1;
    public int guessMax = 100;
    /** auto = uppercase when the puzzle is shown in caps or mentions capitals; always; never. */
    public String scrambleCase = "auto";
    /** Empty = all servers. Otherwise only hosts listed here (lowercase). */
    public Set<String> serverAllowlist = new HashSet<>();
    public Path dataDir;

    public boolean allows(String host) {
        return serverAllowlist.isEmpty() || (host != null && serverAllowlist.contains(host.toLowerCase(Locale.ROOT)));
    }

    public static Config load(Path file, Path dataDir) {
        Config c = new Config();
        c.dataDir = dataDir;
        Properties p = new Properties();
        if (file != null && Files.isRegularFile(file)) {
            try (InputStream in = Files.newInputStream(file)) { p.load(in); } catch (IOException ignored) {}
        }
        c.enabled = Boolean.parseBoolean(p.getProperty("enabled", "true"));
        c.listenToPlayerChat = Boolean.parseBoolean(p.getProperty("listenToPlayerChat", "false"));
        c.suggestBelowThreshold = Boolean.parseBoolean(p.getProperty("suggestBelowThreshold", "true"));
        c.threshold = parseD(p.getProperty("threshold"), c.threshold);
        c.suggestFloor = parseD(p.getProperty("suggestFloor"), c.suggestFloor);
        c.minDelayMs = (int) parseD(p.getProperty("minDelayMs"), c.minDelayMs);
        c.maxDelayMs = Math.max(c.minDelayMs, (int) parseD(p.getProperty("maxDelayMs"), c.maxDelayMs));
        c.cooldownMs = (int) parseD(p.getProperty("cooldownMs"), c.cooldownMs);
        c.guessMin = (int) parseD(p.getProperty("guessMin"), c.guessMin);
        c.guessMax = (int) parseD(p.getProperty("guessMax"), c.guessMax);
        c.scrambleCase = p.getProperty("scrambleCase", "auto").trim().toLowerCase(Locale.ROOT);
        for (String h : p.getProperty("serverAllowlist", "").split(",")) {
            if (!h.isBlank()) c.serverAllowlist.add(h.trim().toLowerCase(Locale.ROOT));
        }
        return c;
    }

    private static double parseD(String s, double def) {
        try { return s == null ? def : Double.parseDouble(s.trim()); } catch (NumberFormatException e) { return def; }
    }
}
