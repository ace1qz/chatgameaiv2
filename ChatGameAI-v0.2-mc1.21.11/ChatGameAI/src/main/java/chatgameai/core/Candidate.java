package chatgameai.core;

/** One proposed answer from one solver. */
public record Candidate(String answer, double confidence, String solver) {}
