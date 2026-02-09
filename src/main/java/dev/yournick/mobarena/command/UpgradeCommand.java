package dev.yournick.mobarena.command;

import dev.yournick.mobarena.MobArenaPlugin;
import dev.yournick.mobarena.enchant.CustomEnchant;
import dev.yournick.mobarena.player.PerkProfile;
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
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public class UpgradeCommand implements CommandExecutor, Listener {

    private final MobArenaPlugin plugin;

    public UpgradeCommand(MobArenaPlugin plugin) {
        this.plugin = plugin;
    }

    // Holders for different menus
    private static class CategoryHolder implements InventoryHolder { @Override public Inventory getInventory() { return null; } }
    private static class ItemSelectHolder implements InventoryHolder {
        private final boolean weapon;
        public ItemSelectHolder(boolean weapon) { this.weapon = weapon; }
        public boolean isWeapon() { return weapon; }
        @Override public Inventory getInventory() { return null; }
    }
    private static class EnchantSelectHolder implements InventoryHolder {
        private final ItemStack item;
        private final int slot; // Slot in player inventory
        public EnchantSelectHolder(ItemStack item, int slot) { this.item = item; this.slot = slot; }
        public ItemStack getItem() { return item; }
        public int getSlot() { return slot; }
        @Override public Inventory getInventory() { return null; }
    }

    @Override
    public boolean onCommand(org.bukkit.command.CommandSender sender, org.bukkit.command.Command command, String label, String[] args) {
        if (!(sender instanceof Player)) return true;
        openCategoryMenu((Player) sender);
        return true;
    }

    private void openCategoryMenu(Player player) {
        Inventory inv = Bukkit.createInventory(new CategoryHolder(), 27, "§6Улучшения: Категория");
        
        ItemStack weapon = new ItemStack(Material.DIAMOND_SWORD);
        ItemMeta wMeta = weapon.getItemMeta();
        if (wMeta != null) {
            wMeta.setDisplayName("§eОружие");
            weapon.setItemMeta(wMeta);
        }
        
        ItemStack armor = new ItemStack(Material.DIAMOND_CHESTPLATE);
        ItemMeta aMeta = armor.getItemMeta();
        if (aMeta != null) {
            aMeta.setDisplayName("§bБроня");
            armor.setItemMeta(aMeta);
        }
        
        inv.setItem(11, weapon);
        inv.setItem(15, armor);
        
        player.openInventory(inv);
    }

    private void openItemSelectMenu(Player player, boolean weapon) {
        Inventory inv = Bukkit.createInventory(new ItemSelectHolder(weapon), 36, "§6Выберите предмет");
        
        for (int i = 0; i < player.getInventory().getSize(); i++) {
            ItemStack item = player.getInventory().getItem(i);
            if (item == null || item.getType() == Material.AIR) continue;
            
            if (isCustomItem(item)) {
                boolean isWeapon = item.getType().name().contains("SWORD");
                boolean isArmor = isArmorMaterial(item.getType());
                
                if ((weapon && isWeapon) || (!weapon && isArmor)) {
                    inv.addItem(item.clone());
                }
            }
        }
        
        player.openInventory(inv);
    }

    private void openEnchantMenu(Player player, ItemStack item, int playerSlot) {
        Inventory inv = Bukkit.createInventory(new EnchantSelectHolder(item, playerSlot), 54, "§6Зачарования");
        
        boolean isWeapon = item.getType().name().contains("SWORD");
        int currentEnchants = getEnchantCount(item);
        int limit = isWeapon ? 3 : 1;

        int slot = 0;
        for (CustomEnchant enchant : CustomEnchant.values()) {
            if (enchant.isForWeapon() != isWeapon) continue;
            
            ItemStack icon = new ItemStack(getMaterialByRarity(enchant.getRarity()));
            ItemMeta meta = icon.getItemMeta();
            if (meta != null) {
                meta.setDisplayName(getRarityColor(enchant.getRarity()) + enchant.getDisplayName());
                List<String> lore = new ArrayList<>();
                lore.add("§7" + enchant.getDescription());
                lore.add("");
                lore.add("§7Редкость: " + getRarityColor(enchant.getRarity()) + enchant.getRarity());
                lore.add("§7Цена: §6" + enchant.getCost() + " золота");
                lore.add("");
                
                if (hasEnchant(item, enchant)) {
                    lore.add("§aУже наложено");
                } else if (currentEnchants >= limit) {
                    lore.add("§cЛимит зачарований (" + limit + ")");
                } else {
                    lore.add("§eНажмите, чтобы купить");
                }
                
                meta.setLore(lore);
                icon.setItemMeta(meta);
            }
            inv.setItem(slot++, icon);
        }
        
        player.openInventory(inv);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent e) {
        if (e.getInventory().getHolder() instanceof CategoryHolder) {
            e.setCancelled(true);
            if (e.getRawSlot() == 11) openItemSelectMenu((Player) e.getWhoClicked(), true);
            else if (e.getRawSlot() == 15) openItemSelectMenu((Player) e.getWhoClicked(), false);
        } 
        else if (e.getInventory().getHolder() instanceof ItemSelectHolder) {
            e.setCancelled(true);
            ItemStack clicked = e.getCurrentItem();
            if (clicked == null || clicked.getType() == Material.AIR) return;
            
            Player player = (Player) e.getWhoClicked();
            // Найти слот в инвентаре игрока
            int slot = -1;
            for (int i = 0; i < player.getInventory().getSize(); i++) {
                ItemStack invItem = player.getInventory().getItem(i);
                if (invItem != null && invItem.isSimilar(clicked)) {
                    slot = i;
                    break;
                }
            }
            if (slot != -1) openEnchantMenu(player, clicked, slot);
        }
        else if (e.getInventory().getHolder() instanceof EnchantSelectHolder) {
            e.setCancelled(true);
            ItemStack clicked = e.getCurrentItem();
            if (clicked == null || !clicked.hasItemMeta()) return;
            
            EnchantSelectHolder holder = (EnchantSelectHolder) e.getInventory().getHolder();
            Player player = (Player) e.getWhoClicked();
            PlayerProfile profile = plugin.getPlayerRepository().getProfile(player);
            
            CustomEnchant enchant = CustomEnchant.fromName(ChatColor.stripColor(clicked.getItemMeta().getDisplayName()));
            if (enchant == null) return;
            
            tryBuyEnchant(player, profile, holder.getSlot(), enchant);
        }
    }

    private void tryBuyEnchant(Player player, PlayerProfile profile, int itemSlot, CustomEnchant enchant) {
        ItemStack item = player.getInventory().getItem(itemSlot);
        if (item == null || !isCustomItem(item)) return;
        
        boolean isWeapon = item.getType().name().contains("SWORD");
        int limit = isWeapon ? 3 : 1;
        
        if (hasEnchant(item, enchant)) {
            player.sendMessage("§cЭто зачарование уже наложено!");
            return;
        }
        
        if (getEnchantCount(item) >= limit) {
            player.sendMessage("§cДостигнут лимит зачарований (" + limit + ")!");
            return;
        }
        
        int cost = enchant.getCost();
        
        // Скидка кузнеца
        if (profile.getPlayerPerk().getPerk() == dev.yournick.mobarena.perk.Perk.BLACKSMITH) {
            PerkProfile pp = profile.getPerkProfile(dev.yournick.mobarena.perk.Perk.BLACKSMITH);
            
            // Экономия (Мини 3) - Шанс на бесплатное улучшение
            int economyLevel = pp.getMini3();
            if (economyLevel > 0 && Math.random() < economyLevel * 0.03) {
                cost = 0;
                player.sendMessage("§6ЭКОНОМИЯ! Улучшение бесплатно!");
            } else {
                int level = pp.getMini1();
                double discount = (35 + level * 3) / 100.0;
                cost = (int) (cost * (1.0 - discount));
            }
        }

        if (profile.getGold() < cost) {
            player.sendMessage("§cНедостаточно золота!");
            return;
        }
        
        profile.removeGold(cost);
        applyEnchant(item, enchant);
        player.sendMessage("§aЗачарование " + enchant.getDisplayName() + " успешно наложено!");
        player.closeInventory();
        profile.recalculateStats();
    }

    private boolean isCustomItem(ItemStack item) {
        if (item == null || !item.hasItemMeta() || item.getItemMeta().getLore() == null) return false;
        for (String line : item.getItemMeta().getLore()) {
            if (line.contains("Custom Item")) return true;
        }
        return false;
    }

    private boolean isArmorMaterial(Material mat) {
        String n = mat.name();
        return n.contains("HELMET") || n.contains("CHESTPLATE") || n.contains("LEGGINGS") || n.contains("BOOTS");
    }

    private boolean hasEnchant(ItemStack item, CustomEnchant enchant) {
        if (item == null || !item.hasItemMeta() || item.getItemMeta().getLore() == null) return false;
        for (String line : item.getItemMeta().getLore()) {
            if (line.contains(enchant.getDisplayName())) return true;
        }
        return false;
    }

    private int getEnchantCount(ItemStack item) {
        int count = 0;
        if (item == null || !item.hasItemMeta() || item.getItemMeta().getLore() == null) return 0;
        for (String line : item.getItemMeta().getLore()) {
            if (line.startsWith("§7- ")) {
                String name = ChatColor.stripColor(line).substring(2);
                if (CustomEnchant.fromName(name) != null) count++;
            }
        }
        return count;
    }

    private void applyEnchant(ItemStack item, CustomEnchant enchant) {
        ItemMeta meta = item.getItemMeta();
        List<String> lore = meta.getLore();
        if (lore == null) lore = new ArrayList<>();
        lore.add("§7- " + getRarityColor(enchant.getRarity()) + enchant.getDisplayName());
        meta.setLore(lore);
        item.setItemMeta(meta);
    }

    private ChatColor getRarityColor(String rarity) {
        switch (rarity) {
            case "Common": return ChatColor.GRAY;
            case "Rare": return ChatColor.BLUE;
            case "Epic": return ChatColor.DARK_PURPLE;
            case "Legendary": return ChatColor.GOLD;
            case "Mythic": return ChatColor.LIGHT_PURPLE;
            default: return ChatColor.WHITE;
        }
    }

    private Material getMaterialByRarity(String rarity) {
        switch (rarity) {
            case "Common": return Material.IRON_INGOT;
            case "Rare": return Material.GOLD_INGOT;
            case "Epic": return Material.DIAMOND;
            case "Legendary": return Material.EMERALD;
            case "Mythic": return Material.NETHER_STAR;
            default: return Material.PAPER;
        }
    }
}
