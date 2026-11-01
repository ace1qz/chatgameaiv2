package chatgameai.core;

import java.util.Locale;
import java.util.regex.Pattern;

/** Decides whether an answer must be sent in ALL CAPS. */
public final class CaseRule {
    private static final Pattern CAPS_WORDS = Pattern.compile(
            "(?i)\\b(capital(s|ized|ised)?|upper-?\\s?case|all caps|in caps|caps)\\b");

    private CaseRule() {}

    /** mode: "auto" (default), "always", or "never". token = the puzzle text as shown by the server. */
    public static String apply(String answer, String text, String token, String mode) {
        return wantsUpper(text, token, mode) ? answer.toUpperCase(Locale.ROOT) : answer;
    }

    static boolean wantsUpper(String text, String token, String mode) {
        if ("always".equalsIgnoreCase(mode)) return true;
        if ("never".equalsIgnoreCase(mode)) return false;
        if (CAPS_WORDS.matcher(text).find()) return true;
        return token != null && token.length() >= 3
                && token.equals(token.toUpperCase(Locale.ROOT))
                && token.chars().anyMatch(Character::isLetter);
    }
}
