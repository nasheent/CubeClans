package com.cubeclans.utils;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

/**
 * Helper class for version compatibility
 * Handles differences between Minecraft versions (1.8.9 vs 1.13+)
 */
public class VersionHelper {

    private static final String SERVER_VERSION = getServerVersion();
    private static final int MAJOR_VERSION = extractMajorVersion();
    private static final int MINOR_VERSION = extractMinorVersion();

    /**
     * Get server version string safely (handles modern Paper/Spigot package changes)
     */
    private static String getServerVersion() {
        try {
            String packageName = Bukkit.getServer().getClass().getPackage().getName();
            String[] parts = packageName.split("\\.");
            // Old format: org.bukkit.craftbukkit.v1_8_R3 (length 4)
            // New format: org.bukkit.craftbukkit (length 3 in Paper 1.20.5+)
            if (parts.length >= 4) {
                return parts[3]; // v1_8_R3, v1_20_R4, etc.
            }
            // Fallback: use Bukkit version (e.g., "1.21.8-R0.1-SNAPSHOT")
            String bukkitVersion = Bukkit.getBukkitVersion();
            String[] versionParts = bukkitVersion.split("-")[0].split("\\.");
            if (versionParts.length >= 2) {
                return "v" + versionParts[0] + "_" + versionParts[1] + "_R0";
            }
            return "v1_20_R0"; // Safe default
        } catch (Exception e) {
            return "v1_20_R0";
        }
    }

    /**
     * Extract major version number (e.g., 1 from "v1_8_R3")
     */
    private static int extractMajorVersion() {
        try {
            String version = SERVER_VERSION.substring(1); // Remove 'v'
            return Integer.parseInt(version.split("_")[0]);
        } catch (Exception e) {
            return 1;
        }
    }

    /**
     * Extract minor version number (e.g., 8 from "v1_8_R3")
     */
    private static int extractMinorVersion() {
        try {
            String version = SERVER_VERSION.substring(1); // Remove 'v'
            return Integer.parseInt(version.split("_")[1]);
        } catch (Exception e) {
            // Fallback: parse from Bukkit version
            try {
                String bukkitVersion = Bukkit.getBukkitVersion();
                String[] parts = bukkitVersion.split("-")[0].split("\\.");
                if (parts.length >= 2) {
                    return Integer.parseInt(parts[1]);
                }
            } catch (Exception ignored) {}
            return 20; // Default to 1.20+
        }
    }

    /**
     * Check if server is running 1.8.x
     */
    public static boolean is1_8() {
        return MAJOR_VERSION == 1 && MINOR_VERSION == 8;
    }

    /**
     * Check if server is running 1.13 or higher
     */
    public static boolean is1_13OrHigher() {
        return MAJOR_VERSION > 1 || (MAJOR_VERSION == 1 && MINOR_VERSION >= 13);
    }

    /**
     * Check if NamespacedKey is available (1.13+)
     * PDC (Persistent Data Container) requires NamespacedKey
     */
    public static boolean hasNamespacedKey() {
        try {
            Class.forName("org.bukkit.NamespacedKey");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    /**
     * Get material with fallback for version compatibility
     * Handles special cases like GRAY_STAINED_GLASS_PANE
     */
    public static Material getMaterial(String materialName) {
        if (materialName == null || materialName.isEmpty()) {
            return Material.STONE;
        }

        // Handle GRAY_STAINED_GLASS_PANE for 1.8.9
        if (materialName.equals("GRAY_STAINED_GLASS_PANE") && is1_8()) {
            try {
                return Material.valueOf("STAINED_GLASS_PANE");
            } catch (IllegalArgumentException e) {
                return Material.valueOf("GLASS");
            }
        }

        // Handle other color variants for 1.8.9
        if (is1_8() && materialName.contains("STAINED_GLASS_PANE")) {
            try {
                return Material.valueOf("STAINED_GLASS_PANE");
            } catch (IllegalArgumentException e) {
                return Material.valueOf("GLASS");
            }
        }

        // Try direct lookup for modern versions
        try {
            return Material.valueOf(materialName);
        } catch (IllegalArgumentException e) {
            // Fallback
            return Material.STONE;
        }
    }

    /**
     * Create ItemStack with proper data value for version compatibility
     * In 1.8.9, use data value; in 1.13+, data is in the material name
     */
    public static ItemStack createMaterialItem(String materialName, int amount) {
        // Special legacy mappings first
        if (is1_8()) {
            // Player head -> SKULL_ITEM:3
            if ("PLAYER_HEAD".equals(materialName)) {
                try {
                    Material legacy = Material.valueOf("SKULL_ITEM");
                    return new ItemStack(legacy, amount, (short) 3);
                } catch (Exception ignored) {
                    return new ItemStack(Material.STONE, amount);
                }
            }
            // Lapis Lazuli item
            if ("LAPIS_LAZULI".equals(materialName)) {
                try {
                    Material legacy = Material.valueOf("INK_SACK");
                    return new ItemStack(legacy, amount, (short) 4);
                } catch (Exception ignored) {
                    return new ItemStack(Material.STONE, amount);
                }
            }
        }

        Material material = getMaterial(materialName);

        // 1.8 legacy mappings
        if (is1_8()) {
            // Stained glass panes
            if (materialName.contains("STAINED_GLASS_PANE")) {
                short dataValue = getDataValue(materialName);
                return new ItemStack(material, amount, dataValue);
            }
            // Dyes -> INK_SACK data values
            if (materialName.endsWith("_DYE")) {
                Material legacy = Material.valueOf("INK_SACK");
                short data = mapDyeToInkSack(materialName);
                return new ItemStack(legacy, amount, data);
            }
            // Wool colors -> WOOL data values
            if (materialName.endsWith("_WOOL")) {
                Material legacy = Material.valueOf("WOOL");
                short data = mapColorToWool(materialName);
                return new ItemStack(legacy, amount, data);
            }
        }

        return new ItemStack(material, amount);
    }

    /**
     * Get the data value for stained glass pane colors in 1.8.9
     */
    private static short getDataValue(String materialName) {
        // Default gray (7) if unspecified
        if (materialName.contains("LIGHT_GRAY")) return (short) 8; // silver
        if (materialName.contains("GRAY")) return (short) 7;
        if (materialName.contains("BLACK")) return (short) 15;
        if (materialName.contains("WHITE")) return (short) 0;
        if (materialName.contains("RED")) return (short) 14;
        if (materialName.contains("GREEN")) return (short) 13;
        if (materialName.contains("BLUE")) return (short) 11;
        if (materialName.contains("YELLOW")) return (short) 4;
        if (materialName.contains("LIME")) return (short) 5;
        if (materialName.contains("PINK")) return (short) 6;
        if (materialName.contains("PURPLE")) return (short) 10;
        if (materialName.contains("CYAN")) return (short) 9;
        if (materialName.contains("BROWN")) return (short) 12;
        if (materialName.contains("ORANGE")) return (short) 1;
        if (materialName.contains("LIGHT_BLUE")) return (short) 3;
        if (materialName.contains("MAGENTA")) return (short) 2;
        return (short) 7;
    }

    // Map modern *_DYE names to INK_SACK data (1.8)
    private static short mapDyeToInkSack(String materialName) {
        switch (materialName) {
            case "BLACK_DYE": return 0;  // Ink Sac
            case "RED_DYE": return 1;    // Rose Red
            case "GREEN_DYE": return 2;  // Cactus Green
            case "BROWN_DYE": return 3;  // Cocoa Beans
            case "BLUE_DYE": return 4;   // Lapis Lazuli
            case "PURPLE_DYE": return 5; // Purple Dye
            case "CYAN_DYE": return 6;   // Cyan Dye
            case "LIGHT_GRAY_DYE": return 7; // Light Gray
            case "GRAY_DYE": return 8;       // Gray
            case "PINK_DYE": return 9;       // Pink
            case "LIME_DYE": return 10;      // Lime
            case "YELLOW_DYE": return 11;    // Dandelion Yellow
            case "LIGHT_BLUE_DYE": return 12;// Light Blue
            case "MAGENTA_DYE": return 13;   // Magenta
            case "ORANGE_DYE": return 14;    // Orange
            case "WHITE_DYE": return 15;     // Bone Meal
            default: return 0;
        }
    }

    // Map modern *_WOOL names to WOOL data (1.8)
    private static short mapColorToWool(String materialName) {
        if (materialName.equals("WHITE_WOOL")) return 0;
        if (materialName.equals("ORANGE_WOOL")) return 1;
        if (materialName.equals("MAGENTA_WOOL")) return 2;
        if (materialName.equals("LIGHT_BLUE_WOOL")) return 3;
        if (materialName.equals("YELLOW_WOOL")) return 4;
        if (materialName.equals("LIME_WOOL")) return 5;
        if (materialName.equals("PINK_WOOL")) return 6;
        if (materialName.equals("GRAY_WOOL")) return 7;
        if (materialName.equals("LIGHT_GRAY_WOOL")) return 8; // silver
        if (materialName.equals("CYAN_WOOL")) return 9;
        if (materialName.equals("PURPLE_WOOL")) return 10;
        if (materialName.equals("BLUE_WOOL")) return 11;
        if (materialName.equals("BROWN_WOOL")) return 12;
        if (materialName.equals("GREEN_WOOL")) return 13;
        if (materialName.equals("RED_WOOL")) return 14;
        if (materialName.equals("BLACK_WOOL")) return 15;
        return 0;
    }

    /**
     * Get version string for debugging
     */
    public static String getVersionString() {
        return String.format("%d.%d (%s)", MAJOR_VERSION, MINOR_VERSION, SERVER_VERSION);
    }
}
