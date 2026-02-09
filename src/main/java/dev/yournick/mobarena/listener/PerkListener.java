package dev.yournick.mobarena.listener;

import dev.yournick.mobarena.MobArenaPlugin;
import dev.yournick.mobarena.arena.ArenaLoot;
import dev.yournick.mobarena.perk.Perk;
import dev.yournick.mobarena.player.PlayerProfile;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.*;

public class PerkListener implements Listener {

    private final dev.yournick.mobarena.MobArenaPlugin plugin;
    private final Map<UUID, ItemStack> savedUniqueItems = new HashMap<>();

    public PerkListener(dev.yournick.mobarena.MobArenaPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        ItemStack item = event.getItem();
        if (item == null || !dev.yournick.mobarena.perk.PlayerPerk.isUniqueItem(item)) return;

        Player player = event.getPlayer();
        PlayerProfile profile = plugin.getPlayerRepository().getProfile(player);
        if (profile == null || profile.getPlayerPerk() == null) return;
        
        Perk perk = profile.getPlayerPerk().getPerk();
        if (perk == null) return;

        String name = item.getItemMeta().getDisplayName();
        if (name == null) return;

        if (name.contains("Колесо Фортуны")) activateWheelOfFortune(player, profile);
        else if (name.contains("Золотая жила")) activateGoldenVein(player, profile);
        else if (name.contains("Аватар пламени")) activateAvatarOfFlame(player, profile);
        else if (name.contains("Идеальное уклонение")) activatePerfectDodge(player, profile);
        else if (name.contains("Безумный абордаж")) activateMadBoarding(player, profile);
    }

    private void activateWheelOfFortune(Player player, PlayerProfile profile) {
        if (checkCooldown(player, profile, "lucky_active", 180)) {
            player.sendMessage("§6Колесо Фортуны запущено! (15 сек)");
            profile.setWheelOfFortuneActive(true);
            profile.recalculateStats();

            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                profile.setWheelOfFortuneActive(false);
                profile.recalculateStats();
                player.sendMessage("§cЭффект Колеса Фортуны закончился.");
            }, 300L);
        }
    }

    private void activateGoldenVein(Player player, PlayerProfile profile) {
        if (checkCooldown(player, profile, "trader_active", 180)) {
            player.sendMessage("§6Золотая жила активирована! (60 сек)");
            profile.setGoldenVeinActive(true);
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                profile.setGoldenVeinActive(false);
                player.sendMessage("§cЗолотая жила закончилась.");
            }, 1200L);
        }
    }

    private void activateAvatarOfFlame(Player player, PlayerProfile profile) {
        if (checkCooldown(player, profile, "fireborn_active", 120)) {
            player.sendMessage("§cАватар пламени активирован!");
            for (Entity e : player.getNearbyEntities(5, 5, 5)) {
                if (e instanceof LivingEntity && !(e instanceof Player)) {
                    e.setFireTicks(100);
                }
            }
            profile.setAvatarOfFlameActive(true);
            Bukkit.getScheduler().runTaskLater(plugin, () -> profile.setAvatarOfFlameActive(false), 200L);
        }
    }

    private void activatePerfectDodge(Player player, PlayerProfile profile) {
        if (checkCooldown(player, profile, "agile_active", 40)) {
            player.sendMessage("§fИдеальное уклонение!");
            profile.setPerfectDodgeActive(true);
            LivingEntity strongest = null;
            double maxHp = 0;
            for (Entity e : player.getNearbyEntities(15, 15, 15)) {
                if (e instanceof LivingEntity && plugin.getArenaManager().isArenaMob(e)) {
                    if (((LivingEntity) e).getHealth() > maxHp) {
                        maxHp = ((LivingEntity) e).getHealth();
                        strongest = (LivingEntity) e;
                    }
                }
            }
            if (strongest != null) {
                player.teleport(strongest.getLocation().add(strongest.getLocation().getDirection().multiply(-1)));
            }
            Bukkit.getScheduler().runTaskLater(plugin, () -> profile.setPerfectDodgeActive(false), 60L);
        }
    }

    private void activateLastStand(Player player, PlayerProfile profile) {
        player.sendMessage("§cТеперь это пассивная способность!");
        player.sendMessage("§7Срабатывает автоматически при низком здоровье.");
    }

    private void activateMadBoarding(Player player, PlayerProfile profile) {
        if (checkCooldown(player, profile, "pirate_active", 60)) {
            player.sendMessage("§eБезумный абордаж!");
            profile.setMadBoardingActive(true);
            profile.recalculateStats();
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                profile.setMadBoardingActive(false);
                profile.recalculateStats();
                player.addPotionEffect(new PotionEffect(PotionEffectType.CONFUSION, 100, 9));
            }, 160L);
        }
    }

    private boolean checkCooldown(Player p, PlayerProfile profile, String key, int seconds) {
        if (profile.isAbilityOnCooldown(key)) {
            p.sendMessage("§cСпособность еще не готова! (" + profile.getAbilityCooldownRemaining(key) + " сек)");
            return false;
        }
        profile.setAbilityCooldown(key, seconds);
        return true;
    }

    @EventHandler
    public void onMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        PlayerProfile profile = plugin.getPlayerRepository().getProfile(player);
        if (profile == null) return;
        
        // Огненный след
        if (profile.getPlayerPerk() != null && profile.getPlayerPerk().getPerk() == Perk.FIREBORN && profile.getPerkProfile(Perk.FIREBORN).hasLarge1()) {
            if (event.getFrom().getBlockX() != event.getTo().getBlockX() || event.getFrom().getBlockZ() != event.getTo().getBlockZ()) {
                plugin.getArenaManager().addFireTrail(event.getFrom());
            }
        }
    }

    @EventHandler
    public void onDeath(EntityDeathEvent event) {
        // Логика дропов перенесена в ArenaManager для надежности (ручной спавн предметов)
    }

    @EventHandler
    public void onHeld(PlayerItemHeldEvent event) {
        update(event.getPlayer());
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        Player player = (Player) event.getWhoClicked();

        // Защита уникального предмета в 9-м слоте
        if (event.getSlot() == 8 && event.getClickedInventory() != null && event.getClickedInventory().getType() == InventoryType.PLAYER) {
            if (dev.yournick.mobarena.perk.PlayerPerk.isUniqueItem(event.getCurrentItem())) {
                event.setCancelled(true);
                return;
            }
        }

        // Запрет перемещения уникального предмета
        if (dev.yournick.mobarena.perk.PlayerPerk.isUniqueItem(event.getCurrentItem()) || dev.yournick.mobarena.perk.PlayerPerk.isUniqueItem(event.getCursor())) {
            event.setCancelled(true);
            return;
        }

        // Запрет взаимодействия через горячие клавиши (цифра 9)
        if (event.getClick() == ClickType.NUMBER_KEY && event.getHotbarButton() == 8) {
            event.setCancelled(true);
            return;
        }

        update(player);
    }

    @EventHandler
    public void onDrop(PlayerDropItemEvent event) {
        if (dev.yournick.mobarena.perk.PlayerPerk.isUniqueItem(event.getItemDrop().getItemStack())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onPickup(EntityPickupItemEvent event) {
        if (event.getEntity() instanceof Player) {
            update((Player) event.getEntity());
        }
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        Iterator<ItemStack> iterator = event.getDrops().iterator();
        while (iterator.hasNext()) {
            ItemStack item = iterator.next();
            if (dev.yournick.mobarena.perk.PlayerPerk.isUniqueItem(item)) {
                savedUniqueItems.put(player.getUniqueId(), item.clone());
                iterator.remove();
            }
        }
    }

    @EventHandler
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();
        ItemStack saved = savedUniqueItems.remove(player.getUniqueId());
        if (saved != null) {
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                if (player.isOnline()) {
                    player.getInventory().setItem(8, saved);
                }
            }, 10L);
        }
    }

    private void update(Player player) {
        PlayerProfile profile = plugin.getPlayerRepository().getProfile(player);
        if (profile != null && profile.getPlayerPerk() != null) {
            // Запускаем через 1 тик, чтобы инвентарь успел обновиться
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                if (player.isOnline()) {
                    profile.getPlayerPerk().updateWeaponEnchant(player);
                }
            }, 1L);
        }
    }
}
