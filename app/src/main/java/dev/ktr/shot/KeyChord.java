package dev.ktr.shot;

import java.util.HashSet;
import java.util.Set;

public final class KeyChord {
    private final int first;
    private final int second;
    private final Set<Integer> pressed = new HashSet<>();
    private boolean latched;

    public KeyChord(int first, int second) {
        if (first <= 0 || second < 0 || first == second) {
            throw new IllegalArgumentException("Invalid key binding");
        }
        this.first = first;
        this.second = second;
    }

    public boolean accept(int code, int value) {
        if (code != first && code != second) return false;
        if (value == 0) {
            pressed.remove(code);
            if (pressed.isEmpty()) latched = false;
            return false;
        }
        if (value != 1 || !pressed.add(code)) return false;
        if (!latched && pressed.contains(first)
                && (second == 0 || pressed.contains(second))) {
            latched = true;
            return true;
        }
        return false;
    }

    public void reset() {
        pressed.clear();
        latched = false;
    }
}