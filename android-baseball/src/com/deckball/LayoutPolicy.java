package com.deckball;

public final class LayoutPolicy {
    private LayoutPolicy() {}
    public static int railWidth(int widthDp) { return Math.min(260,Math.max(150,widthDp/3)); }
}
