package dev.yournick.mobarena.command;

import dev.yournick.mobarena.MobArenaPlugin;
import dev.yournick.mobarena.arena.ArenaManager;
import dev.yournick.mobarena.player.PlayerProfile;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class ArenaCommand implements CommandExecutor {

    private final ArenaManager arena;

    public ArenaCommand(ArenaManager arena) {
        this.arena = arena;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player)) return true;
        Player player = (Player) sender;

        PlayerProfile profile = MobArenaPlugin.getInstance().getPlayerRepository().getProfile(player);
        if (profile == null) {
            player.sendMessage("§cОшибка: профиль не найден!");
            return true;
        }

        if (args.length == 0) return false;

        switch (args[0].toLowerCase()) {
            case "join":
                arena.join(player);
                break;

            case "leave":
                arena.leave(player);
                break;

            case "start":
                arena.startWave();
                break;

            case "upgrade":
                if (args.length < 2) return false;
                switch (args[1].toLowerCase()) {
                    case "damage": // Strength
                        if (profile.buyStrengthUpgrade(10)) {
                            arena.applyPlayerEffects(player);
                            player.sendMessage("§aСила увеличена!");
                        } else player.sendMessage("§cНедостаточно золота!");
                        break;
                    case "health":
                        if (profile.buyHealthUpgrade(10, 2)) {
                            arena.applyPlayerEffects(player);
                            player.sendMessage("§aЗдоровье увеличено!");
                        } else player.sendMessage("§cНедостаточно золота!");
                        break;
                    case "speed":
                        if (profile.buySpeedUpgrade(10, 0.1)) {
                            arena.applyPlayerEffects(player);
                            player.sendMessage("§aСкорость увеличена!");
                        } else player.sendMessage("§cНедостаточно золота!");
                        break;
                }
                break;

            case "coins":
            case "gold":
                player.sendMessage("§eУ тебя золота: §6" + profile.getGold());
                break;

            default:
                player.sendMessage("§cНеизвестная команда!");
        }

        return true;
    }
}
