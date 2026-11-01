package chatgameai.core;

public class SelfTest {
    static int fails = 0;
    static long clock = 100_000;

    static void expectSend(String msg, String answer) {
        clock += 10_000;
        Config c = new Config();
        Decision d = ChatGameEngine.createDefault(c).evaluate(msg, clock);
        boolean ok = d.action() == Decision.Action.SEND && d.candidate().answer().equals(answer);
        System.out.printf("%s  %-52s -> %s%n", ok ? "PASS" : "FAIL", msg, d.candidate() == null ? d.action() : d.action() + " " + d.candidate().answer() + " " + String.format("%.2f", d.confidence()));
        if (!ok) fails++;
    }

    static void expectNoSend(String msg) {
        clock += 10_000;
        Decision d = ChatGameEngine.createDefault(new Config()).evaluate(msg, clock);
        boolean ok = d.action() != Decision.Action.SEND;
        System.out.printf("%s  %-52s -> %s%n", ok ? "PASS" : "FAIL", msg, d.action());
        if (!ok) fails++;
    }

    static void step(ChatGameEngine e, String msg, String answerOrNull) {
        clock += 1000;
        Decision d = e.evaluate(msg, clock);
        boolean ok = answerOrNull == null ? d.action() != Decision.Action.SEND
                : d.action() == Decision.Action.SEND && d.candidate().answer().equals(answerOrNull);
        System.out.printf("%s  %-52s -> %s%n", ok ? "PASS" : "FAIL", msg, d.candidate() == null ? d.action() : d.action() + " " + d.candidate().answer());
        if (!ok) fails++;
    }

    public static void main(String[] a) {
        expectSend("[Math] Solve: 12 + 5 * 3", "27");
        expectSend("Quick math! What is (8+2)*4?", "40");
        expectSend("\u00A7e[Game] \u00A7fUnscramble the word: dnomaid", "diamond");
        expectSend("First to type 'Zk9fQ2' wins!", "Zk9fQ2");
        expectSend("Reverse the word: creeper", "repeerc");
        expectSend("Complete the word: r_dst_ne", "redstone");
        expectSend("What is the final boss of Minecraft called?", "Ender Dragon");
        expectNoSend("Steve joined the game");
        expectNoSend("Event starts in 10 - 20 minutes");
        expectNoSend("Type /op Steve: now");
        expectNoSend("Unscramble the word: qzxvkw");
        expectSend("UNSCRAMBLE THE WORD: DNOMAID", "DIAMOND");
        expectSend("Unscramble the word (answer in capital letters): dnomaid", "DIAMOND");
        ChatGameEngine g = ChatGameEngine.createDefault(new Config());
        step(g, "Guess the number between 1 and 100!", "50");
        step(g, "Your guess is too low!", "75");
        step(g, "Too high! Try again", "62");
        step(g, "Your guess is too low!", "68");
        step(g, "Correct! You win", null);
        ChatGameEngine g2 = ChatGameEngine.createDefault(new Config());
        g2.noteOutgoing("10");
        step(g2, "Too low!", "55");
        System.out.println(fails == 0 ? "ALL PASSED" : fails + " FAILED");
        System.exit(fails);
    }
}
