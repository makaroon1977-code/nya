package dev.yournick.mobarena.arena;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public class ArenaLoot {

    /**
     * Создает кастомный предмет для продажи.
     * Здесь можно легко менять названия и цены.
     */
    public static ItemStack getZombieDrop() {
        return createLootItem(
                Material.ROTTEN_FLESH,
                ChatColor.DARK_GREEN + "Плоть зомби",
                15 // Цена продажи
        );
    }

    public static ItemStack getSkeletonDrop() {
        return createLootItem(
                Material.BONE,
                ChatColor.WHITE + "Кость скелета",
                20
        );
    }

    public static ItemStack getSpiderDrop() {
        return createLootItem(
                Material.SPIDER_EYE,
                ChatColor.RED + "Глаз паука",
                25
        );
    }

    public static ItemStack getGoldDrop() {
        return createLootItem(
                Material.GOLD_NUGGET,
                ChatColor.GOLD + "Золотой самородок",
                50
        );
    }

    public static ItemStack getMagicalShard() {
        ItemStack item = new ItemStack(Material.PRISMARINE_CRYSTALS);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.AQUA + "Магический осколок");
            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.GRAY + "Используется для гачи амулетов");
            lore.add("");
            lore.add(ChatColor.YELLOW + "Нужно 10 штук для 1 крутки");
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }

    private static ItemStack createLootItem(Material mat, String name, int price) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.GRAY + "Предмет арены");
            lore.add("");
            lore.add(ChatColor.YELLOW + "Цена продажи: " + ChatColor.GOLD + price + " золота");
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }

    /**
     * Проверяет, является ли предмет "предметом арены" и возвращает его цену.
     */
    public static int getPrice(ItemStack item) {
        if (item == null || !item.hasItemMeta() || item.getItemMeta().getLore() == null) return 0;
        List<String> lore = item.getItemMeta().getLore();
        
        // Проверяем метку "Предмет арены"
        boolean isArenaItem = false;
        for (String line : lore) {
            if (line.contains("Предмет арены")) {
                isArenaItem = true;
                break;
            }
        }
        
        if (!isArenaItem) return 0;

        // Ищем строку с ценой
        for (String line : lore) {
            if (line.contains("Цена продажи:")) {
                try {
                    return Integer.parseInt(ChatColor.stripColor(line).replaceAll("[^0-9]", ""));
                } catch (NumberFormatException e) {
                    return 0;
                }
            }
        }
        return 0;
    }
}
