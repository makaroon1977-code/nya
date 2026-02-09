package dev.yournick.mobarena.player;

import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PlayerRepository {

    private final Map<UUID, PlayerProfile> profiles = new HashMap<>();

    public PlayerProfile getProfile(Player player) {
        // Создаём профиль с uuid
        return profiles.computeIfAbsent(player.getUniqueId(), uuid -> new PlayerProfile(uuid));
    }

    public void removeProfile(Player player) {
        profiles.remove(player.getUniqueId());
    }
}
