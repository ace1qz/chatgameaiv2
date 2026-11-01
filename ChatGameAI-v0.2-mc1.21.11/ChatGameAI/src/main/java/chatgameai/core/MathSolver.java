package chatgameai.core;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.regex.*;

public final class MathSolver implements Solver {
    private static final Pattern CUE = Pattern.compile("(?i)solve|calculate|compute|math|what is|what's|equals|=|answer|\\?");
    private static final Pattern EXPR = Pattern.compile("[0-9(][0-9+\\-*/^%().\\s]*[0-9)]");
    private static final Pattern DATE = Pattern.compile("\\d{4}-\\d{2}-\\d{2}");

    public String name() { return "math"; }

    public List<Candidate> solve(String text) {
        if (text.matches("(?is).*\\bguess\\b.*\\bnumber\\b.*")) return List.of(); // handled by GuessNumberSolver
        String t = normalize(text);
        boolean cue = CUE.matcher(text).find();
        List<Candidate> out = new ArrayList<>();
        Matcher m = EXPR.matcher(t);
        while (m.find()) {
            String e = m.group().trim();
            if (!e.matches(".*[+\\-*/^%].*") || DATE.matcher(e).find()) continue;
            try {
                double v = new P(e).parse();
                if (Double.isNaN(v) || Double.isInfinite(v)) continue;
                boolean integral = Math.abs(v - Math.rint(v)) < 1e-9;
                String ans = integral ? Long.toString((long) Math.rint(v))
                        : BigDecimal.valueOf(v).setScale(2, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString();
                double conf = cue ? (integral ? 0.97 : 0.8) : 0.55;
                out.add(new Candidate(ans, conf, name()));
            } catch (RuntimeException ignored) {}
        }
        return out;
    }

    static String normalize(String s) {
        String t = s.replace('\u00D7', '*').replace('\u00F7', '/').replace('\u2212', '-')
                .replace('\u2013', '-').replace('\u00B7', '*');
        t = t.replaceAll("(?i)\\bplus\\b", "+").replaceAll("(?i)\\bminus\\b", "-")
             .replaceAll("(?i)\\b(times|multiplied by)\\b", "*").replaceAll("(?i)\\bdivided by\\b", "/");
        return t.replaceAll("(?<=\\d)\\s*[xX]\\s*(?=\\d)", "*");
    }

    private static final class P {
        final String s; int i = 0;
        P(String s) { this.s = s.replaceAll("\\s+", ""); }
        double parse() { double v = expr(); if (i != s.length()) throw new IllegalArgumentException(); return v; }
        boolean at(char c) { return i < s.length() && s.charAt(i) == c; }
        double expr() {
            double v = term();
            while (at('+') || at('-')) { char c = s.charAt(i++); double r = term(); v = c == '+' ? v + r : v - r; }
            return v;
        }
        double term() {
            double v = unary();
            while (at('*') || at('/') || at('%')) {
                char c = s.charAt(i++); double r = unary();
                v = c == '*' ? v * r : c == '/' ? v / r : v % r;
            }
            return v;
        }
        double unary() {
            if (at('-')) { i++; return -unary(); }
            if (at('+')) { i++; return unary(); }
            return pow();
        }
        double pow() {
            double b = prim();
            if (at('^')) { i++; return Math.pow(b, unary()); }
            return b;
        }
        double prim() {
            if (at('(')) {
                i++; double v = expr();
                if (!at(')')) throw new IllegalArgumentException();
                i++; return v;
            }
            int st = i;
            while (i < s.length() && (Character.isDigit(s.charAt(i)) || s.charAt(i) == '.')) i++;
            if (st == i) throw new IllegalArgumentException();
            return Double.parseDouble(s.substring(st, i));
        }
    }
}
