package dev.yournick.mobarena.amulet;

import dev.yournick.mobarena.MobArenaPlugin;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class GachaAnimation implements Listener {

    private final MobArenaPlugin plugin;
    private final Player player;
    private final AmuletType result;
    private final AmuletManager amuletManager;
    private final Random random = new Random();

    public GachaAnimation(MobArenaPlugin plugin, Player player, AmuletType result, AmuletManager amuletManager) {
        this.plugin = plugin;
        this.player = player;
        this.result = result;
        this.amuletManager = amuletManager;
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    public static class AnimationHolder implements InventoryHolder {
        @Override
        public Inventory getInventory() {
            return null;
        }
    }

    public void start() {
        Inventory inv = Bukkit.createInventory(new AnimationHolder(), 27, "§6Открытие кейса...");
        player.openInventory(inv);

        // Fill top and bottom rows with glass
        ItemStack glass = new ItemStack(Material.STAINED_GLASS_PANE, 1, (short) 15);
        ItemMeta glassMeta = glass.getItemMeta();
        if (glassMeta != null) {
            glassMeta.setDisplayName(" ");
            glass.setItemMeta(glassMeta);
        }

        for (int i = 0; i < 9; i++) {
            inv.setItem(i, glass);
            inv.setItem(i + 18, glass);
        }

        // Pointer
        ItemStack pointer = new ItemStack(Material.HOPPER);
        ItemMeta pMeta = pointer.getItemMeta();
        if (pMeta != null) {
            pMeta.setDisplayName("§e▼ §6ВАШ ПРИЗ §e▼");
            pointer.setItemMeta(pMeta);
        }
        inv.setItem(4, pointer);

        // Animation sequence
        List<AmuletType> sequence = new ArrayList<>();
        // Generate ~35 random amulets for the spin
        for (int i = 0; i < 35; i++) {
            sequence.add(getRandomAmulet());
        }
        
        // We want the result to be at index 4 of the displayed row when it stops.
        // The middle slot index is (step + 4). 
        // We will stop when step reaches maxSteps.
        // So sequence.get(maxSteps + 4) should be result.
        
        int resultIndex = 35; 
        sequence.add(result);
        
        // Add a few more so it doesn't look like it just ended abruptly
        for (int i = 0; i < 10; i++) {
            sequence.add(getRandomAmulet());
        }

        new BukkitRunnable() {
            int step = 0;
            int delay = 1;
            int ticks = 0;
            int maxSteps = resultIndex - 4; // So at the end, slot 13 (index 4) shows sequence[maxSteps + 4] = result

            @Override
            public void run() {
                // Safety check: if player closed inv, stop and give reward
                if (!player.isOnline() || !(player.getOpenInventory().getTopInventory().getHolder() instanceof AnimationHolder)) {
                    finish();
                    this.cancel();
                    return;
                }

                ticks++;
                if (ticks < delay) return;
                ticks = 0;

                // Update items in middle row (9-17)
                for (int i = 0; i < 9; i++) {
                    int seqIdx = step + i;
                    if (seqIdx < sequence.size()) {
                        inv.setItem(9 + i, createAmuletItem(sequence.get(seqIdx)));
                    }
                }

                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_HAT, 1f, 1f);
                step++;

                // Slow down
                if (step > maxSteps - 15) delay++;
                if (step > maxSteps - 10) delay++;
                if (step > maxSteps - 5) delay += 2;

                if (step > maxSteps) {
                    this.cancel();
                    Bukkit.getScheduler().runTaskLater(plugin, () -> {
                        finish();
                        player.closeInventory();
                    }, 20L);
                }
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getInventory().getHolder() instanceof AnimationHolder) {
            event.setCancelled(true);
        }
    }

    private AmuletType getRandomAmulet() {
        double legendChance = AmuletRarity.LEGENDARY.getBaseChance();
        double epicChance = AmuletRarity.EPIC.getBaseChance();
        double rareChance = AmuletRarity.RARE.getBaseChance();

        double roll = random.nextDouble();
        AmuletRarity rarity;

        if (roll < legendChance) rarity = AmuletRarity.LEGENDARY;
        else if (roll < legendChance + epicChance) rarity = AmuletRarity.EPIC;
        else if (roll < legendChance + epicChance + rareChance) rarity = AmuletRarity.RARE;
        else rarity = AmuletRarity.COMMON;

        List<AmuletType> types = new ArrayList<>();
        for (AmuletType type : AmuletType.values()) {
            if (type.getRarity() == rarity) types.add(type);
        }
        if (types.isEmpty()) return AmuletType.STRENGTH; // Fallback
        return types.get(random.nextInt(types.size()));
    }

    private ItemStack createAmuletItem(AmuletType type) {
        ItemStack item = new ItemStack(type.getMaterial());
        
        // Специальная обработка для зелий
        if (type == AmuletType.REGEN) {
            org.bukkit.inventory.meta.PotionMeta pm = (org.bukkit.inventory.meta.PotionMeta) item.getItemMeta();
            pm.setMainEffect(org.bukkit.potion.PotionEffectType.REGENERATION);
            item.setItemMeta(pm);
        } else if (type == AmuletType.FORTUNE) {
            org.bukkit.inventory.meta.PotionMeta pm = (org.bukkit.inventory.meta.PotionMeta) item.getItemMeta();
            pm.setMainEffect(org.bukkit.potion.PotionEffectType.LUCK);
            item.setItemMeta(pm);
        }

        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            // Эффект свечения для Амулета Критов и Титана
            if (type == AmuletType.CRIT || type == AmuletType.TITAN) {
                meta.addEnchant(org.bukkit.enchantments.Enchantment.DURABILITY, 1, true);
                meta.addItemFlags(org.bukkit.inventory.ItemFlag.HIDE_ENCHANTS);
            }

            meta.setDisplayName(type.getRarity().getColor() + type.getDisplayName());
            item.setItemMeta(meta);
        }
        return item;
    }

    private void finish() {
        HandlerList.unregisterAll(this);
        amuletManager.processGachaResult(player, result);
    }
}
