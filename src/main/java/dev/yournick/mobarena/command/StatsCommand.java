package dev.yournick.mobarena.command;

import dev.yournick.mobarena.MobArenaPlugin;
import dev.yournick.mobarena.player.PlayerProfile;
import dev.yournick.mobarena.player.PlayerStats;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class StatsCommand implements CommandExecutor {

    private final MobArenaPlugin plugin;

    public StatsCommand(MobArenaPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) return true;
        Player player = (Player) sender;
        PlayerProfile profile = plugin.getPlayerRepository().getProfile(player);
        if (profile == null) return true;

        profile.recalculateStats();
        PlayerStats stats = profile.getStats();

        player.sendMessage(ChatColor.GOLD + "=== Ваши характеристики ===");
        
        player.sendMessage(ChatColor.YELLOW + "Атака:");
        player.sendMessage(formatStat("Общий урон", stats.getDamagePercent() * 100, "%"));
        player.sendMessage(formatStat("Шанс крита", stats.getCritChance() * 100, "%"));
        player.sendMessage(formatStat("Крит. урон", stats.getCritDamage() * 100, "%"));
        player.sendMessage(formatStat("Скор. атаки", stats.getAttackSpeedPercent() * 100, "%"));
        player.sendMessage(formatStat("Вампиризм", stats.getLifesteal() * 100, "%"));
        player.sendMessage(formatStat("Урон огнем", stats.getFireDamagePercent() * 100, "%"));

        player.sendMessage(ChatColor.YELLOW + "Защита:");
        player.sendMessage(formatStat("Макс. здоровье", stats.getMaxHealthPercent() * 100, "%"));
        player.sendMessage(formatStat("Броня (Armor)", stats.getArmorPercent() * 100, "%"));
        player.sendMessage(formatStat("Уклонение", stats.getDodgeChance() * 100, "%"));
        player.sendMessage(formatStat("Отражение урона", stats.getDamageReflection() * 100, "%"));
        player.sendMessage(formatStat("Сопр. откидыванию", stats.getKnockbackResistance() * 100, "%"));
        player.sendMessage(formatStat("Защита от огня", stats.getFireResistance() * 100, "%"));

        player.sendMessage(ChatColor.YELLOW + "Поддержка:");
        player.sendMessage(formatStat("Хил за убийство", stats.getHealOnKill(), " HP"));
        player.sendMessage(formatStat("Сила регенерации", stats.getRegenPower() * 100, "%"));
        player.sendMessage(formatStat("Эффект лечения", stats.getHealReceived() * 100, "%"));
        player.sendMessage(formatStat("Скорость", stats.getMoveSpeedPercent() * 100, "%"));

        player.sendMessage(ChatColor.YELLOW + "Экономика:");
        player.sendMessage(formatStat("Удача (Luck)", stats.getLuck(), ""));
        player.sendMessage(formatStat("Шанс дропа", stats.getDropChance() * 100, "%"));
        player.sendMessage(formatStat("Двойной дроп", stats.getDoubleDropChance() * 100, "%"));
        player.sendMessage(formatStat("Бонус золота", stats.getGoldGainPercent() * 100, "%"));
        player.sendMessage(formatStat("Множитель цен", stats.getSellPricePercent() * 100, "%"));

        player.sendMessage(ChatColor.YELLOW + "Дополнительно:");
        player.sendMessage(formatStat("Золотых свиней убито", profile.getSpecialPigsKilled(), ""));
        player.sendMessage(formatStat("Лучшая волна", profile.getBestWave(), ""));

        return true;
    }

    private String formatStat(String name, double value, String suffix) {
        String valStr = String.format("%.1f", value);
        return ChatColor.GRAY + " • " + name + ": " + ChatColor.WHITE + valStr + suffix;
    }
}
