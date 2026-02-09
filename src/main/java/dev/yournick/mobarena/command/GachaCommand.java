package dev.yournick.mobarena.command;

import dev.yournick.mobarena.MobArenaPlugin;
import dev.yournick.mobarena.amulet.AmuletManager;
import dev.yournick.mobarena.amulet.AmuletRarity;
import dev.yournick.mobarena.amulet.AmuletType;
import dev.yournick.mobarena.amulet.GachaAnimation;
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
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public class GachaCommand implements CommandExecutor, Listener {

    private final MobArenaPlugin plugin;
    private final AmuletManager amuletManager;

    public GachaCommand(MobArenaPlugin plugin, AmuletManager amuletManager) {
        this.plugin = plugin;
        this.amuletManager = amuletManager;
        Bukkit.getScheduler().runTaskLater(plugin, this::setupChest, 20L);
    }

    private void setupChest() {
        String worldName = plugin.getConfig().getString("gacha.chest.world", "world");
        org.bukkit.World world = Bukkit.getWorld(worldName);
        if (world == null) return;

        int x = plugin.getConfig().getInt("gacha.chest.x");
        int y = plugin.getConfig().getInt("gacha.chest.y");
        int z = plugin.getConfig().getInt("gacha.chest.z");

        org.bukkit.block.Block block = world.getBlockAt(x, y, z);
        if (block.getType() != Material.CHEST) {
            block.setType(Material.CHEST);
        }
    }

    public static class GachaHolder implements InventoryHolder {
        @Override public Inventory getInventory() { return null; }
    }

    public static class DropListHolder implements InventoryHolder {
        @Override public Inventory getInventory() { return null; }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) return true;
        openGachaMenu((Player) sender);
        return true;
    }

    public void openGachaMenu(Player player) {
        Inventory inv = Bukkit.createInventory(new GachaHolder(), 27, "§6Гача амулетов");
        PlayerProfile profile = plugin.getPlayerRepository().getProfile(player);
        
        // Кейс 1
        ItemStack case1 = new ItemStack(Material.CHEST);
        ItemMeta meta = case1.getItemMeta();
        if (meta != null) {
            meta.setDisplayName("§eОбычный кейс амулетов");
            List<String> lore = new ArrayList<>();
            lore.add("§7Цена: §b10 Магических осколков");
            lore.add("");
            lore.add("§7У вас: §b" + (profile != null ? profile.getShards() : 0) + " осколков");
            lore.add("");
            lore.add("§aЛКМ §7- Открыть");
            lore.add("§eПКМ §7- Список дропа");
            meta.setLore(lore);
            case1.setItemMeta(meta);
        }
        inv.setItem(11, case1);

        // Кейс 2 (Заготовка)
        ItemStack case2 = new ItemStack(Material.ENDER_CHEST);
        ItemMeta meta2 = case2.getItemMeta();
        if (meta2 != null) {
            meta2.setDisplayName("§bРедкий кейс амулетов");
            List<String> lore = new ArrayList<>();
            lore.add("§cВ разработке...");
            meta2.setLore(lore);
            case2.setItemMeta(meta2);
        }
        inv.setItem(13, case2);

        // Кейс 3 (Заготовка)
        ItemStack case3 = new ItemStack(Material.TRAPPED_CHEST);
        ItemMeta meta3 = case3.getItemMeta();
        if (meta3 != null) {
            meta3.setDisplayName("§dЭпический кейс амулетов");
            List<String> lore = new ArrayList<>();
            lore.add("§cВ разработке...");
            meta3.setLore(lore);
            case3.setItemMeta(meta3);
        }
        inv.setItem(15, case3);

        player.openInventory(inv);
    }

    @EventHandler
    public void onChestInteract(PlayerInteractEvent e) {
        if (e.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        if (e.getClickedBlock() == null || e.getClickedBlock().getType() != Material.CHEST) return;

        double x = plugin.getConfig().getDouble("gacha.chest.x");
        double y = plugin.getConfig().getDouble("gacha.chest.y");
        double z = plugin.getConfig().getDouble("gacha.chest.z");

        if (e.getClickedBlock().getX() == (int)x && 
            e.getClickedBlock().getY() == (int)y && 
            e.getClickedBlock().getZ() == (int)z) {
            e.setCancelled(true);
            openGachaMenu(e.getPlayer());
        }
    }

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        if (e.getInventory().getHolder() instanceof GachaHolder) {
            e.setCancelled(true);
            if (e.getCurrentItem() == null) return;
            
            Player player = (Player) e.getWhoClicked();
            if (e.getSlot() == 11) {
                if (e.getClick() == ClickType.LEFT) {
                    rollCase(player, 10);
                } else if (e.getClick() == ClickType.RIGHT) {
                    openDropList(player);
                }
            }
        } else if (e.getInventory().getHolder() instanceof DropListHolder) {
            e.setCancelled(true);
        }
    }

    private void rollCase(Player player, int cost) {
        PlayerProfile profile = plugin.getPlayerRepository().getProfile(player);
        if (profile.getShards() < cost) {
            player.sendMessage("§cНедостаточно магических осколков!");
            return;
        }
        profile.removeShards(cost);
        
        AmuletType result = amuletManager.rollAmulet(profile);
        new GachaAnimation(plugin, player, result, amuletManager).start();
    }

    private void openDropList(Player player) {
        Inventory inv = Bukkit.createInventory(new DropListHolder(), 54, "§6Список дропа: Обычный кейс");
        
        // Info Item
        ItemStack info = new ItemStack(Material.PAPER);
        ItemMeta infoMeta = info.getItemMeta();
        if (infoMeta != null) {
            infoMeta.setDisplayName("§eШансы:");
            List<String> lore = new ArrayList<>();
            lore.add("§fОбычный: §770%");
            lore.add("§9Редкий: §720%");
            lore.add("§dЭпический: §78%");
            lore.add("§6Легендарный: §72%");
            lore.add("");
            lore.add("§7Каждые 10 круток: §aГарант Эпик");
            lore.add("§7Каждые 10 круток: §e+3% к Легендарке");
            lore.add("§7Крутка 40: §6Гарант Легендарка");
            infoMeta.setLore(lore);
            info.setItemMeta(infoMeta);
        }
        inv.setItem(4, info);

        int slot = 9;
        for (AmuletType type : AmuletType.values()) {
            if (slot >= 54) break;
            
            ItemStack item = new ItemStack(type.getMaterial());
            // Специальная обработка для визуализации зелий в списке
            if (type == AmuletType.REGEN) {
                org.bukkit.inventory.meta.PotionMeta pm = (org.bukkit.inventory.meta.PotionMeta) item.getItemMeta();
                pm.setMainEffect(org.bukkit.potion.PotionEffectType.REGENERATION);
                item.setItemMeta(pm);
            } else if (type == AmuletType.FORTUNE) {
                org.bukkit.inventory.meta.PotionMeta pm = (org.bukkit.inventory.meta.PotionMeta) item.getItemMeta();
                pm.setMainEffect(org.bukkit.potion.PotionEffectType.LUCK);
                item.setItemMeta(pm);
            }

            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                // Свечение для критов и титана
                if (type == AmuletType.CRIT || type == AmuletType.TITAN) {
                    meta.addEnchant(org.bukkit.enchantments.Enchantment.DURABILITY, 1, true);
                    meta.addItemFlags(org.bukkit.inventory.ItemFlag.HIDE_ENCHANTS);
                }

                AmuletRarity rarity = type.getRarity();
                meta.setDisplayName(rarity.getColor() + type.getDisplayName());
                List<String> lore = new ArrayList<>();
                lore.add(rarity.getColor() + rarity.getDisplayName());
                lore.add("");
                lore.add("§7Базовые статы:");
                for (java.util.Map.Entry<String, Double> entry : type.getBaseStats().entrySet()) {
                    String key = entry.getKey();
                    String statName = AmuletType.formatStatName(key);
                    double val = entry.getValue();
                    String sign = val >= 0 ? "+" : "";
                    
                    String formatted;
                    String unit;
                    if (AmuletType.isPercent(key)) {
                        formatted = String.format("%.1f", val * 100);
                        unit = "%";
                    } else {
                        formatted = String.format("%.1f", val);
                        unit = " HP";
                    }
                    
                    lore.add("§e• " + statName + ": " + ChatColor.GREEN + sign + formatted + unit);
                }

                // Уникальные баффы легендарок
                if (rarity == AmuletRarity.LEGENDARY) {
                    lore.add("");
                    lore.add("§6Уникальный эффект:");
                    switch (type) {
                        case TITAN: lore.add("§7Непробиваемый щит при <30% HP"); break;
                        case DESTRUCTION: lore.add("§7Взрывной удар каждые 10с (AoE)"); break;
                        case IMMORTALITY: lore.add("§7Призрачная форма при <10% HP"); break;
                        case STORM: lore.add("§7Цепная молния на 6-ю атаку"); break;
                        case ETERNAL_FIRE: lore.add("§7Пламенная аура + Взрыв при <20% HP"); break;
                    }
                }

                meta.setLore(lore);
                item.setItemMeta(meta);
            }
            inv.setItem(slot++, item);
        }
        
        player.openInventory(inv);
    }
}
