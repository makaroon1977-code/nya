package dev.yournick.mobarena.listener;

import dev.yournick.mobarena.MobArenaPlugin;
import dev.yournick.mobarena.player.PlayerProfile;
import dev.yournick.mobarena.player.PlayerRepository;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class PlayerJoinQuitListener implements Listener {

    private final PlayerRepository repository;

    public PlayerJoinQuitListener(PlayerRepository repository) {
        this.repository = repository;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        // создаём профиль при заходе
        PlayerProfile profile = repository.getProfile(e.getPlayer());
        MobArenaPlugin.getInstance().getDatabaseManager().loadProfile(profile);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        // Убираем с арены, если он там
        MobArenaPlugin.getInstance().getArenaManager().leave(e.getPlayer());

        // сохраняем и удаляем профиль из памяти
        PlayerProfile profile = repository.getProfile(e.getPlayer());
        MobArenaPlugin.getInstance().getDatabaseManager().saveProfile(profile);
        repository.removeProfile(e.getPlayer());
    }
}
