package dev.yournick.mobarena;

import dev.yournick.mobarena.arena.ArenaManager;
import dev.yournick.mobarena.amulet.AmuletManager;
import dev.yournick.mobarena.command.*;
import dev.yournick.mobarena.listener.*;
import dev.yournick.mobarena.npc.NPCManager;
import dev.yournick.mobarena.player.PlayerProfile;
import dev.yournick.mobarena.player.PlayerRepository;
import org.bukkit.NamespacedKey;
import org.bukkit.plugin.java.JavaPlugin;
import dev.yournick.mobarena.task.ActionBarTask;
import dev.yournick.mobarena.task.AmuletEffectTask;

public class MobArenaPlugin extends JavaPlugin {

    private static MobArenaPlugin instance;

    private NamespacedKey customItemKey;
    private NamespacedKey enchantsKey;

    private ArenaManager arenaManager;
    private AmuletManager amuletManager;
    private PlayerRepository playerRepository;
    private dev.yournick.mobarena.storage.DatabaseManager databaseManager;

    @Override
    public void onEnable() {
        instance = this;

        customItemKey = new NamespacedKey(this, "custom_item");
        enchantsKey = new NamespacedKey(this, "enchants");

        saveDefaultConfig();

        // БД
        databaseManager = new dev.yournick.mobarena.storage.DatabaseManager(this);
        databaseManager.init();

        // Репозиторий профилей игроков
        playerRepository = new PlayerRepository();

        // Менеджер арены
        arenaManager = new ArenaManager(this);

        // Амулеты
        amuletManager = new AmuletManager(this);

        // === Регистрация листенеров ===
        getServer().getPluginManager().registerEvents(new PlayerJoinQuitListener(playerRepository), this);
        getServer().getPluginManager().registerEvents(new MobDeathListener(arenaManager), this);
        getServer().getPluginManager().registerEvents(new MobDamageListener(arenaManager), this);
        getServer().getPluginManager().registerEvents(new PerkListener(this), this);
        getServer().getPluginManager().registerEvents(new EntityListener(this), this);
        getServer().getPluginManager().registerEvents(new ConsumableListener(this), this);

        // === Команды и GUI ===
        if (getCommand("arena") != null) {
            getCommand("arena").setExecutor(new ArenaCommand(arenaManager));
        }

        // Команда апгрейдов
        UpgradeCommand upgradeCommand = new UpgradeCommand(this);
        if (getCommand("upgrade") != null) {
            getCommand("upgrade").setExecutor(upgradeCommand);
        }
        if (getCommand("arena_upgrade") != null) {
            getCommand("arena_upgrade").setExecutor(upgradeCommand);
        }
        getServer().getPluginManager().registerEvents(upgradeCommand, this);

        // Команда магазина
        ShopCommand shopCommand = new ShopCommand(this);
        if (getCommand("shop") != null) {
            getCommand("shop").setExecutor(shopCommand);
        }
        if (getCommand("arena_shop") != null) {
            getCommand("arena_shop").setExecutor(shopCommand);
        }
        getServer().getPluginManager().registerEvents(shopCommand, this);

        // === Команда выбора перка ===
        PerkSelectCommand perkCommand = new PerkSelectCommand(this);
        if (getCommand("perk") != null) {
            getCommand("perk").setExecutor(perkCommand);
        }
        getServer().getPluginManager().registerEvents(perkCommand, this);

        // === Команда прокачки перков ===
        PerkUpgradeCommand perkUpgradeCommand = new PerkUpgradeCommand(this);
        if (getCommand("perk_upgrade") != null) {
            getCommand("perk_upgrade").setExecutor(perkUpgradeCommand);
        }
        getServer().getPluginManager().registerEvents(perkUpgradeCommand, this);

        // === Команда продажи ===
        if (getCommand("arena_sell") != null) {
            getCommand("arena_sell").setExecutor(new SellCommand(this));
        }

        // === Команда характеристик ===
        if (getCommand("stats") != null) {
            getCommand("stats").setExecutor(new StatsCommand(this));
        }
        DetailedStatsCommand detailedStatsCommand = new DetailedStatsCommand(this);
        if (getCommand("detailedstats") != null) {
            getCommand("detailedstats").setExecutor(detailedStatsCommand);
        }
        getServer().getPluginManager().registerEvents(detailedStatsCommand, this);

        // === Амулеты ===
        AmuletCommand amuletCommand = new AmuletCommand(this);
        if (getCommand("amulet") != null) getCommand("amulet").setExecutor(amuletCommand);
        getServer().getPluginManager().registerEvents(amuletCommand, this);

        GachaCommand gachaCommand = new GachaCommand(this, amuletManager);
        if (getCommand("gacha") != null) getCommand("gacha").setExecutor(gachaCommand);
        getServer().getPluginManager().registerEvents(gachaCommand, this);

        // Инициализация NPC
        new NPCManager(this);

        getLogger().info("MobArenaPlus enabled");
        new ActionBarTask(this).runTaskTimer(this, 20L, 5L); // Ускоряем обновление до 4 раз в секунду
        new dev.yournick.mobarena.task.PerkEffectTask(this).runTaskTimer(this, 20L, 5L);
        new AmuletEffectTask(this).runTaskTimer(this, 20L, 20L); // Раз в секунду достаточно
        new dev.yournick.mobarena.task.MobAbilityTask(this).runTaskTimer(this, 20L, 10L); // Каждые 0.5с для способностей мобов
        
        // Таск для периодического обновления атрибутов игроков (раз в 2 секунды)
        new org.bukkit.scheduler.BukkitRunnable() {
            @Override
            public void run() {
                for (org.bukkit.entity.Player player : org.bukkit.Bukkit.getOnlinePlayers()) {
                    arenaManager.applyPlayerEffects(player);
                }
            }
        }.runTaskTimer(this, 60L, 40L);
    }

    @Override
    public void onDisable() {
        if (playerRepository != null && databaseManager != null) {
            for (org.bukkit.entity.Player player : org.bukkit.Bukkit.getOnlinePlayers()) {
                PlayerProfile profile = playerRepository.getProfile(player);
                if (profile != null) {
                    databaseManager.saveProfile(profile);
                }
            }
        }
        getLogger().info("MobArenaPlus disabled");
    }

    public static MobArenaPlugin getInstance() {
        return instance;
    }

    public PlayerRepository getPlayerRepository() {
        return playerRepository;
    }

    public ArenaManager getArenaManager() {
        return arenaManager;
    }

    public dev.yournick.mobarena.storage.DatabaseManager getDatabaseManager() {
        return databaseManager;
    }

    public NamespacedKey getCustomItemKey() {
        return customItemKey;
    }

    public NamespacedKey getEnchantsKey() {
        return enchantsKey;
    }
}
