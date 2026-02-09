package dev.yournick.mobarena.task;

import dev.yournick.mobarena.MobArenaPlugin;
import dev.yournick.mobarena.player.PlayerProfile;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

public class ActionBarTask extends BukkitRunnable {

    private final MobArenaPlugin plugin;

    public ActionBarTask(MobArenaPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void run() {
        for (Player player : Bukkit.getOnlinePlayers()) {

            PlayerProfile profile = plugin.getPlayerRepository().getProfile(player);
            if (profile == null) continue;

            int gold = profile.getGold();
            int shards = profile.getShards();
            int wave = plugin.getArenaManager().getWave();

            String msg = "§6⚔ Волна: §e" + wave + " §7| §6💰 Золото: §e" + gold + " §7| §b✨ Осколки: §e" + shards;

            player.spigot().sendMessage(
                    ChatMessageType.ACTION_BAR,
                    new TextComponent(msg)
            );
        }
    }
}
