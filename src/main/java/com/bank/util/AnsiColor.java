package com.bank.util;

/**
 * ANSI Escape Codes for CLI styling and terminal coloring.
 */
public final class AnsiColor {

    public static final String RESET = "\u001B[0m";
    public static final String BOLD = "\u001B[1m";
    public static final String DIM = "\u001B[2m";
    public static final String ITALIC = "\u001B[3m";
    public static final String UNDERLINE = "\u001B[4m";

    // Text colors
    public static final String BLACK = "\u001B[30m";
    public static final String RED = "\u001B[31m";
    public static final String GREEN = "\u001B[32m";
    public static final String YELLOW = "\u001B[33m";
    public static final String BLUE = "\u001B[34m";
    public static final String MAGENTA = "\u001B[35m";
    public static final String CYAN = "\u001B[36m";
    public static final String WHITE = "\u001B[37m";

    // Bright text colors
    public static final String BRIGHT_RED = "\u001B[91m";
    public static final String BRIGHT_GREEN = "\u001B[92m";
    public static final String BRIGHT_YELLOW = "\u001B[93m";
    public static final String BRIGHT_BLUE = "\u001B[94m";
    public static final String BRIGHT_MAGENTA = "\u001B[95m";
    public static final String BRIGHT_CYAN = "\u001B[96m";
    public static final String BRIGHT_WHITE = "\u001B[97m";

    // Background colors
    public static final String BG_BLUE = "\u001B[44m";
    public static final String BG_GREEN = "\u001B[42m";
    public static final String BG_YELLOW = "\u001B[43m";
    public static final String BG_RED = "\u001B[41m";

    private static boolean enabled = true;

    private AnsiColor() {}

    public static void setEnabled(boolean enable) {
        enabled = enable;
    }

    public static boolean isEnabled() {
        return enabled;
    }

    public static String colorize(String color, String text) {
        if (!enabled || color == null) {
            return text;
        }
        return color + text + RESET;
    }

    public static String bold(String text) {
        return colorize(BOLD, text);
    }

    public static String dim(String text) {
        return colorize(DIM, text);
    }

    public static String cyan(String text) {
        return colorize(CYAN, text);
    }

    public static String green(String text) {
        return colorize(GREEN, text);
    }

    public static String yellow(String text) {
        return colorize(YELLOW, text);
    }

    public static String red(String text) {
        return colorize(RED, text);
    }

    public static String magenta(String text) {
        return colorize(MAGENTA, text);
    }

    public static String brightCyan(String text) {
        return colorize(BRIGHT_CYAN, text);
    }
}
