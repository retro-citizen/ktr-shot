package dev.ktr.shot;

public final class CoreTest {
    private static int checks;

    private static void check(boolean condition, String message) {
        checks++;
        if (!condition) throw new AssertionError(message);
    }

    public static void main(String[] args) {
        KeyChord chord = new KeyChord(139, 172);
        check(!chord.accept(139, 1), "MENU alone");
        check(chord.accept(172, 1), "MENU + FUNC");
        check(!chord.accept(172, 2), "No repeat");
        check(!chord.accept(172, 1), "No duplicate down");
        chord.accept(172, 0);
        check(!chord.accept(172, 1), "Must release both keys");
        chord.accept(139, 0);
        chord.accept(172, 0);
        check(!chord.accept(172, 1), "Reverse order starts");
        check(chord.accept(139, 1), "Reverse order fires");
        chord.reset();
        check(!chord.accept(139, 1), "Reset clears held keys");
        check(!chord.accept(314, 1), "MENU + SELECT ignored");
        chord.accept(139, 0);
        check(!chord.accept(172, 1), "Sequential taps are not a chord");
        KeyChord single = new KeyChord(172, 0);
        check(single.accept(172, 1), "Single key");
        check(!single.accept(172, 2), "Single repeat suppressed");
        single.accept(172, 0);
        check(single.accept(172, 1), "Single rearmed");
        KeyChord menuSelect = new KeyChord(139, 314);
        check(!menuSelect.accept(139, 1), "MENU alone does not capture");
        check(!menuSelect.accept(310, 1), "MENU+L1 not a capture binding");
        check(menuSelect.accept(314, 1), "MENU+SELECT captures");
        check(!menuSelect.accept(314, 2), "MENU+SELECT held does not repeat");
        menuSelect.accept(139, 0);
        menuSelect.accept(314, 0);
        check(!menuSelect.accept(314, 1), "SELECT alone does not capture");
        check(menuSelect.accept(139, 1), "SELECT+MENU captures in reverse order");
        System.out.println("PASS: " + checks + " core checks");
    }
}