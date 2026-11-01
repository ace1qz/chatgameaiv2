package chatgameai.core;

public record Decision(Action action, Candidate candidate, double confidence) {
    public enum Action { SEND, SUGGEST, IGNORE }
    public static final Decision IGNORE = new Decision(Action.IGNORE, null, 0);
}
