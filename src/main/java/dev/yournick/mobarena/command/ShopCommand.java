package dev.yournick.mobarena.command;

import dev.yournick.mobarena.MobArenaPlugin;
import dev.yournick.mobarena.player.PlayerProfile;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public class ShopCommand implements CommandExecutor, Listener {

    private final MobArenaPlugin plugin;

    public ShopCommand(MobArenaPlugin plugin) {
        this.plugin = plugin;
    }

    private static class ShopHolder implements InventoryHolder { @Override public Inventory getInventory() { return null; } }
    private static class WeaponShopHolder implements InventoryHolder { @Override public Inventory getInventory() { return null; } }
    private static class ArmorShopHolder implements InventoryHolder { @Override public Inventory getInventory() { return null; } }
    private static class ConsumableShopHolder implements InventoryHolder { @Override public Inventory getInventory() { return null; } }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player)) return true;
        openShop((Player) sender);
        return true;
    }

    public void openShop(Player player) {
        Inventory inv = Bukkit.createInventory(new ShopHolder(), 27, "§6Магазин арены");
        
        inv.setItem(11, createCategoryItem(Material.DIAMOND_SWORD, "§eОружие"));
        inv.setItem(13, createCategoryItem(Material.DIAMOND_CHESTPLATE, "§bБроня"));
        inv.setItem(15, createCategoryItem(Material.COOKED_BEEF, "§aРасходники"));
        
        player.openInventory(inv);
    }

    private ItemStack createCategoryItem(Material mat, String name) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
            item.setItemMeta(meta);
        }
        return item;
    }

    private void openWeaponShop(Player player) {
        Inventory inv = Bukkit.createInventory(new WeaponShopHolder(), 27, "§6Оружие");
        
        addItem(inv, Material.WOOD_SWORD, "Деревянный меч", 50, "1.0", 1, player);
        addItem(inv, Material.STONE_SWORD, "Каменный меч", 100, "2.5", 1, player);
        addItem(inv, Material.IRON_SWORD, "Железный меч", 200, "4.0", 1, player);
        addItem(inv, Material.DIAMOND_SWORD, "Алмазный меч", 400, "6.0", 1, player);
        addItem(inv, Material.DIAMOND_SWORD, "Меч Гладиатора", 1500, "9.0", 1, player);
        addItem(inv, Material.DIAMOND_SWORD, "Меч Погибели", 2500, "12.0", 1, player);
        addItem(inv, Material.DIAMOND_SWORD, "Клинок Бездны", 5000, "15.0", 1, player);
        
        player.openInventory(inv);
    }

    private void openArmorShop(Player player) {
        Inventory inv = Bukkit.createInventory(new ArmorShopHolder(), 27, "§6Броня");
        
        addItem(inv, Material.LEATHER_HELMET, "Кожаная броня", 100, "10.0%", 1, player);
        addItem(inv, Material.CHAINMAIL_HELMET, "Кольчужная броня", 200, "16.0%", 1, player);
        addItem(inv, Material.IRON_HELMET, "Железная броня", 300, "20.0%", 1, player);
        addItem(inv, Material.DIAMOND_HELMET, "Алмазная броня", 500, "28.0%", 1, player);
        addItem(inv, Material.DIAMOND_HELMET, "Элитная броня", 2500, "36.0%", 1, player);
        addItem(inv, Material.DIAMOND_HELMET, "Героическая броня", 7500, "40.0%", 1, player);
        addItem(inv, Material.DIAMOND_HELMET, "Мифическая броня", 15000, "50.0%", 1, player);
        addItem(inv, Material.SHIELD, "Щит", 15, "+15.0% Брони", 1, player);
        
        player.openInventory(inv);
    }

    private void openConsumableShop(Player player) {
        Inventory inv = Bukkit.createInventory(new ConsumableShopHolder(), 27, "§6Расходники");
        
        addItem(inv, Material.COOKED_BEEF, "Стейк арены", 30, "+25.0% Макс. HP", 16, player);
        addItem(inv, Material.POTION, "Зелье здоровья", 600, "+50.0% Макс. HP (60с)", 1, player);
        addItem(inv, Material.POTION, "Зелье урона", 750, "+35.0% Урона (60с)", 1, player);
        addItem(inv, Material.POTION, "Зелье скорости", 550, "+25.0% Скорости (60с)", 1, player);
        addItem(inv, Material.GOLDEN_APPLE, "Золотое яблоко арены", 5000, "Реген II + 100% HP", 1, player);
        addItem(inv, Material.PRISMARINE_CRYSTALS, "Магический осколок", 500, "Валюта для гачи", 1, player);
        
        player.openInventory(inv);
    }

    private void addItem(Inventory inv, Material mat, String name, int cost, String stats, int amount, Player player) {
        PlayerProfile profile = plugin.getPlayerRepository().getProfile(player);
        int finalCost = cost;
        if (profile != null && profile.getPlayerPerk().getPerk() == dev.yournick.mobarena.perk.Perk.TRADER) {
            finalCost = (int) (cost * 0.75);
        }

        ItemStack item = new ItemStack(mat, amount);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.GREEN + name);
            List<String> lore = new ArrayList<>();
            if (!stats.isEmpty()) lore.add("§7Характеристика: §f" + stats);
            lore.add(ChatColor.YELLOW + "Цена: " + finalCost + " золота");
            if (mat.name().contains("SWORD") || isArmor(mat) || mat == Material.SHIELD || mat == Material.POTION || mat == Material.GOLDEN_APPLE || mat == Material.COOKED_BEEF) {
                lore.add("§8Custom Item");
            }
            meta.setLore(lore);
            meta.setUnbreakable(true);
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_UNBREAKABLE);

            if (mat == Material.POTION) {
                org.bukkit.inventory.meta.PotionMeta pm = (org.bukkit.inventory.meta.PotionMeta) meta;
                if (name.contains("здоровья")) pm.setColor(org.bukkit.Color.RED);
                else if (name.contains("урона")) pm.setColor(org.bukkit.Color.PURPLE);
                else if (name.contains("скорости")) pm.setColor(org.bukkit.Color.AQUA);
            }
            
            item.setItemMeta(meta);
        }
        inv.addItem(item);
    }

    @EventHandler
    public void handleClick(InventoryClickEvent e) {
        Inventory inv = e.getInventory();
        if (inv == null || inv.getHolder() == null) return;
        
        InventoryHolder holder = inv.getHolder();
        if (!(holder instanceof ShopHolder || holder instanceof WeaponShopHolder || holder instanceof ArmorShopHolder || holder instanceof ConsumableShopHolder)) return;
        
        e.setCancelled(true);
        Player player = (Player) e.getWhoClicked();
        ItemStack clicked = e.getCurrentItem();
        if (clicked == null || !clicked.hasItemMeta()) return;

        if (holder instanceof ShopHolder) {
            String name = ChatColor.stripColor(clicked.getItemMeta().getDisplayName());
            if (name.equals("Оружие")) openWeaponShop(player);
            else if (name.equals("Броня")) openArmorShop(player);
            else if (name.equals("Расходники")) openConsumableShop(player);
            return;
        }

        PlayerProfile profile = plugin.getPlayerRepository().getProfile(player);
        if (profile == null) return;

        int cost = getCostFromLore(clicked);
        if (profile.getGold() < cost) {
            player.sendMessage(ChatColor.RED + "Недостаточно золота!");
            return;
        }

        profile.removeGold(cost);

        if (holder instanceof ArmorShopHolder && isArmor(clicked.getType())) {
            giveFullArmorSet(player, clicked);
            player.sendMessage(ChatColor.GREEN + "Вы купили полный сет " + clicked.getItemMeta().getDisplayName() + " за " + cost + " золота!");
        } else if (clicked.getType() == Material.PRISMARINE_CRYSTALS) {
            profile.addShards(1);
            player.sendMessage(ChatColor.GREEN + "Вы купили Магический осколок за " + cost + " золота!");
        } else {
            ItemStack give = clicked.clone();
            ItemMeta meta = give.getItemMeta();
            List<String> lore = meta.getLore();
            if (lore != null) {
                lore.removeIf(line -> line.contains("Цена:"));
                meta.setLore(lore);
                give.setItemMeta(meta);
            }
            
            player.getInventory().addItem(give);
            player.sendMessage(ChatColor.GREEN + "Вы купили " + clicked.getItemMeta().getDisplayName() + " за " + cost + " золота!");
        }
        player.closeInventory();
    }

    private boolean isArmor(Material mat) {
        return mat.name().endsWith("_HELMET") || mat.name().endsWith("_CHESTPLATE") ||
                mat.name().endsWith("_LEGGINGS") || mat.name().endsWith("_BOOTS");
    }

    private void giveFullArmorSet(Player player, ItemStack clicked) {
        Material helmet = clicked.getType();
        ItemStack h = new ItemStack(helmet);
        ItemStack c = new ItemStack(getChestFromHelmet(helmet));
        ItemStack l = new ItemStack(getLegsFromHelmet(helmet));
        ItemStack b = new ItemStack(getBootsFromHelmet(helmet));

        for (ItemStack i : new ItemStack[]{h, c, l, b}) {
            ItemMeta meta = i.getItemMeta();
            if (meta != null) {
                meta.setDisplayName(clicked.getItemMeta().getDisplayName());
                List<String> lore = new ArrayList<>();
                lore.add("§8Custom Item");
                meta.setLore(lore);
                meta.setUnbreakable(true);
                meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_UNBREAKABLE);
                i.setItemMeta(meta);
            }
        }

        player.getInventory().setHelmet(h);
        player.getInventory().setChestplate(c);
        player.getInventory().setLeggings(l);
        player.getInventory().setBoots(b);
    }

    private Material getChestFromHelmet(Material h) {
        switch (h) {
            case LEATHER_HELMET: return Material.LEATHER_CHESTPLATE;
            case CHAINMAIL_HELMET: return Material.CHAINMAIL_CHESTPLATE;
            case IRON_HELMET: return Material.IRON_CHESTPLATE;
            case DIAMOND_HELMET: return Material.DIAMOND_CHESTPLATE;
            default: return Material.DIAMOND_CHESTPLATE;
        }
    }
    private Material getLegsFromHelmet(Material h) {
        switch (h) {
            case LEATHER_HELMET: return Material.LEATHER_LEGGINGS;
            case CHAINMAIL_HELMET: return Material.CHAINMAIL_LEGGINGS;
            case IRON_HELMET: return Material.IRON_LEGGINGS;
            case DIAMOND_HELMET: return Material.DIAMOND_LEGGINGS;
            default: return Material.DIAMOND_LEGGINGS;
        }
    }
    private Material getBootsFromHelmet(Material h) {
        switch (h) {
            case LEATHER_HELMET: return Material.LEATHER_BOOTS;
            case CHAINMAIL_HELMET: return Material.CHAINMAIL_BOOTS;
            case IRON_HELMET: return Material.IRON_BOOTS;
            case DIAMOND_HELMET: return Material.DIAMOND_BOOTS;
            default: return Material.DIAMOND_BOOTS;
        }
    }

    private int getCostFromLore(ItemStack item) {
        if (!item.hasItemMeta() || item.getItemMeta().getLore() == null) return 0;
        for (String line : item.getItemMeta().getLore()) {
            if (line.contains("Цена:")) {
                return Integer.parseInt(line.replaceAll("[^0-9]", ""));
            }
        }
        return 0;
    }
}
