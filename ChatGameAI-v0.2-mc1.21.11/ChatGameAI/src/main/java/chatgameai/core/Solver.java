package chatgameai.core;

import java.util.List;

/** A solver inspects a cleaned chat line and proposes zero or more answers. */
public interface Solver {
    String name();
    List<Candidate> solve(String text);
}
