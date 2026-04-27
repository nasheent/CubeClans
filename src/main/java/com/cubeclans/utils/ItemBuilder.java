package com.cubeclans.utils;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ItemBuilder {

    private final ItemStack itemStack;
    private static NamespacedKey MENU_ITEM_KEY;

    public ItemBuilder(Material material) {
        this.itemStack = new ItemStack(material);
    }

    public ItemBuilder(Material material, int amount) {
        this.itemStack = new ItemStack(material, amount);
    }

    public ItemBuilder(ItemStack itemStack) {
        this.itemStack = itemStack.clone();
    }

    /**
     * Constructor that takes a material name string and applies version-appropriate data value
     * Useful for materials like STAINED_GLASS_PANE that need data values in older versions
     */
    public ItemBuilder(String materialName) {
        this.itemStack = VersionHelper.createMaterialItem(materialName, 1);
    }

    /**
     * Constructor that takes a material name string with custom amount
     */
    public ItemBuilder(String materialName, int amount) {
        this.itemStack = VersionHelper.createMaterialItem(materialName, amount);
    }

    public ItemBuilder name(String name) {
        ItemMeta meta = itemStack.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ColorUtil.translate(name));
            itemStack.setItemMeta(meta);
        }
        return this;
    }

    public ItemBuilder lore(String... lore) {
        return lore(Arrays.asList(lore));
    }

    public ItemBuilder lore(List<String> lore) {
        ItemMeta meta = itemStack.getItemMeta();
        if (meta != null) {
            List<String> coloredLore = new ArrayList<>();
            for (String line : lore) {
                coloredLore.add(ColorUtil.translate(line));
            }
            meta.setLore(coloredLore);
            itemStack.setItemMeta(meta);
        }
        return this;
    }

    public ItemBuilder addLore(String... lore) {
        ItemMeta meta = itemStack.getItemMeta();
        if (meta != null) {
            List<String> currentLore = meta.getLore();
            if (currentLore == null) {
                currentLore = new ArrayList<>();
            }
            for (String line : lore) {
                currentLore.add(ColorUtil.translate(line));
            }
            meta.setLore(currentLore);
            itemStack.setItemMeta(meta);
        }
        return this;
    }

    public ItemBuilder amount(int amount) {
        itemStack.setAmount(amount);
        return this;
    }

    public ItemBuilder glow(boolean glow) {
        if (glow) {
            ItemMeta meta = itemStack.getItemMeta();
            if (meta != null) {
                meta.addEnchant(org.bukkit.enchantments.Enchantment.LUCK, 1, true);
                meta.addItemFlags(org.bukkit.inventory.ItemFlag.HIDE_ENCHANTS);
                itemStack.setItemMeta(meta);
            }
        }
        return this;
    }

    public ItemBuilder flags(org.bukkit.inventory.ItemFlag... flags) {
        ItemMeta meta = itemStack.getItemMeta();
        if (meta != null) {
            meta.addItemFlags(flags);
            itemStack.setItemMeta(meta);
        }
        return this;
    }

    public ItemBuilder menuItemType(String type) {
        // Only use PDC if NamespacedKey is available (1.13+)
        if (!VersionHelper.hasNamespacedKey()) {
            return this;
        }
        
        try {
            ItemMeta meta = itemStack.getItemMeta();
            if (meta != null) {
                if (MENU_ITEM_KEY == null) {
                    MENU_ITEM_KEY = new NamespacedKey(org.bukkit.Bukkit.getPluginManager().getPlugin("CubeClans"), "menu_item_type");
                }
                PersistentDataContainer pdc = meta.getPersistentDataContainer();
                pdc.set(MENU_ITEM_KEY, PersistentDataType.STRING, type);
                itemStack.setItemMeta(meta);
            }
        } catch (Exception ignored) {
            // Silently fail if PDC is not available
        }
        return this;
    }

    public static String getMenuItemType(ItemStack item) {
        // Only use PDC if NamespacedKey is available (1.13+)
        if (!VersionHelper.hasNamespacedKey()) {
            return null;
        }
        
        try {
            if (item == null || !item.hasItemMeta()) return null;
            if (MENU_ITEM_KEY == null) {
                MENU_ITEM_KEY = new NamespacedKey(org.bukkit.Bukkit.getPluginManager().getPlugin("CubeClans"), "menu_item_type");
            }
            PersistentDataContainer pdc = item.getItemMeta().getPersistentDataContainer();
            return pdc.get(MENU_ITEM_KEY, PersistentDataType.STRING);
        } catch (Exception ignored) {
            // Silently fail if PDC is not available
            return null;
        }
    }

    public ItemBuilder tag(String key, String value) {
        try {
            Object nmsStack = getNMSItemStack(itemStack);
            Object tag = getTag(nmsStack);
            
            setString(tag, key, value);
            setTag(nmsStack, tag);
        } catch (Exception e) {
            // Fallback: if NBT doesn't work, at least return
        }
        return this;
    }

    private Object getNMSItemStack(ItemStack stack) throws Exception {
        Class<?> craftItemStack = Class.forName("org.bukkit.craftbukkit.inventory.CraftItemStack");
        java.lang.reflect.Method asNMS = craftItemStack.getMethod("asNMSCopy", ItemStack.class);
        return asNMS.invoke(null, stack);
    }

    private Object getTag(Object nmsStack) throws Exception {
        Class<?> nmsItemStackClass = nmsStack.getClass();
        java.lang.reflect.Method getTag = nmsItemStackClass.getMethod("getTag");
        Object tag = getTag.invoke(nmsStack);
        if (tag == null) {
            Class<?> nbtTagCompound = Class.forName("net.minecraft.nbt.NBTTagCompound");
            tag = nbtTagCompound.newInstance();
        }
        return tag;
    }

    private void setString(Object tag, String key, String value) throws Exception {
        java.lang.reflect.Method setString = tag.getClass().getMethod("setString", String.class, String.class);
        setString.invoke(tag, key, value);
    }

    private void setTag(Object nmsStack, Object tag) throws Exception {
        Class<?> nmsItemStackClass = nmsStack.getClass();
        java.lang.reflect.Method setTag = nmsItemStackClass.getMethod("setTag", tag.getClass());
        setTag.invoke(nmsStack, tag);
    }

    public ItemBuilder texture(String textureValue) {
        if (itemStack.getType().name().equals("PLAYER_HEAD")) {
            try {
                ItemMeta meta = itemStack.getItemMeta();
                if (meta != null && meta instanceof org.bukkit.inventory.meta.SkullMeta) {
                    org.bukkit.inventory.meta.SkullMeta skullMeta = (org.bukkit.inventory.meta.SkullMeta) meta;
                    
                    // Use reflection to set texture via GameProfile
                    Class<?> profileClass = Class.forName("com.mojang.authlib.GameProfile");
                    Class<?> propertyClass = Class.forName("com.mojang.authlib.properties.Property");
                    Class<?> propertyMapClass = Class.forName("com.google.common.collect.ForwardingMultimap");
                    
                    java.util.UUID uuid = java.util.UUID.nameUUIDFromBytes(("OfflinePlayer:" + System.nanoTime()).getBytes());
                    java.lang.reflect.Constructor<?> profileConstructor = profileClass.getConstructor(java.util.UUID.class, String.class);
                    Object gameProfile = profileConstructor.newInstance(uuid, "CubeClans");
                    
                    java.lang.reflect.Constructor<?> propConstructor = propertyClass.getConstructor(String.class, String.class);
                    Object property = propConstructor.newInstance("textures", textureValue);
                    
                    java.lang.reflect.Method getPropertiesMethod = profileClass.getMethod("getProperties");
                    Object properties = getPropertiesMethod.invoke(gameProfile);
                    
                    java.lang.reflect.Method putMethod = propertyMapClass.getMethod("put", Object.class, Object.class);
                    putMethod.invoke(properties, "textures", property);
                    
                    java.lang.reflect.Field profileField = org.bukkit.inventory.meta.SkullMeta.class.getDeclaredField("profile");
                    profileField.setAccessible(true);
                    profileField.set(skullMeta, gameProfile);
                    
                    itemStack.setItemMeta(skullMeta);
                }
            } catch (Exception e) {
                // Fallback if reflection fails - just continue without texture
            }
        }
        return this;
    }

    public ItemStack build() {
        return itemStack;
    }
}
