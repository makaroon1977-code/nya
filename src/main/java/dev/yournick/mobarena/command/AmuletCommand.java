package dev.yournick.mobarena.command;

import dev.yournick.mobarena.MobArenaPlugin;
import dev.yournick.mobarena.amulet.Amulet;
import dev.yournick.mobarena.amulet.AmuletRarity;
import dev.yournick.mobarena.amulet.AmuletType;
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
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class AmuletCommand implements CommandExecutor, Listener {

    private final MobArenaPlugin plugin;

    public AmuletCommand(MobArenaPlugin plugin) {
        this.plugin = plugin;
    }

    public static class AmuletMenuHolder implements InventoryHolder {
        @Override public Inventory getInventory() { return null; }
    }

    public static class AmuletUpgradeHolder implements InventoryHolder {
        private final AmuletType type;
        public AmuletUpgradeHolder(AmuletType type) { this.type = type; }
        public AmuletType getType() { return type; }
        @Override public Inventory getInventory() { return null; }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) return true;
        openAmuletMenu((Player) sender);
        return true;
    }

    public void openAmuletMenu(Player player) {
        Inventory inv = Bukkit.createInventory(new AmuletMenuHolder(), 54, "§6Ваши амулеты");
        PlayerProfile profile = plugin.getPlayerRepository().getProfile(player);

        // Equipped slots (10, 12, 14, 16)
        int[] slots = {10, 12, 14, 16};
        for (int i = 0; i < 4; i++) {
            AmuletType type = profile.getEquippedAmulets()[i];
            if (type == null) {
                inv.setItem(slots[i], createEmptySlot());
            } else {
                int level = profile.getOwnedAmulets().get(type);
                ItemStack item = new Amulet(type, level).createItem();
                ItemMeta meta = item.getItemMeta();
                List<String> lore = meta.getLore();
                lore.add("");
                lore.add("§cНажмите, чтобы снять");
                meta.setLore(lore);
                item.setItemMeta(meta);
                inv.setItem(slots[i], item);
            }
        }

        // Decorative glass
        ItemStack glass = new ItemStack(Material.STAINED_GLASS_PANE, 1, (short) 15);
        for (int i = 0; i < 27; i++) {
            boolean isSlot = false;
            for (int s : slots) if (s == i) isSlot = true;
            if (!isSlot) inv.setItem(i, glass);
        }

        // Owned amulets from slot 27
        int slot = 27;
        for (Map.Entry<AmuletType, Integer> entry : profile.getOwnedAmulets().entrySet()) {
            if (slot >= 54) break;
            AmuletType type = entry.getKey();
            int level = entry.getValue();
            
            ItemStack item = new Amulet(type, level).createItem();
            ItemMeta meta = item.getItemMeta();
            List<String> lore = meta.getLore();
            
            boolean equipped = false;
            for (AmuletType eq : profile.getEquippedAmulets()) if (eq == type) equipped = true;
            
            lore.add("");
            if (equipped) {
                lore.add("§aЭкипировано");
            } else {
                lore.add("§eЛКМ §7- Экипировать");
            }
            lore.add("§bПКМ §7- Улучшить");
            
            meta.setLore(lore);
            item.setItemMeta(meta);
            inv.setItem(slot++, item);
        }

        player.openInventory(inv);
    }

    private ItemStack createEmptySlot() {
        ItemStack item = new ItemStack(Material.BARRIER);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName("§7Пустой слот");
            item.setItemMeta(meta);
        }
        return item;
    }

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        if (e.getInventory().getHolder() instanceof AmuletMenuHolder) {
            e.setCancelled(true);
            if (e.getCurrentItem() == null) return;
            Player player = (Player) e.getWhoClicked();
            PlayerProfile profile = plugin.getPlayerRepository().getProfile(player);
            
            int slot = e.getSlot();
            if (slot >= 27) {
                // Owned amulets area
                AmuletType type = getAmuletTypeFromItem(e.getCurrentItem());
                if (type == null) return;
                
                if (e.getClick() == ClickType.LEFT) {
                    equipAmulet(player, profile, type);
                } else if (e.getClick() == ClickType.RIGHT) {
                    openUpgradeMenu(player, type);
                }
            } else {
                // Equipped slots
                int eqIdx = -1;
                if (slot == 10) eqIdx = 0;
                else if (slot == 12) eqIdx = 1;
                else if (slot == 14) eqIdx = 2;
                else if (slot == 16) eqIdx = 3;
                
                if (eqIdx != -1) {
                    AmuletType equipped = profile.getEquippedAmulets()[eqIdx];
                    if (equipped != null) {
                        profile.getEquippedAmulets()[eqIdx] = null;
                        profile.recalculateStats();
                        openAmuletMenu(player);
                    }
                }
            }
        } else if (e.getInventory().getHolder() instanceof AmuletUpgradeHolder) {
            e.setCancelled(true);
            if (e.getCurrentItem() == null) return;
            AmuletUpgradeHolder holder = (AmuletUpgradeHolder) e.getInventory().getHolder();
            Player player = (Player) e.getWhoClicked();
            
            if (e.getSlot() == 13) {
                tryUpgrade(player, holder.getType());
            }
        }
    }

    private void equipAmulet(Player player, PlayerProfile profile, AmuletType type) {
        // Check if already equipped
        for (AmuletType eq : profile.getEquippedAmulets()) {
            if (eq == type) {
                player.sendMessage("§cЭтот амулет уже надет!");
                return;
            }
        }
        
        // Find free slot
        for (int i = 0; i < 4; i++) {
            if (profile.getEquippedAmulets()[i] == null) {
                profile.getEquippedAmulets()[i] = type;
                profile.recalculateStats();
                openAmuletMenu(player);
                return;
            }
        }
        player.sendMessage("§cВсе слоты заняты!");
    }

    private void openUpgradeMenu(Player player, AmuletType type) {
        PlayerProfile profile = plugin.getPlayerRepository().getProfile(player);
        int level = profile.getOwnedAmulets().get(type);
        AmuletRarity rarity = type.getRarity();
        
        Inventory inv = Bukkit.createInventory(new AmuletUpgradeHolder(type), 27, "§6Улучшение амулета");
        
        ItemStack amuletItem = new Amulet(type, level).createItem();
        inv.setItem(11, amuletItem);
        
        ItemStack upgrade = new ItemStack(Material.ANVIL);
        ItemMeta meta = upgrade.getItemMeta();
        if (meta != null) {
            meta.setDisplayName("§aУлучшить уровень");
            List<String> lore = new ArrayList<>();
            if (level < rarity.getMaxLevel()) {
                int cost = rarity.getUpgradeCostForLevel(level);
                int current = profile.getCurrency(rarity);
                lore.add("§7Цена: §f" + cost + " " + rarity.getDisplayName() + " Пыль");
                lore.add("§7У вас: §e" + current);
                lore.add("");
                if (current >= cost) lore.add("§eНажмите для улучшения!");
                else lore.add("§cНедостаточно пыли!");
            } else {
                lore.add("§aМаксимальный уровень!");
            }
            meta.setLore(lore);
            upgrade.setItemMeta(meta);
        }
        inv.setItem(13, upgrade);
        
        player.openInventory(inv);
    }

    private void tryUpgrade(Player player, AmuletType type) {
        PlayerProfile profile = plugin.getPlayerRepository().getProfile(player);
        int level = profile.getOwnedAmulets().get(type);
        AmuletRarity rarity = type.getRarity();
        
        if (level >= rarity.getMaxLevel()) return;
        
        int cost = rarity.getUpgradeCostForLevel(level);
        if (profile.getCurrency(rarity) >= cost) {
            profile.addCurrency(rarity, -cost);
            profile.getOwnedAmulets().put(type, level + 1);
            profile.recalculateStats();
            player.sendMessage("§aАмулет улучшен до " + (level + 1) + " уровня!");
            openUpgradeMenu(player, type);
        } else {
            player.sendMessage("§cНедостаточно пыли!");
        }
    }

    private AmuletType getAmuletTypeFromItem(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return null;
        String name = ChatColor.stripColor(item.getItemMeta().getDisplayName());
        for (AmuletType type : AmuletType.values()) {
            if (name.contains(type.getDisplayName())) return type;
        }
        return null;
    }
}
