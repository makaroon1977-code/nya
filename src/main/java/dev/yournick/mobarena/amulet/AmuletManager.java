package dev.yournick.mobarena.amulet;

import dev.yournick.mobarena.MobArenaPlugin;
import dev.yournick.mobarena.player.PlayerProfile;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public class AmuletManager {

    private final MobArenaPlugin plugin;
    private final Random random = new Random();

    public AmuletManager(MobArenaPlugin plugin) {
        this.plugin = plugin;
    }

    public AmuletType rollAmulet(PlayerProfile profile) {
        profile.setGachaTotalRolls(profile.getGachaTotalRolls() + 1);
        profile.setRollsSinceLastEpic(profile.getRollsSinceLastEpic() + 1);
        profile.setRollsSinceLastLegendary(profile.getRollsSinceLastLegendary() + 1);

        AmuletRarity rarity = determineRarity(profile);
        
        // Reset counters based on rarity
        if (rarity == AmuletRarity.LEGENDARY) {
            profile.setRollsSinceLastLegendary(0);
            profile.setRollsSinceLastEpic(0); // Legendary counts as epic too for pity
            // Should total rolls reset? User didn't say. Usually total rolls in gacha is lifetime.
            // "+3% к шансу легендарного каждые 10 круток" - I'll reset this bonus too?
            // "каждые 10 круток" implies it grows. I'll reset it to 0 only if I want the chance to reset.
            profile.setGachaTotalRolls(0); 
        } else if (rarity == AmuletRarity.EPIC) {
            profile.setRollsSinceLastEpic(0);
        }

        return getRandomAmuletOfRarity(rarity);
    }

    private AmuletRarity determineRarity(PlayerProfile profile) {
        if (profile.getRollsSinceLastLegendary() >= 40) return AmuletRarity.LEGENDARY;
        if (profile.getRollsSinceLastEpic() >= 10) return AmuletRarity.EPIC;

        double legendChance = AmuletRarity.LEGENDARY.getBaseChance() + (profile.getGachaTotalRolls() / 10) * 0.03;
        double epicChance = AmuletRarity.EPIC.getBaseChance();
        double rareChance = AmuletRarity.RARE.getBaseChance();

        double roll = random.nextDouble();

        if (roll < legendChance) return AmuletRarity.LEGENDARY;
        if (roll < legendChance + epicChance) return AmuletRarity.EPIC;
        if (roll < legendChance + epicChance + rareChance) return AmuletRarity.RARE;
        
        return AmuletRarity.COMMON;
    }

    private AmuletType getRandomAmuletOfRarity(AmuletRarity rarity) {
        List<AmuletType> types = new ArrayList<>();
        for (AmuletType type : AmuletType.values()) {
            if (type.getRarity() == rarity) types.add(type);
        }
        Collections.shuffle(types);
        return types.get(0);
    }

    public void processGachaResult(Player player, AmuletType rolledType) {
        PlayerProfile profile = plugin.getPlayerRepository().getProfile(player);
        if (profile == null) return;

        player.sendMessage("§7Вам выпал: " + rolledType.getRarity().getColor() + rolledType.getDisplayName());
        
        if (profile.getOwnedAmulets().containsKey(rolledType)) {
            // Duplicate
            AmuletRarity rarity = rolledType.getRarity();
            profile.addCurrency(rarity, 1);
            player.sendMessage("§eДубликат! §7Получена валюта: " + rarity.getColor() + rarity.getDisplayName() + " Пыль §ax1");
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_PLING, 1f, 1f);
        } else {
            // New amulet
            profile.getOwnedAmulets().put(rolledType, 1);
            player.sendMessage("§aНовый амулет добавлен в коллекцию!");
            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1f);
        }
    }
}
