package dev.yournick.mobarena.command;

import dev.yournick.mobarena.MobArenaPlugin;
import dev.yournick.mobarena.perk.Perk;
import dev.yournick.mobarena.player.PlayerProfile;
import dev.yournick.mobarena.perk.PlayerPerk;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public class PerkSelectCommand implements CommandExecutor, Listener {

    private final MobArenaPlugin plugin;

    public PerkSelectCommand(MobArenaPlugin plugin) {
        this.plugin = plugin;
    }

    private static class PerkHolder implements InventoryHolder {
        @Override
        public Inventory getInventory() {
            return null;
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player)) return true;
        Player player = (Player) sender;
        openPerkGUI(player);
        return true;
    }

    public void openPerkGUI(Player player) {
        Inventory inv = Bukkit.createInventory(new PerkHolder(), 9, "§6Выберите перк");

        PlayerProfile profile = plugin.getPlayerRepository().getProfile(player);
        if (profile == null) return;

        for (Perk perk : Perk.values()) {
            ItemStack item = new ItemStack(perk.getIcon());
            // Специальная обработка для Пирата (зелье огнестойкости)
            if (perk == Perk.PIRATE) {
                org.bukkit.inventory.meta.PotionMeta pm = (org.bukkit.inventory.meta.PotionMeta) item.getItemMeta();
                pm.setMainEffect(org.bukkit.potion.PotionEffectType.FIRE_RESISTANCE);
                item.setItemMeta(pm);
            }
            
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                meta.setDisplayName(ChatColor.GREEN + perk.getDisplayName());

                List<String> lore = new ArrayList<>();
                switch (perk) {
                    case LUCKY:
                        lore.add(ChatColor.YELLOW + "• Золото +40.0%, Удача +50.0%");
                        lore.add(ChatColor.YELLOW + "• Двойной дроп 10.0%, Редкий дроп 5.0%");
                        break;
                    case TOUGH:
                        lore.add(ChatColor.YELLOW + "• Броня +20.0% (снижение урона)");
                        lore.add(ChatColor.YELLOW + "• Здоровье +20.0% и скорость -15.0%");
                        break;
                    case AGILE:
                        lore.add(ChatColor.YELLOW + "• 35.0% базовый шанс уклониться от удара");
                        lore.add(ChatColor.YELLOW + "• Скорость +20.0% и Шанс крита +15.0%");
                        break;
                    case FIREBORN:
                        lore.add(ChatColor.YELLOW + "• Огнестойкость (100.0% защита от огня)");
                        lore.add(ChatColor.YELLOW + "• Урон огнем +10.0% и Fire Aspect II");
                        break;
                    case PIRATE:
                        lore.add(ChatColor.YELLOW + "• Урон +15.0%, Вампиризм 5.0% и Регенерация +20.0%");
                        lore.add(ChatColor.YELLOW + "• Штраф: +10.0% входящего урона");
                        break;
                    case BLACKSMITH:
                        lore.add(ChatColor.YELLOW + "• Скидка 35.0% на все улучшения в кузнице");
                        lore.add(ChatColor.YELLOW + "• Броня +10.0% и +20.0% сопр. откидыванию");
                        break;
                    case TRADER:
                        lore.add(ChatColor.YELLOW + "• +50.0% к цене продажи предметов скупщику");
                        lore.add(ChatColor.YELLOW + "• Скидка 25.0% на товары в магазине");
                        lore.add(ChatColor.YELLOW + "• Штраф: -20.0% золота за убийство мобов");
                        break;
                }
                lore.add("");
                if (profile.getPlayerPerk().getPerk() == null) {
                    lore.add(ChatColor.GRAY + "Цена выбора: " + ChatColor.GREEN + "БЕСПЛАТНО");
                } else {
                    lore.add(ChatColor.GRAY + "Цена смены: " + ChatColor.GOLD + "2500 монет");
                }
                lore.add("");
                lore.add(ChatColor.AQUA + "ЛКМ " + ChatColor.GRAY + "- Выбрать класс");
                lore.add(ChatColor.YELLOW + "ПКМ " + ChatColor.GRAY + "- Просмотр прокачек");

                meta.setLore(lore);
                item.setItemMeta(meta);
            }
            inv.addItem(item);
        }

        player.openInventory(inv);
    }

    @org.bukkit.event.EventHandler
    public void handleClick(org.bukkit.event.inventory.InventoryClickEvent e) {
        if (!(e.getInventory().getHolder() instanceof PerkHolder)) return;

        e.setCancelled(true);

        if (!(e.getWhoClicked() instanceof Player)) return;
        Player player = (Player) e.getWhoClicked();

        PlayerProfile profile = plugin.getPlayerRepository().getProfile(player);
        if (profile == null) return;

        ItemStack clicked = e.getCurrentItem();
        if (clicked == null || !clicked.hasItemMeta()) return;

        String name = clicked.getItemMeta().getDisplayName();
        if (name == null) return;

        String strippedName = ChatColor.stripColor(name);

        for (Perk perk : Perk.values()) {
            if (perk.getDisplayName().equals(strippedName)) {
                if (e.getClick() == org.bukkit.event.inventory.ClickType.LEFT) {
                    Perk currentPerk = profile.getPlayerPerk().getPerk();
                    
                    // Проверка текущего перка
                    if (currentPerk == perk) {
                        player.sendMessage(ChatColor.RED + "Этот перк уже выбран!");
                        return;
                    }

                    // Если перк уже есть, то смена за 2500 монет
                    if (currentPerk != null) {
                        if (profile.getGold() < 2500) {
                            player.sendMessage(ChatColor.RED + "Недостаточно золота для смены перка (нужно 2500)!");
                            return;
                        }
                        profile.removeGold(2500);
                        player.sendMessage(ChatColor.GREEN + "Вы сменили перк на: " + perk.getDisplayName() + " за 2500 монет!");
                    } else {
                        player.sendMessage(ChatColor.GREEN + "Вы выбрали свой первый перк: " + perk.getDisplayName() + "!");
                    }

                    profile.getPlayerPerk().setPerk(perk);
                    profile.recalculateStats();
                    player.closeInventory();
                } else if (e.getClick() == org.bukkit.event.inventory.ClickType.RIGHT) {
                    // Открыть меню прокачек этого перка
                    player.performCommand("perk_upgrade " + perk.name());
                }
                break;
            }
        }
    }
}
