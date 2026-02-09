package dev.yournick.mobarena.listener;

import dev.yournick.mobarena.MobArenaPlugin;
import dev.yournick.mobarena.player.PlayerProfile;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class ConsumableListener implements Listener {

    private final MobArenaPlugin plugin;

    public ConsumableListener(MobArenaPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onConsume(PlayerItemConsumeEvent e) {
        Player player = e.getPlayer();
        PlayerProfile profile = plugin.getPlayerRepository().getProfile(player);
        if (profile == null) return;

        if (e.getItem() == null || !e.getItem().hasItemMeta() || e.getItem().getItemMeta().getDisplayName() == null) return;
        String name = ChatColor.stripColor(e.getItem().getItemMeta().getDisplayName());

        if (name.contains("Стейк арены")) {
            profile.activateSteakBuff();
            player.sendMessage(ChatColor.GREEN + "Бафф стейка активирован! (+25.0% HP)");
        } else if (name.contains("Зелье здоровья")) {
            profile.activateHpPotion(60);
            player.sendMessage(ChatColor.GREEN + "Зелье здоровья активировано! (+50.0% HP на 60с)");
        } else if (name.contains("Зелье урона")) {
            profile.activateDamagePotion(60);
            player.sendMessage(ChatColor.GREEN + "Зелье урона активировано! (+35.0% Урона на 60с)");
        } else if (name.contains("Зелье скорости")) {
            profile.activateSpeedPotion(60);
            player.sendMessage(ChatColor.GREEN + "Зелье скорости активировано! (+25.0% Скорости на 60с)");
        } else if (name.contains("Золотое яблоко арены")) {
            profile.activateGAppleBuff(30);
            player.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 600, 1)); // Реген II на 30с
            player.sendMessage(ChatColor.GOLD + "Эффект золотого яблока! (+100.0% HP и Реген II на 30с)");
        }
    }
}
