package dev.yournick.mobarena.command;

import dev.yournick.mobarena.MobArenaPlugin;
import dev.yournick.mobarena.arena.ArenaLoot;
import dev.yournick.mobarena.player.PlayerProfile;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class SellCommand implements CommandExecutor {

    private final MobArenaPlugin plugin;

    public SellCommand(MobArenaPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) return true;
        Player player = (Player) sender;

        PlayerProfile profile = plugin.getPlayerRepository().getProfile(player);
        if (profile == null) return true;

        int totalGold = 0;
        int itemsSold = 0;

        ItemStack[] contents = player.getInventory().getContents();
        for (int i = 0; i < contents.length; i++) {
            ItemStack item = contents[i];
            int price = ArenaLoot.getPrice(item);
            
            if (price > 0) {
                int amount = item.getAmount();
                totalGold += price * amount;
                itemsSold += amount;
                
                // Удаляем предмет из инвентаря
                player.getInventory().setItem(i, null);
            }
        }

        if (itemsSold > 0) {
            // Бонусы торговца и статов
            profile.recalculateStats();
            totalGold = (int) (totalGold * profile.getStats().getSellPricePercent());
            
            // 2. Оптовик (крупный апгрейд 1)
            if (profile.getPerkProfile(dev.yournick.mobarena.perk.Perk.TRADER).hasLarge1()) {
                double wholesaleBonus = 1.0;
                String tier = "";
                if (itemsSold >= 20) { wholesaleBonus = 1.5; tier = "Оптовый гигант"; }
                else if (itemsSold >= 15) { wholesaleBonus = 1.3; tier = "Крупный оптовик"; }
                else if (itemsSold >= 10) { wholesaleBonus = 1.2; tier = "Средний оптовик"; }
                else if (itemsSold >= 5) { wholesaleBonus = 1.1; tier = "Начинающий оптовик"; }
                
                if (wholesaleBonus > 1.0) {
                    totalGold = (int) (totalGold * wholesaleBonus);
                    player.sendMessage(ChatColor.GOLD + "Бонус оптовика (" + tier + "): " + ChatColor.YELLOW + "+" + (int)((wholesaleBonus-1)*100) + "% золота!");
                }
            }

            profile.addGold(totalGold);
            player.sendMessage(ChatColor.GREEN + "Вы продали " + ChatColor.YELLOW + itemsSold + 
                    ChatColor.GREEN + " предметов за " + ChatColor.GOLD + totalGold + " золота!");
        } else {
            player.sendMessage(ChatColor.RED + "У вас нет предметов арены для продажи!");
        }

        return true;
    }
}
