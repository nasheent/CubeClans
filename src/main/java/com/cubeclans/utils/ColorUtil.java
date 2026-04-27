package com.cubeclans.utils;

import org.bukkit.ChatColor;

import java.util.ArrayList;
import java.util.List;

public class ColorUtil {

    /**
     * Translates color codes in a string
     * Supports both & and § color codes
     * Supports hex colors in format &#RRGGBB
     */
    public static String translate(String text) {
        if (text == null) {
            return "";
        }
        
        // Support for hex colors (1.16+)
        text = translateHexColorCodes(text);
        
        // Standard color codes
        return ChatColor.translateAlternateColorCodes('&', text);
    }

    /**
     * Translates a list of strings
     */
    public static List<String> translate(List<String> lines) {
        List<String> translated = new ArrayList<>();
        for (String line : lines) {
            translated.add(translate(line));
        }
        return translated;
    }

    /**
     * Strips all color codes from a string
     */
    public static String strip(String text) {
        if (text == null) {
            return "";
        }
        return ChatColor.stripColor(translate(text));
    }

    /**
     * Translates hex color codes
     * Format: &#RRGGBB
     */
    private static String translateHexColorCodes(String message) {
        char colorChar = ChatColor.COLOR_CHAR;
        
        StringBuffer buffer = new StringBuffer();
        java.util.regex.Matcher matcher = java.util.regex.Pattern.compile("&#([A-Fa-f0-9]{6})").matcher(message);
        
        while (matcher.find()) {
            String hexCode = matcher.group(1);
            matcher.appendReplacement(buffer, colorChar + "x"
                    + colorChar + hexCode.charAt(0) + colorChar + hexCode.charAt(1)
                    + colorChar + hexCode.charAt(2) + colorChar + hexCode.charAt(3)
                    + colorChar + hexCode.charAt(4) + colorChar + hexCode.charAt(5));
        }
        
        return matcher.appendTail(buffer).toString();
    }
}
