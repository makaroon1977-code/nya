package dev.yournick.mobarena.command;

import dev.yournick.mobarena.MobArenaPlugin;
import dev.yournick.mobarena.player.PlayerProfile;
import dev.yournick.mobarena.player.PlayerStats;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public class DetailedStatsCommand implements CommandExecutor, Listener {

    private final MobArenaPlugin plugin;

    public DetailedStatsCommand(MobArenaPlugin plugin) {
        this.plugin = plugin;
    }

    private static class MainStatsHolder implements InventoryHolder {
        @Override public Inventory getInventory() { return null; }
    }

    private static class StatSourcesHolder implements InventoryHolder {
        private final String statKey;
        public StatSourcesHolder(String statKey) { this.statKey = statKey; }
        public String getStatKey() { return statKey; }
        @Override public Inventory getInventory() { return null; }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) return true;
        openMainStats((Player) sender);
        return true;
    }

    private void openMainStats(Player player) {
        PlayerProfile profile = plugin.getPlayerRepository().getProfile(player);
        if (profile == null) return;
        profile.recalculateStats();
        PlayerStats stats = profile.getStats();

        Inventory inv = Bukkit.createInventory(new MainStatsHolder(), 36, "§6Подробная статистика");

        addStatItem(inv, 10, Material.DIAMOND_SWORD, "Урон", "damagePercent", stats.getDamagePercent(), "Сложение", "Урон меча * Урон%");
        addStatItem(inv, 11, Material.BOW, "Крит. шанс", "critChance", stats.getCritChance(), "Убывающая полезность (Лимит 80%)", "Шанс нанести Крит. урон");
        addStatItem(inv, 12, Material.ENCHANTED_BOOK, "Крит. урон", "critDamage", stats.getCritDamage(), "Сложение", "Множитель урона при крите");
        addStatItem(inv, 13, Material.FEATHER, "Скор. атаки", "attackSpeed", stats.getAttackSpeedPercent(), "Сложение", "100% = 3 уд/сек, 200% = 6 уд/сек");
        addStatItem(inv, 14, Material.GHAST_TEAR, "Вампиризм", "lifesteal", stats.getLifesteal(), "Убывающая полезность (Лимит 75%)", "% от нанесенного урона в HP");
        addStatItem(inv, 15, Material.FIREBALL, "Огненный урон", "fireDamagePercent", stats.getFireDamagePercent(), "Сложение", "Множитель по горящим целям");
        addStatItem(inv, 16, Material.MAGMA_CREAM, "Сопр. огню", "fireResistance", stats.getFireResistance(), "Сложение (100% = Иммунитет)", "Снижение урона от огня");

        addStatItem(inv, 19, Material.GOLDEN_APPLE, "Макс. здоровье", "maxHealthPercent", stats.getMaxHealthPercent(), "Сложение", "20 HP * Здоровье%");
        addStatItem(inv, 20, Material.DIAMOND_CHESTPLATE, "Броня", "armorPercent", stats.getArmorPercent(), "Убывающая полезность (Лимит 85%)", "Снижение входящего урона");
        addStatItem(inv, 21, Material.RABBIT_FOOT, "Уклонение", "dodgeChance", stats.getDodgeChance(), "Убывающая полезность (Лимит 90%)", "Шанс избежать урона");
        addStatItem(inv, 22, Material.PRISMARINE_SHARD, "Отражение", "damageReflection", stats.getDamageReflection(), "Сложение", "% возврата урона ДО снижения");
        addStatItem(inv, 23, Material.ANVIL, "Сопр. откидыванию", "knockbackResistance", stats.getKnockbackResistance(), "Убывающая полезность (Лимит 90%)", "Шанс не отлетать");
        addStatItem(inv, 24, Material.SPECKLED_MELON, "Хил за убийство", "healOnKill", stats.getHealOnKill(), "Сложение", "Восстановление HP при смерти моба");
        addStatItem(inv, 25, Material.SUGAR, "Скорость", "moveSpeedPercent", stats.getMoveSpeedPercent(), "Сложение", "Базовая скорость * Скорость%");

        addStatItem(inv, 31, Material.TOTEM, "Шанс спасения", "deathSaveChance", stats.getDeathSaveChance(), "Сложение", "Шанс выжить при смертельном ударе");

        player.openInventory(inv);
    }

    private void addStatItem(Inventory inv, int slot, Material mat, String name, String key, double value, String system, String formula) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            String valStr;
            if (key.equals("healOnKill")) {
                valStr = String.format("%.1f HP", value);
            } else {
                valStr = String.format("%.1f%%", value * 100);
            }

            meta.setDisplayName("§e" + name + ": §f" + valStr);
            List<String> lore = new ArrayList<>();
            lore.add("§7Система: §b" + system);
            lore.add("§7Работает: §a" + formula);
            lore.add("");
            lore.add("§eПКМ §7- Показать источники");
            
            meta.setLore(lore);
            // Скрываем атрибуты чтобы не мешались (для брони/мечей)
            meta.addItemFlags(org.bukkit.inventory.ItemFlag.HIDE_ATTRIBUTES);
            item.setItemMeta(meta);
        }
        inv.setItem(slot, item);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent e) {
        if (e.getInventory().getHolder() instanceof MainStatsHolder) {
            e.setCancelled(true);
            if (e.getClick() == ClickType.RIGHT) {
                ItemStack clicked = e.getCurrentItem();
                if (clicked == null || clicked.getType() == Material.AIR) return;
                
                String statKey = getStatKeyFromItem(clicked);
                if (statKey != null) {
                    openStatSources((Player) e.getWhoClicked(), statKey);
                }
            }
        } else if (e.getInventory().getHolder() instanceof StatSourcesHolder) {
            e.setCancelled(true);
            // Можно добавить кнопку "Назад"
            if (e.getSlot() == 45) { // Например
                openMainStats((Player) e.getWhoClicked());
            }
        }
    }

    private void openStatSources(Player player, String statKey) {
        PlayerProfile profile = plugin.getPlayerRepository().getProfile(player);
        if (profile == null) return;

        String statName = getStatNameFromKey(statKey);
        Inventory inv = Bukkit.createInventory(new StatSourcesHolder(statKey), 54, "§6Источники: " + statName);

        List<ItemStack> sources = profile.getStatBreakdown(statKey);
        for (int i = 0; i < sources.size() && i < 45; i++) {
            inv.setItem(i, sources.get(i));
        }

        // Кнопка назад
        ItemStack back = new ItemStack(Material.ARROW);
        ItemMeta bMeta = back.getItemMeta();
        if (bMeta != null) {
            bMeta.setDisplayName("§cНазад");
            back.setItemMeta(bMeta);
        }
        inv.setItem(45, back);

        player.openInventory(inv);
    }

    private String getStatKeyFromItem(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return null;
        String name = ChatColor.stripColor(item.getItemMeta().getDisplayName());
        if (name.contains("Урон")) return "damagePercent";
        if (name.contains("Крит. шанс")) return "critChance";
        if (name.contains("Крит. урон")) return "critDamage";
        if (name.contains("Скор. атаки")) return "attackSpeed";
        if (name.contains("Вампиризм")) return "lifesteal";
        if (name.contains("Огненный урон")) return "fireDamagePercent";
        if (name.contains("Сопр. огню")) return "fireResistance";
        if (name.contains("Макс. здоровье")) return "maxHealthPercent";
        if (name.contains("Броня")) return "armorPercent";
        if (name.contains("Уклонение")) return "dodgeChance";
        if (name.contains("Отражение")) return "damageReflection";
        if (name.contains("Сопр. откидыванию")) return "knockbackResistance";
        if (name.contains("Хил за убийство")) return "healOnKill";
        if (name.contains("Скорость")) return "moveSpeedPercent";
        if (name.contains("Шанс спасения")) return "deathSaveChance";
        return null;
    }

    private String getStatNameFromKey(String key) {
        switch (key) {
            case "damagePercent": return "Урон";
            case "critChance": return "Крит. шанс";
            case "critDamage": return "Крит. урон";
            case "attackSpeed": return "Скор. атаки";
            case "lifesteal": return "Вампиризм";
            case "fireDamagePercent": return "Огненный урон";
            case "fireResistance": return "Сопр. огню";
            case "maxHealthPercent": return "Макс. здоровье";
            case "armorPercent": return "Броня";
            case "dodgeChance": return "Уклонение";
            case "damageReflection": return "Отражение";
            case "knockbackResistance": return "Сопр. откидыванию";
            case "healOnKill": return "Хил за убийство";
            case "moveSpeedPercent": return "Скорость";
            case "deathSaveChance": return "Шанс спасения";
            default: return key;
        }
    }
}
