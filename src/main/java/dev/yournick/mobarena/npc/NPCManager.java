package dev.yournick.mobarena.npc;

import dev.yournick.mobarena.MobArenaPlugin;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEntityEvent;

import org.bukkit.metadata.FixedMetadataValue;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class NPCManager implements Listener {

    private final MobArenaPlugin plugin;
    private final Map<UUID, NPCType> npcMap = new HashMap<>();

    public enum NPCType {
        SHOP,
        UPGRADE,
        PERK,
        SELL,
        PERK_UPGRADE,
        JOIN,
        LEAVE,
        START_WAVE,
        AMULET,
        DETAILED_STATS
    }

    public NPCManager(MobArenaPlugin plugin) {
        this.plugin = plugin;

        Bukkit.getScheduler().runTask(plugin, this::spawnNPCs);
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    private void spawnNPCs() {
        cleanupOldNPCs();

        spawnNPC(
                getLocation("arena.npcs.shop"),
                "§aМагазин",
                NPCType.SHOP
        );

        spawnNPC(
                getLocation("arena.npcs.upgrade"),
                "§bАпгрейды",
                NPCType.UPGRADE
        );

        spawnNPC(
                getLocation("arena.npcs.perk"),
                "§eВыбор перка",
                NPCType.PERK
        );

        spawnNPC(
                getLocation("arena.npcs.sell"),
                "§6Скупщик",
                NPCType.SELL
        );

        spawnNPC(
                getLocation("arena.npcs.perk_upgrade"),
                "§dУлучшение перка",
                NPCType.PERK_UPGRADE
        );

        spawnNPC(
                getLocation("arena.npcs.join"),
                "§2Вход на арену",
                NPCType.JOIN
        );

        spawnNPC(
                getLocation("arena.npcs.leave"),
                "§cВыход с арены",
                NPCType.LEAVE
        );
        
        spawnNPC(
                getLocation("arena.npcs.start_wave"),
                "§6Старт волны",
                NPCType.START_WAVE
        );

        spawnNPC(
                getLocation("arena.npcs.amulet"),
                "§6Амулеты",
                NPCType.AMULET
        );

        spawnNPC(
                getLocation("arena.npcs.detailed_stats"),
                "§6Подробная статистика",
                NPCType.DETAILED_STATS
        );
    }

    private void cleanupOldNPCs() {
        for (org.bukkit.World world : Bukkit.getWorlds()) {
            for (Villager villager : world.getEntitiesByClass(Villager.class)) {
                if (villager.hasMetadata("mobarena_npc")) {
                    villager.remove();
                }
                // На всякий случай по имени, если метаданные не сохранились (хотя они не сохраняются при рестарте)
                else if (villager.getCustomName() != null && (
                        villager.getCustomName().equals("§aМагазин") ||
                        villager.getCustomName().equals("§bАпгрейды") ||
                        villager.getCustomName().equals("§eВыбор перка") ||
                        villager.getCustomName().equals("§6Скупщик") ||
                        villager.getCustomName().equals("§dУлучшение перка") ||
                        villager.getCustomName().equals("§2Вход на арену") ||
                        villager.getCustomName().equals("§cВыход с арены") ||
                        villager.getCustomName().equals("§6Старт волны") ||
                        villager.getCustomName().equals("§6Амулеты") ||
                        villager.getCustomName().equals("§6Подробная статистика")
                )) {
                    villager.remove();
                }
            }
        }
    }

    private Location getLocation(String path) {
        String worldName = plugin.getConfig().getString(path + ".world", "world");
        double x = plugin.getConfig().getDouble(path + ".x");
        double y = plugin.getConfig().getDouble(path + ".y");
        double z = plugin.getConfig().getDouble(path + ".z");

        return new Location(Bukkit.getWorld(worldName), x, y, z);
    }

    private void spawnNPC(Location loc, String name, NPCType type) {
        if (loc == null || loc.getWorld() == null) return;

        Villager npc = (Villager) loc.getWorld().spawnEntity(loc, EntityType.VILLAGER);

        npc.setCustomName(name);
        npc.setCustomNameVisible(true);
        npc.setAI(false);
        npc.setInvulnerable(true);
        npc.setCollidable(false);
        npc.setSilent(true);
        npc.setRemoveWhenFarAway(false);
        npc.setMetadata("mobarena_npc", new FixedMetadataValue(plugin, true));

        // 🔒 ВАЖНО — убираем торговлю
        npc.setProfession(Villager.Profession.FARMER);

        npcMap.put(npc.getUniqueId(), type);
    }

    @EventHandler
    public void onNPCClick(PlayerInteractEntityEvent event) {
        if (!(event.getRightClicked() instanceof Villager)) return;

        UUID id = event.getRightClicked().getUniqueId();
        NPCType type = npcMap.get(id);

        // Если в мапе нет, проверяем по метаданным/имени (для надежности после релоада)
        if (type == null) {
            if (event.getRightClicked().hasMetadata("mobarena_npc") || event.getRightClicked().getCustomName() != null) {
                String name = event.getRightClicked().getCustomName();
                if (name == null) return;
                
                if (name.equals("§aМагазин")) type = NPCType.SHOP;
                else if (name.equals("§bАпгрейды")) type = NPCType.UPGRADE;
                else if (name.equals("§eВыбор перка")) type = NPCType.PERK;
                else if (name.equals("§6Скупщик")) type = NPCType.SELL;
                else if (name.equals("§dУлучшение перка")) type = NPCType.PERK_UPGRADE;
                else if (name.equals("§2Вход на арену")) type = NPCType.JOIN;
                else if (name.equals("§cВыход с арены")) type = NPCType.LEAVE;
                else if (name.equals("§6Старт волны")) type = NPCType.START_WAVE;
                else if (name.equals("§6Амулеты")) type = NPCType.AMULET;
                else if (name.equals("§6Подробная статистика")) type = NPCType.DETAILED_STATS;

                if (type != null) {
                    npcMap.put(id, type);
                } else {
                    return;
                }
            } else {
                return;
            }
        }

        // ⛔ ОТМЕНЯЕМ СРАЗУ
        event.setCancelled(true);

        Player player = event.getPlayer();

        switch (type) {
            case SHOP:
                player.performCommand("arena_shop");
                break;
            case UPGRADE:
                player.performCommand("arena_upgrade");
                break;
            case PERK:
                player.performCommand("perk");
                break;
            case SELL:
                player.performCommand("arena_sell");
                break;
            case PERK_UPGRADE:
                player.performCommand("perk_upgrade");
                break;
            case JOIN:
                player.performCommand("arena join");
                break;
            case LEAVE:
                player.performCommand("arena leave");
                break;
            case START_WAVE:
                player.performCommand("arena start");
                break;
            case AMULET:
                player.performCommand("amulet");
                break;
            case DETAILED_STATS:
                player.performCommand("detailedstats");
                break;
        }
    }
}
