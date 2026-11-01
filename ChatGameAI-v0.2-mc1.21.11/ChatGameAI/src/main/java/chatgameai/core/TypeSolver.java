package chatgameai.core;

import java.util.*;
import java.util.regex.*;

/** "Type 'abc' first", "Type the following: abc", "First to type aB3dE9 wins". */
public final class TypeSolver implements Solver {
    private static final String Q_OPEN = "\"\u201C\u2018'`\u00AB";
    private static final Pattern QUOTED = Pattern.compile(
            "(?i)\\b(?:type|say|write|send|enter|retype|copy|chat)\\b[^\"\u201C\u201D\u2018\u2019'`\u00AB\u00BB]*[" + Q_OPEN + "]([^\"\u201C\u201D\u2018\u2019'`\u00AB\u00BB]{1,64})[\"\u201D\u2019'`\u00BB]");
    private static final Pattern COLON = Pattern.compile(
            "(?i)\\b(?:type|say|write|send|enter|retype|copy)\\b[^:\uFF1A]*[:\uFF1A]\\s*(\\S.{0,63})$");
    private static final Pattern CODE = Pattern.compile(
            "(?i)\\b(?:type|say|write)\\b\\s+(?:out\\s+)?(?:the\\s+(?:word|code|phrase|following)\\s+)?([A-Za-z0-9]{4,})\\b");
    private static final Pattern ANY_QUOTE = Pattern.compile("[\"\u201C`]([^\"\u201D`]{2,32})[\"\u201D`]");
    private static final Pattern RACE = Pattern.compile("(?i)first|fastest|win|answer");

    public String name() { return "type"; }

    private static final Pattern SLASH_CMD = Pattern.compile("(^|\\s)/\\w+");

    public List<Candidate> solve(String text) {
        List<Candidate> out = new ArrayList<>();
        // Prompts that mention slash commands are never auto-answered.
        if (SLASH_CMD.matcher(text).find()) return out;
        Matcher m = QUOTED.matcher(text);
        if (m.find()) out.add(new Candidate(m.group(1).trim(), 0.95, name()));
        m = COLON.matcher(text);
        if (m.find()) {
            String a = strip(m.group(1));
            if (!a.isEmpty()) out.add(new Candidate(a, a.contains(" ") ? 0.78 : 0.9, name()));
        }
        m = CODE.matcher(text);
        if (m.find()) {
            String c = m.group(1);
            boolean digitAndLetter = c.matches(".*\\d.*") && c.matches(".*[A-Za-z].*");
            boolean mixedCase = c.substring(1).matches(".*[A-Z].*") && c.matches(".*[a-z].*");
            if (digitAndLetter || mixedCase) out.add(new Candidate(c, 0.88, name()));
        }
        if (out.isEmpty() && RACE.matcher(text).find()) {
            m = ANY_QUOTE.matcher(text);
            if (m.find()) out.add(new Candidate(m.group(1).trim(), 0.5, name()));
        }
        return out;
    }

    private static String strip(String s) {
        return s.trim().replaceAll("^[\"\u201C'`\u2018]+|[\"\u201D'`\u2019]+$", "").trim();
    }
}
