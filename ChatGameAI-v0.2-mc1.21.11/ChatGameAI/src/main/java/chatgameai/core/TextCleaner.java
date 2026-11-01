package chatgameai.core;

public final class TextCleaner {
    private TextCleaner() {}

    public static String clean(String s) {
        if (s == null) return "";
        return s.replaceAll("\u00A7.", "")
                .replaceAll("[\\u200B-\\u200F\\uFEFF]", "")
                .replaceAll("\\s+", " ")
                .trim();
    }
}
