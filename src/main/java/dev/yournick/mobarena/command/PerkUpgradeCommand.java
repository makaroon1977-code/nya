package dev.yournick.mobarena.command;

import dev.yournick.mobarena.MobArenaPlugin;
import dev.yournick.mobarena.perk.Perk;
import dev.yournick.mobarena.player.PerkProfile;
import dev.yournick.mobarena.player.PlayerProfile;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public class PerkUpgradeCommand implements CommandExecutor, Listener {

    private final MobArenaPlugin plugin;

    public PerkUpgradeCommand(MobArenaPlugin plugin) {
        this.plugin = plugin;
    }

    private static class PerkListHolder implements InventoryHolder {
        @Override public Inventory getInventory() { return null; }
    }

    private static class UpgradeMenuHolder implements InventoryHolder {
        private final Perk perk;
        public UpgradeMenuHolder(Perk perk) { this.perk = perk; }
        public Perk getPerk() { return perk; }
        @Override public Inventory getInventory() { return null; }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) return true;
        Player player = (Player) sender;
        PlayerProfile profile = plugin.getPlayerRepository().getProfile(player);
        if (profile == null) return true;

        // Если передан аргумент (название перка), открываем его (для ПКМ в меню выбора)
        if (args.length > 0) {
            try {
                Perk perk = Perk.valueOf(args[0].toUpperCase());
                openUpgradeMenu(player, perk);
                return true;
            } catch (IllegalArgumentException ignored) {}
        }

        // По умолчанию открываем меню текущего перка игрока
        Perk current = profile.getPlayerPerk().getPerk();
        if (current != null) {
            openUpgradeMenu(player, current);
        } else {
            openPerkList(player);
        }
        return true;
    }

    private void openPerkList(Player player) {
        Inventory inv = Bukkit.createInventory(new PerkListHolder(), 9, "§6Улучшение перков");
        for (Perk perk : Perk.values()) {
            ItemStack item = new ItemStack(Material.BOOK);
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                meta.setDisplayName(ChatColor.GREEN + perk.getDisplayName());
                List<String> lore = new ArrayList<>();
                lore.add(ChatColor.GRAY + "Нажмите, чтобы открыть меню прокачки");
                meta.setLore(lore);
                item.setItemMeta(meta);
            }
            inv.addItem(item);
        }
        player.openInventory(inv);
    }

    private void openUpgradeMenu(Player player, Perk perk) {
        Inventory inv = Bukkit.createInventory(new UpgradeMenuHolder(perk), 27, "§6Прокачка: " + perk.getDisplayName());
        PlayerProfile profile = plugin.getPlayerRepository().getProfile(player);
        PerkProfile pp = profile.getPerkProfile(perk);

        // Инфо об опыте и золоте
        ItemStack info = new ItemStack(Material.PAPER);
        ItemMeta infoMeta = info.getItemMeta();
        if (infoMeta != null) {
            infoMeta.setDisplayName("§eВаша статистика");
            List<String> lore = new ArrayList<>();
            lore.add("§7Опыт класса: §a" + pp.getXp());
            lore.add("§7Золото: §6" + profile.getGold());
            lore.add("§7Всего мини-улучшений: §b" + pp.getTotalMini());
            infoMeta.setLore(lore);
            info.setItemMeta(infoMeta);
        }
        inv.setItem(4, info);

        // Мини-улучшения (слоты 10, 12, 14)
        addMiniUpgrade(inv, 10, perk, 1, pp);
        addMiniUpgrade(inv, 12, perk, 2, pp);
        addMiniUpgrade(inv, 14, perk, 3, pp);

        // Крупные улучшения (слоты 19, 21, 23)
        addLargeUpgrade(inv, 19, perk, 1, pp);
        addLargeUpgrade(inv, 21, perk, 2, pp);
        addLargeUpgrade(inv, 23, perk, 3, pp);

        // Уникальная способность (слот 25)
        addUniqueUpgrade(inv, 25, perk, pp);

        player.openInventory(inv);
    }

    private void addMiniUpgrade(Inventory inv, int slot, Perk perk, int index, PerkProfile pp) {
        int level = (index == 1) ? pp.getMini1() : (index == 2) ? pp.getMini2() : pp.getMini3();

        ItemStack item = new ItemStack(Material.IRON_INGOT);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            String name = getMiniName(perk, index);
            meta.setDisplayName("§b" + name + " (" + level + "/5)");
            List<String> lore = new ArrayList<>();
            String desc = getMiniDesc(perk, index, level);
            if (desc != null && !desc.isEmpty()) {
                for (String line : desc.split("\n")) {
                    lore.add(line);
                }
            }
            lore.add("");
            if (level < 5) {
                lore.add("§7Стоимость: §a25 XP§7, §650 золота");
                lore.add("§eНажмите для улучшения");
            } else {
                lore.add("§aМаксимальный уровень!");
            }
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        inv.setItem(slot, item);
    }

    private void addLargeUpgrade(Inventory inv, int slot, Perk perk, int index, PerkProfile pp) {
        boolean bought = (index == 1) ? pp.hasLarge1() : (index == 2) ? pp.hasLarge2() : pp.hasLarge3();

        String name = getLargeName(perk, index);
        if (name == null) return;

        ItemStack item = new ItemStack(bought ? Material.GOLD_INGOT : Material.COAL);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName((bought ? "§6" : "§7") + name);
            List<String> lore = new ArrayList<>();
            String desc = getLargeDesc(perk, index);
            if (desc != null && !desc.isEmpty()) {
                for (String line : desc.split("\n")) {
                    lore.add(line);
                }
            }
            lore.add("");
            if (!bought) {
                int req = index * 5;
                lore.add("§7Условие: §b" + req + " мини-улучшений");
                lore.add("§7Стоимость: §a50 XP§7, §6100 золота");
                if (pp.getTotalMini() >= req) lore.add("§eНажмите для покупки");
                else lore.add("§cНедостаточно мини-улучшений!");
            } else {
                lore.add("§aКуплено!");
            }
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        inv.setItem(slot, item);
    }

    private void addUniqueUpgrade(Inventory inv, int slot, Perk perk, PerkProfile pp) {
        boolean bought = pp.hasUnique();
        ItemStack item = new ItemStack(bought ? Material.DIAMOND : Material.BARRIER);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName((bought ? "§d" : "§7") + "Уникальная способность");
            List<String> lore = new ArrayList<>();
            String desc = getUniqueDesc(perk);
            if (desc != null && !desc.isEmpty()) {
                for (String line : desc.split("\n")) {
                    lore.add(line);
                }
            }
            lore.add("");
            if (!bought) {
                lore.add("§7Условие: §b2 крупных апгрейда");
                lore.add("§7Стоимость: §a200 XP§7, §6500 золота");
                int largeCount = (pp.hasLarge1() ? 1 : 0) + (pp.hasLarge2() ? 1 : 0);
                if (largeCount >= 2) lore.add("§eНажмите для покупки");
                else lore.add("§cНужно 2 крупных апгрейда!");
            } else {
                lore.add("§aАктивировано!");
            }
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        inv.setItem(slot, item);
    }

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        if (e.getInventory().getHolder() instanceof PerkListHolder) {
            e.setCancelled(true);
            if (e.getCurrentItem() == null) return;
            String name = ChatColor.stripColor(e.getCurrentItem().getItemMeta().getDisplayName());
            for (Perk perk : Perk.values()) {
                if (perk.getDisplayName().equalsIgnoreCase(name)) {
                    openUpgradeMenu((Player) e.getWhoClicked(), perk);
                    return;
                }
            }
        } else if (e.getInventory().getHolder() instanceof UpgradeMenuHolder) {
            e.setCancelled(true);
            UpgradeMenuHolder holder = (UpgradeMenuHolder) e.getInventory().getHolder();
            Perk perk = holder.getPerk();
            Player player = (Player) e.getWhoClicked();
            PlayerProfile profile = plugin.getPlayerRepository().getProfile(player);
            PerkProfile pp = profile.getPerkProfile(perk);

            int slot = e.getSlot();
            if (slot == 10 || slot == 12 || slot == 14) {
                int index = (slot == 10) ? 1 : (slot == 12) ? 2 : 3;
                buyMini(player, profile, perk, index);
            } else if (slot == 19 || slot == 21 || slot == 23) {
                int index = (slot == 19) ? 1 : (slot == 21) ? 2 : 3;
                buyLarge(player, profile, perk, index);
            } else if (slot == 25) {
                buyUnique(player, profile, perk);
            }
            openUpgradeMenu(player, perk); // Refresh
        }
    }

    private void buyMini(Player player, PlayerProfile profile, Perk perk, int index) {
        PerkProfile pp = profile.getPerkProfile(perk);
        int level = (index == 1) ? pp.getMini1() : (index == 2) ? pp.getMini2() : pp.getMini3();
        if (level >= 5) return;
        if (pp.getXp() < 25 || profile.getGold() < 50) {
            player.sendMessage("§cНедостаточно ресурсов!");
            return;
        }
        pp.setXp(pp.getXp() - 25);
        profile.removeGold(50);
        if (index == 1) pp.setMini1(level + 1);
        else if (index == 2) pp.setMini2(level + 1);
        else pp.setMini3(level + 1);
        player.sendMessage("§aУлучшено!");
    }

    private void buyLarge(Player player, PlayerProfile profile, Perk perk, int index) {
        PerkProfile pp = profile.getPerkProfile(perk);
        boolean bought = (index == 1) ? pp.hasLarge1() : (index == 2) ? pp.hasLarge2() : pp.hasLarge3();
        if (bought) return;
        int req = index * 5;
        if (pp.getTotalMini() < req) {
            player.sendMessage("§cНужно " + req + " мини-улучшений!");
            return;
        }
        if (pp.getXp() < 50 || profile.getGold() < 100) {
            player.sendMessage("§cНедостаточно ресурсов!");
            return;
        }
        pp.setXp(pp.getXp() - 50);
        profile.removeGold(100);
        if (index == 1) pp.setLarge1(true);
        else if (index == 2) pp.setLarge2(true);
        else pp.setLarge3(true);
        player.sendMessage("§aКуплено!");
    }

    private void buyUnique(Player player, PlayerProfile profile, Perk perk) {
        PerkProfile pp = profile.getPerkProfile(perk);
        if (pp.hasUnique()) return;
        int largeCount = (pp.hasLarge1() ? 1 : 0) + (pp.hasLarge2() ? 1 : 0);
        if (largeCount < 2) {
            player.sendMessage("§cНужно 2 крупных апгрейда!");
            return;
        }
        if (pp.getXp() < 200 || profile.getGold() < 500) {
            player.sendMessage("§cНедостаточно ресурсов!");
            return;
        }
        pp.setXp(pp.getXp() - 200);
        profile.removeGold(500);
        pp.setUnique(true);
        player.sendMessage("§aУникальная способность активирована!");
    }

    // --- Descriptions ---

    private String getMiniName(Perk perk, int index) {
        switch (perk) {
            case LUCKY:
                return (index == 1) ? "Золотая лихорадка" : (index == 2) ? "Охотник за сокровищами" : "Улыбка фортуны";
            case BLACKSMITH:
                return (index == 1) ? "Мастерство" : (index == 2) ? "Закалка" : "Экономия";
            case TRADER:
                return (index == 1) ? "Наценка" : (index == 2) ? "Связи" : "Золотоискатель";
            case FIREBORN:
                return (index == 1) ? "Вампиризм" : (index == 2) ? "Жар" : "Ответный огонь";
            case AGILE:
                return (index == 1) ? "Ловкость" : (index == 2) ? "Контрудар" : "Охотник";
            case TOUGH:
                return (index == 1) ? "Виталити" : (index == 2) ? "Отражение" : "Удача";
            case PIRATE:
                return (index == 1) ? "Пиратский грабеж" : (index == 2) ? "Попутный ветер" : "Черная метка";
        }
        return "Мини-улучшение";
    }

    private String getMiniDesc(Perk perk, int index, int level) {
        switch (perk) {
            case LUCKY:
                if (index == 1) return "§7Увеличивает получаемое золото.\n§7Бонус золота (Gold Gain): §a+" + String.format("%.1f", (double)(40 + 10 * level)) + "%";
                if (index == 2) return "§7Шанс выпадения редких предметов.\n§7Редкий дроп (Rare Drop): §a" + String.format("%.1f", (double)(5 + 3 * level)) + "%";
                if (index == 3) return "§7Шанс нанести критический удар.\n§7Крит. шанс (Crit Chance): §a+" + String.format("%.1f", (double)(4 * level)) + "%";
                break;
            case BLACKSMITH:
                if (index == 1) return "§7Дополнительная скидка на улучшения.\n§7Текущая скидка: §a" + String.format("%.1f", (double)(35 + 3 * level)) + "%\n§e+10% Брони (База)";
                if (index == 2) return "§7Уменьшает входящий урон.\n§7Броня (Armor): §a+" + String.format("%.1f", (double)(2 * level)) + "%";
                if (index == 3) return "§7Шанс, что улучшение будет бесплатным.\n§7Текущий шанс: §a" + String.format("%.1f", (double)(3 * level)) + "%";
                break;
            case TRADER:
                if (index == 1) return "§7Увеличивает выручку от продажи лута.\n§7Цена продажи (Sell Price): §a+" + String.format("%.1f", (double)(50 + 5 * level)) + "%";
                if (index == 2) return "§7Снижает штраф на золото с мобов.\n§7Бонус золота (Gold Gain): §a" + String.format("%.1f", (double)(-(20 - 4 * level))) + "%";
                if (index == 3) return "§7Шанс выпадения золотого самородка.\n§7Текущий шанс: §a" + String.format("%.1f", (double)(2 * level)) + "%";
                break;
            case FIREBORN:
                if (index == 1) return "§7Восстановление здоровья при ударах.\n§7Вампиризм (Lifesteal): §a+" + String.format("%.1f", (double)(3 * level)) + "%";
                if (index == 2) return "§7Урон по горящим целям.\n§7Урон огнем (Fire Damage): §a+" + String.format("%.1f", (double)(5 * level)) + "%";
                if (index == 3) return "§7Шанс поджечь атакующего вас врага.\n§7Длительность горения: §a+" + String.format("%.1f", (double)(10 * level)) + "%";
                break;
            case AGILE:
                if (index == 1) return "§7Шанс полностью избежать урона.\n§7Уклонение (Dodge): §a" + String.format("%.1f", (double)(35 + 3 * level)) + "%";
                if (index == 2) return "§7Шанс получить усиление после уклонения.\n§7Текущий шанс: §a" + String.format("%.1f", (double)(2 * level)) + "%";
                if (index == 3) return "§7Увеличивает общий урон.\n§7Урон (Damage): §a+" + String.format("%.1f", (double)(3 * level)) + "%";
                break;
            case TOUGH:
                if (index == 1) return "§7Увеличивает максимальный запас здоровья.\n§7Здоровье (Max HP): §a+" + String.format("%.1f", (double)(4 * level)) + "%";
                if (index == 2) return "§7Возвращает часть урона атакующему.\n§7Отражение (Reflection): §a+" + String.format("%.1f", (double)(3 * level)) + "%";
                if (index == 3) return "§7Шанс стать неуязвимым на 2 сек.\n§7Текущий шанс: §a" + String.format("%.1f", (double)(1 * level)) + "%";
                break;
            case PIRATE:
                if (index == 1) return "§7Шанс при ударе забрать у моба 5 монет.\n§7Шанс: §a" + String.format("%.1f", (double)(3 * level)) + "%\n§8(1 раз на моба)";
                if (index == 2) return "§7Увеличивает скорость передвижения.\n§7Скорость (Speed): §a+" + String.format("%.1f", (double)(5 * level)) + "%";
                if (index == 3) return "§7Увеличивает количество золота с мобов.\n§7Золото (Gold Gain): §a+" + String.format("%.1f", (double)(5 * level)) + "%";
                break;
        }
        return "";
    }

    private String getLargeName(Perk perk, int index) {
        switch (perk) {
            case LUCKY:
                return (index == 1) ? "Джекпот" : (index == 2) ? "Второй шанс" : "Удачный удар";
            case BLACKSMITH:
                return (index == 1) ? "Смертельный удар" : (index == 2) ? "Броня в бою" : null;
            case TRADER:
                return (index == 1) ? "Оптовик" : (index == 2) ? "Удача торговца" : null;
            case FIREBORN:
                return (index == 1) ? "Огненный след" : (index == 2) ? "Жар битвы" : null;
            case AGILE:
                return (index == 1) ? "Контратака" : (index == 2) ? "Медленный враг" : (index == 3) ? "Палач" : null;
            case TOUGH:
                return (index == 1) ? "Железная воля" : (index == 2) ? "Крепость" : (index == 3) ? "Сильные зелья" : null;
            case PIRATE:
                return (index == 1) ? "Пьяная ярость" : (index == 2) ? "Грабёж" : (index == 3) ? "Морской волк" : null;
        }
        return null;
    }

    private String getLargeDesc(Perk perk, int index) {
        switch (perk) {
            case LUCKY:
                if (index == 1) return "§7Шанс 5% при убийстве моба\n§7получить в 10 раз больше золота.";
                if (index == 2) return "§7Добавляет 15% шанс выжить с 1 HP\n§7при получении смертельного урона.";
                if (index == 3) return "§7Шанс 10% нанести от 100% до 300%\n§7урона при каждом ударе.";
                break;
            case BLACKSMITH:
                if (index == 1) return "§7Шанс 10% нанести 500 урона мобу\n§7при ударе мечом.";
                if (index == 2) return "§7При получении урона есть шанс 15%\n§7получить Броню +20% на 3 сек.";
                break;
            case TRADER:
                if (index == 1) return "§7Дополнительный бонус при продаже\n§7сразу большого количества предметов.";
                if (index == 2) return "§7Шанс 20% получить дополнительный\n§7предмет при убийстве моба.";
                break;
            case FIREBORN:
                if (index == 1) return "§7Вы оставляете визуальный след (2 сек),\n§7который поджигает врагов и наносит\n§73 урона в секунду.";
                if (index == 2) return "§7+3% урона за каждого горящего врага\n§7в радиусе 10 блоков вокруг вас.";
                break;
            case AGILE:
                if (index == 1) return "§7После уклонения ваш следующий удар\n§7нанесет на 50% больше урона.";
                if (index == 2) return "§7Накладывает Замедление I на всех\n§7врагов в радиусе 3 блоков.";
                if (index == 3) return "§7Увеличивает урон по замедленным\n§7врагам на 25%.";
                break;
            case TOUGH:
                if (index == 1) return "§7Каждый удар снижает получаемый вами\n§7урон на 3% (стакается до 21%).";
                if (index == 2) return "§7Дает +20% к регенерации, если вы\n§7стоите неподвижно.";
                if (index == 3) return "§7Все исцеляющие зелья восстанавливают\n§7на 50% больше здоровья.";
                break;
            case PIRATE:
                if (index == 1) return "§7Урон растет при падении HP.\n§7(Урон +0% -> +20% -> +40%)";
                if (index == 2) return "§7Шанс 25% получить в два раза\n§7больше дропа с моба.";
                if (index == 3) return "§7Снижает штраф к входящему урону\n§7с 10% до 0%.";
                break;
        }
        return "";
    }

    private String getUniqueDesc(Perk perk) {
        switch (perk) {
            case LUCKY:
                return "§7Колесо Фортуны (Активная):\n§7На 15 сек 100% Крит. шанс и\n§7100% шанс двойного дропа.\n§8Перезарядка: 3 мин.";
            case BLACKSMITH:
                return "§7Если здоровье <30%:\n§7Броня +100% на 10 сек.\n§8Перезарядка: 60 сек.";
            case TRADER:
                return "§7Золотая жила (Активная):\n§7На 60 сек все мобы дропают золото\n§7и +20% золота за убийства.\n§eСкидка 25% в магазине.\n§8Перезарядка: 3 мин.";
            case FIREBORN:
                return "§7Аватар пламени (Активная):\n§7На 10 сек все враги вокруг загораются,\n§7а убитые вами взрываются (25% макс. HP\n§7урона в радиусе 3 блоков).\n§8Перезарядка: 2 мин.";
            case AGILE:
                return "§7Идеальное уклонение (Активная):\n§7На 3 сек 100% уклонение, телепорт\n§7и след. удар +1000% урона.\n§8Перезарядка: 40 сек.";
            case TOUGH:
                return "§7Последний рубеж (Активная):\n§7Если HP < 10%: бессмертие на 5 сек,\n§7и замедление врагов.\n§8Перезарядка: 2 мин.";
            case PIRATE:
                return "§7Безумный абордаж (Активная):\n§7На 8 сек Урон +60%, Регенерация +20%,\n§7игнорирование брони врагов.\n§8Перезарядка: 1 мин.";
        }
        return "";
    }
}
