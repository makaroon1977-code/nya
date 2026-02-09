package dev.yournick.mobarena.storage;

import dev.yournick.mobarena.amulet.AmuletRarity;
import dev.yournick.mobarena.amulet.AmuletType;
import dev.yournick.mobarena.perk.Perk;
import dev.yournick.mobarena.player.PerkProfile;
import dev.yournick.mobarena.player.PlayerProfile;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import java.sql.*;
import java.util.Map;
import java.util.UUID;

public class DatabaseManager {

    private final JavaPlugin plugin;
    private Connection connection;

    public DatabaseManager(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void init() {
        try {
            // SQLite — файл в папке плагина
            if (!plugin.getDataFolder().exists()) plugin.getDataFolder().mkdirs();
            
            connection = DriverManager.getConnection(
                    "jdbc:sqlite:" + plugin.getDataFolder() + "/data.db"
            );

            // Создаём таблицы
            try (Statement st = connection.createStatement()) {
                st.executeUpdate(
                        "CREATE TABLE IF NOT EXISTS player_data (" +
                                "uuid TEXT PRIMARY KEY," +
                                "gold INTEGER," +
                                "best_wave INTEGER," +
                                "bonus_health REAL," +
                                "bonus_damage REAL," +
                                "bonus_speed REAL," +
                                "shards INTEGER DEFAULT 0," +
                                "wave_health_multiplier REAL DEFAULT 1.0," +
                                "pigs_killed INTEGER DEFAULT 0" +
                                ")"
                );
                try {
                    st.executeUpdate("ALTER TABLE player_data ADD COLUMN shards INTEGER DEFAULT 0");
                } catch (SQLException ignored) {}
                try {
                    st.executeUpdate("ALTER TABLE player_data ADD COLUMN wave_health_multiplier REAL DEFAULT 1.0");
                } catch (SQLException ignored) {}
                try {
                    st.executeUpdate("ALTER TABLE player_data ADD COLUMN pigs_killed INTEGER DEFAULT 0");
                } catch (SQLException ignored) {}
                st.executeUpdate(
                        "CREATE TABLE IF NOT EXISTS perk_data (" +
                                "uuid TEXT," +
                                "perk TEXT," +
                                "xp INTEGER," +
                                "mini1 INTEGER," +
                                "mini2 INTEGER," +
                                "mini3 INTEGER," +
                                "large1 INTEGER," +
                                "large2 INTEGER," +
                                "large3 INTEGER," +
                                "unique_unlocked INTEGER," +
                                "PRIMARY KEY (uuid, perk)" +
                                ")"
                );

                // Амулеты
                st.executeUpdate(
                        "CREATE TABLE IF NOT EXISTS player_amulet_data (" +
                                "uuid TEXT PRIMARY KEY," +
                                "gacha_rolls INTEGER DEFAULT 0," +
                                "rolls_epic INTEGER DEFAULT 0," +
                                "rolls_legendary INTEGER DEFAULT 0" +
                                ")"
                );
                st.executeUpdate(
                        "CREATE TABLE IF NOT EXISTS owned_amulets (" +
                                "uuid TEXT," +
                                "type TEXT," +
                                "level INTEGER," +
                                "PRIMARY KEY (uuid, type)" +
                                ")"
                );
                st.executeUpdate(
                        "CREATE TABLE IF NOT EXISTS equipped_amulets (" +
                                "uuid TEXT," +
                                "slot INTEGER," +
                                "type TEXT," +
                                "PRIMARY KEY (uuid, slot)" +
                                ")"
                );
                st.executeUpdate(
                        "CREATE TABLE IF NOT EXISTS amulet_currency (" +
                                "uuid TEXT," +
                                "rarity TEXT," +
                                "amount INTEGER," +
                                "PRIMARY KEY (uuid, rarity)" +
                                ")"
                );
            }
            Bukkit.getLogger().info("SQLite database initialized!");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void saveProfile(PlayerProfile profile) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                // Save main data
                try (PreparedStatement ps = connection.prepareStatement(
                        "INSERT OR REPLACE INTO player_data (uuid, gold, best_wave, bonus_health, bonus_damage, bonus_speed, shards, wave_health_multiplier, pigs_killed) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)")) {
                    ps.setString(1, profile.getUuid().toString());
                    ps.setInt(2, profile.getGold());
                    ps.setInt(3, profile.getBestWave());
                    ps.setDouble(4, profile.getBonusHealth());
                    ps.setDouble(5, (double) profile.getStrengthLevel());
                    ps.setDouble(6, profile.getBonusSpeed());
                    ps.setInt(7, profile.getShards());
                    ps.setDouble(8, profile.getWaveHealthMultiplier());
                    ps.setInt(9, profile.getSpecialPigsKilled());
                    ps.executeUpdate();
                }

                // Save perks
                for (Perk perk : Perk.values()) {
                    PerkProfile pp = profile.getPerkProfile(perk);
                    try (PreparedStatement ps = connection.prepareStatement(
                            "INSERT OR REPLACE INTO perk_data (uuid, perk, xp, mini1, mini2, mini3, large1, large2, large3, unique_unlocked) " +
                                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)")) {
                        ps.setString(1, profile.getUuid().toString());
                        ps.setString(2, perk.name());
                        ps.setInt(3, pp.getXp());
                        ps.setInt(4, pp.getMini1());
                        ps.setInt(5, pp.getMini2());
                        ps.setInt(6, pp.getMini3());
                        ps.setInt(7, pp.hasLarge1() ? 1 : 0);
                        ps.setInt(8, pp.hasLarge2() ? 1 : 0);
                        ps.setInt(9, pp.hasLarge3() ? 1 : 0);
                        ps.setInt(10, pp.hasUnique() ? 1 : 0);
                        ps.executeUpdate();
                    }
                }

                // Save Amulets
                try (PreparedStatement ps = connection.prepareStatement(
                        "INSERT OR REPLACE INTO player_amulet_data (uuid, gacha_rolls, rolls_epic, rolls_legendary) VALUES (?, ?, ?, ?)")) {
                    ps.setString(1, profile.getUuid().toString());
                    ps.setInt(2, profile.getGachaTotalRolls());
                    ps.setInt(3, profile.getRollsSinceLastEpic());
                    ps.setInt(4, profile.getRollsSinceLastLegendary());
                    ps.executeUpdate();
                }

                for (Map.Entry<AmuletType, Integer> entry : profile.getOwnedAmulets().entrySet()) {
                    try (PreparedStatement ps = connection.prepareStatement(
                            "INSERT OR REPLACE INTO owned_amulets (uuid, type, level) VALUES (?, ?, ?)")) {
                        ps.setString(1, profile.getUuid().toString());
                        ps.setString(2, entry.getKey().name());
                        ps.setInt(3, entry.getValue());
                        ps.executeUpdate();
                    }
                }

                for (int i = 0; i < profile.getEquippedAmulets().length; i++) {
                    AmuletType type = profile.getEquippedAmulets()[i];
                    if (type != null) {
                        try (PreparedStatement ps = connection.prepareStatement(
                                "INSERT OR REPLACE INTO equipped_amulets (uuid, slot, type) VALUES (?, ?, ?)")) {
                            ps.setString(1, profile.getUuid().toString());
                            ps.setInt(2, i);
                            ps.setString(3, type.name());
                            ps.executeUpdate();
                        }
                    }
                }

                for (Map.Entry<AmuletRarity, Integer> entry : profile.getAmuletCurrency().entrySet()) {
                    try (PreparedStatement ps = connection.prepareStatement(
                            "INSERT OR REPLACE INTO amulet_currency (uuid, rarity, amount) VALUES (?, ?, ?)")) {
                        ps.setString(1, profile.getUuid().toString());
                        ps.setString(2, entry.getKey().name());
                        ps.setInt(3, entry.getValue());
                        ps.executeUpdate();
                    }
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        });
    }

    public void loadProfile(PlayerProfile profile) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                // Load main data
                try (PreparedStatement ps = connection.prepareStatement("SELECT * FROM player_data WHERE uuid = ?")) {
                    ps.setString(1, profile.getUuid().toString());
                    ResultSet rs = ps.executeQuery();
                    if (rs.next()) {
                        profile.addGold(rs.getInt("gold"));
                        profile.addShards(rs.getInt("shards"));
                        profile.setBestWave(rs.getInt("best_wave"));
                        profile.setBonusHealth(rs.getDouble("bonus_health"));
                        profile.setStrengthLevel((int) rs.getDouble("bonus_damage"));
                        profile.setBonusSpeed(rs.getDouble("bonus_speed"));
                        profile.setWaveHealthMultiplier(rs.getDouble("wave_health_multiplier"));
                        profile.setSpecialPigsKilled(rs.getInt("pigs_killed"));
                    }
                }

                // Load perks
                try (PreparedStatement ps = connection.prepareStatement("SELECT * FROM perk_data WHERE uuid = ?")) {
                    ps.setString(1, profile.getUuid().toString());
                    ResultSet rs = ps.executeQuery();
                    while (rs.next()) {
                        try {
                            Perk perk = Perk.valueOf(rs.getString("perk"));
                            PerkProfile pp = profile.getPerkProfile(perk);
                            pp.setXp(rs.getInt("xp"));
                            pp.setMini1(rs.getInt("mini1"));
                            pp.setMini2(rs.getInt("mini2"));
                            pp.setMini3(rs.getInt("mini3"));
                            pp.setLarge1(rs.getInt("large1") == 1);
                            pp.setLarge2(rs.getInt("large2") == 1);
                            pp.setLarge3(rs.getInt("large3") == 1);
                            pp.setUnique(rs.getInt("unique_unlocked") == 1);
                        } catch (IllegalArgumentException ignored) {}
                    }
                }

                // Load Amulet Data
                try (PreparedStatement ps = connection.prepareStatement("SELECT * FROM player_amulet_data WHERE uuid = ?")) {
                    ps.setString(1, profile.getUuid().toString());
                    ResultSet rs = ps.executeQuery();
                    if (rs.next()) {
                        profile.setGachaTotalRolls(rs.getInt("gacha_rolls"));
                        profile.setRollsSinceLastEpic(rs.getInt("rolls_epic"));
                        profile.setRollsSinceLastLegendary(rs.getInt("rolls_legendary"));
                    }
                }

                try (PreparedStatement ps = connection.prepareStatement("SELECT * FROM owned_amulets WHERE uuid = ?")) {
                    ps.setString(1, profile.getUuid().toString());
                    ResultSet rs = ps.executeQuery();
                    while (rs.next()) {
                        try {
                            AmuletType type = AmuletType.valueOf(rs.getString("type"));
                            profile.getOwnedAmulets().put(type, rs.getInt("level"));
                        } catch (IllegalArgumentException ignored) {}
                    }
                }

                try (PreparedStatement ps = connection.prepareStatement("SELECT * FROM equipped_amulets WHERE uuid = ?")) {
                    ps.setString(1, profile.getUuid().toString());
                    ResultSet rs = ps.executeQuery();
                    while (rs.next()) {
                        try {
                            int slot = rs.getInt("slot");
                            AmuletType type = AmuletType.valueOf(rs.getString("type"));
                            if (slot >= 0 && slot < 4) {
                                profile.getEquippedAmulets()[slot] = type;
                            }
                        } catch (IllegalArgumentException ignored) {}
                    }
                }

                try (PreparedStatement ps = connection.prepareStatement("SELECT * FROM amulet_currency WHERE uuid = ?")) {
                    ps.setString(1, profile.getUuid().toString());
                    ResultSet rs = ps.executeQuery();
                    while (rs.next()) {
                        try {
                            AmuletRarity rarity = AmuletRarity.valueOf(rs.getString("rarity"));
                            profile.getAmuletCurrency().put(rarity, rs.getInt("amount"));
                        } catch (IllegalArgumentException ignored) {}
                    }
                }
                
                // Пересчитываем статы после загрузки
                Bukkit.getScheduler().runTask(plugin, profile::recalculateStats);
            } catch (SQLException e) {
                e.printStackTrace();
            }
        });
    }

    public Connection getConnection() {
        return connection;
    }

    public void shutdown() {
        try {
            if (connection != null) connection.close();
        } catch (SQLException ignored) {}
    }
}
