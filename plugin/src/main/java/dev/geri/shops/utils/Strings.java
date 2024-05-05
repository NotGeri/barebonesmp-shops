package dev.geri.shops.utils;

import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Strings {

    /**
     * Parse a set of placeholders in a String
     *
     * @param text         The text to parse
     * @param placeholders The placeholders' names and their values
     * @return The parsed String
     */
    public static String placeholders(String text, Map<String, Object> placeholders) {
        if (text == null) return text;
        for (String placeholder : placeholders.keySet()) {
            if (text.contains(placeholder)) {
                text = text.replaceAll(placeholder, escapeRegex(String.valueOf(placeholders.get(placeholder))));
            }
        }
        return text;
    }

    private static String escapeRegex(String s) {
        return Pattern.compile("[{}()\\[\\].+*?^$\\\\|]").matcher(s).replaceAll("\\\\$0");
    }

    /**
     * Capitalise all the words of a string
     */
    public static String capitalise(final String words) {
        return Stream.of(words.trim().split("\\s"))
                .filter(word -> !word.isEmpty())
                .map(word -> word.substring(0, 1).toUpperCase() + word.substring(1))
                .collect(Collectors.joining(" "));
    }

}
