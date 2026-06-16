package dev.passport.command;

import dev.passport.PassportPlugin;
import dev.passport.listener.PassportItemListener;
import dev.passport.model.PassportData;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.BiConsumer;

/**
 * Обработчик команды {@code /passport} и её подкоманд, а также таб-комплит.
 */
public class PassportCommand implements CommandExecutor, TabCompleter {

    private final PassportPlugin plugin;

    /**
     * Доступные для редактирования поля: ключ команды -> сеттер.
     * LinkedHashMap сохраняет порядок для подсказок и сообщений.
     */
    private final Map<String, BiConsumer<PassportData, String>> fields = new LinkedHashMap<>();

    public PassportCommand(PassportPlugin plugin) {
        this.plugin = plugin;
        fields.put("rpName", PassportData::setRpName);
        fields.put("birthDate", PassportData::setBirthDate);
        fields.put("birthPlace", PassportData::setBirthPlace);
        fields.put("maritalStatus", PassportData::setMaritalStatus);
        fields.put("profession", PassportData::setProfession);
        fields.put("citizenship", PassportData::setCitizenship);
        fields.put("spouseName", PassportData::setSpouseName);
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {

        if (args.length == 0) {
            handleSelf(sender);
            return true;
        }

        String sub = args[0].toLowerCase(Locale.ROOT);
        switch (sub) {
            case "reload" -> handleReload(sender);
            case "give" -> handleGive(sender, args);
            case "set" -> handleSet(sender, args);
            default -> handleView(sender, args[0]);
        }
        return true;
    }

    // ----- Подкоманды -------------------------------------------------------

    private void handleSelf(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(plugin.message("player-only"));
            return;
        }
        if (!player.hasPermission("passport.use")) {
            player.sendMessage(plugin.message("no-permission"));
            return;
        }
        PassportData data = plugin.getDatabaseManager().loadPassport(player.getUniqueId());
        plugin.getPassportGUI().open(player, player, data);
    }

    private void handleView(CommandSender sender, String targetName) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(plugin.message("player-only"));
            return;
        }
        if (!player.hasPermission("passport.view")) {
            player.sendMessage(plugin.message("no-permission"));
            return;
        }

        OfflinePlayer target = resolvePlayer(targetName);
        if (target == null) {
            player.sendMessage(plugin.message("player-not-found", "player", targetName));
            return;
        }
        if (!plugin.getDatabaseManager().hasPassport(target.getUniqueId())) {
            player.sendMessage(plugin.message("no-passport", "player", targetName));
            return;
        }

        PassportData data = plugin.getDatabaseManager().loadPassport(target.getUniqueId());
        plugin.getPassportGUI().open(player, target, data);
    }

    private void handleReload(CommandSender sender) {
        if (!sender.hasPermission("passport.admin")) {
            sender.sendMessage(plugin.message("no-permission"));
            return;
        }
        plugin.reloadConfig();
        sender.sendMessage(plugin.message("reloaded"));
    }

    private void handleGive(CommandSender sender, String[] args) {
        if (!sender.hasPermission("passport.admin")) {
            sender.sendMessage(plugin.message("no-permission"));
            return;
        }

        Player target;
        if (args.length >= 2) {
            target = Bukkit.getPlayerExact(args[1]);
            if (target == null) {
                sender.sendMessage(plugin.message("player-not-found", "player", args[1]));
                return;
            }
        } else if (sender instanceof Player player) {
            target = player;
        } else {
            sender.sendMessage(plugin.message("usage", "usage", "/passport give <игрок>"));
            return;
        }

        ItemStack passport = PassportItemListener.createPassportItem(plugin);
        Map<Integer, ItemStack> overflow = target.getInventory().addItem(passport);
        if (!overflow.isEmpty()) {
            sender.sendMessage(plugin.message("inventory-full", "player", target.getName()));
            return;
        }

        if (sender.equals(target)) {
            sender.sendMessage(plugin.message("given-self"));
        } else {
            sender.sendMessage(plugin.message("given", "player", target.getName()));
        }
    }

    private void handleSet(CommandSender sender, String[] args) {
        if (!sender.hasPermission("passport.edit")) {
            sender.sendMessage(plugin.message("no-permission"));
            return;
        }
        if (args.length < 4) {
            sender.sendMessage(plugin.message("usage", "usage",
                    "/passport set <поле> <игрок> <значение>"));
            return;
        }

        String fieldKey = matchField(args[1]);
        if (fieldKey == null) {
            sender.sendMessage(plugin.message("unknown-field",
                    "field", args[1],
                    "fields", String.join(", ", fields.keySet())));
            return;
        }

        OfflinePlayer target = resolvePlayer(args[2]);
        if (target == null) {
            sender.sendMessage(plugin.message("player-not-found", "player", args[2]));
            return;
        }

        String value = String.join(" ", Arrays.copyOfRange(args, 3, args.length));

        PassportData data = plugin.getDatabaseManager().loadPassport(target.getUniqueId());
        fields.get(fieldKey).accept(data, value);
        plugin.getDatabaseManager().savePassport(data);

        sender.sendMessage(plugin.message("field-updated",
                "field", fieldKey,
                "player", args[2],
                "value", value));
    }

    // ----- Вспомогательное --------------------------------------------------

    /**
     * Сопоставляет введённое имя поля с ключом (без учёта регистра).
     *
     * @return канонический ключ поля или {@code null}, если не найдено
     */
    private String matchField(String input) {
        for (String key : fields.keySet()) {
            if (key.equalsIgnoreCase(input)) {
                return key;
            }
        }
        return null;
    }

    /**
     * Находит игрока по имени: сначала среди онлайн, затем среди известных серверу.
     *
     * @return {@link OfflinePlayer} или {@code null}, если игрок неизвестен
     */
    private OfflinePlayer resolvePlayer(String name) {
        Player online = Bukkit.getPlayerExact(name);
        if (online != null) {
            return online;
        }
        return Bukkit.getOfflinePlayerIfCached(name);
    }

    // ----- Tab-комплит ------------------------------------------------------

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                      @NotNull String label, @NotNull String[] args) {
        List<String> suggestions = new ArrayList<>();

        if (args.length == 1) {
            if (sender.hasPermission("passport.admin")) {
                suggestions.add("give");
                suggestions.add("reload");
            }
            if (sender.hasPermission("passport.edit")) {
                suggestions.add("set");
            }
            if (sender.hasPermission("passport.view")) {
                suggestions.addAll(onlinePlayerNames());
            }
            return filter(suggestions, args[0]);
        }

        String sub = args[0].toLowerCase(Locale.ROOT);

        if (sub.equals("set")) {
            if (args.length == 2 && sender.hasPermission("passport.edit")) {
                return filter(new ArrayList<>(fields.keySet()), args[1]);
            }
            if (args.length == 3 && sender.hasPermission("passport.edit")) {
                return filter(onlinePlayerNames(), args[2]);
            }
            return suggestions;
        }

        if (sub.equals("give") && args.length == 2 && sender.hasPermission("passport.admin")) {
            return filter(onlinePlayerNames(), args[1]);
        }

        return suggestions;
    }

    private List<String> onlinePlayerNames() {
        List<String> names = new ArrayList<>();
        for (Player player : Bukkit.getOnlinePlayers()) {
            names.add(player.getName());
        }
        return names;
    }

    private List<String> filter(List<String> options, String prefix) {
        String lower = prefix.toLowerCase(Locale.ROOT);
        List<String> result = new ArrayList<>();
        for (String option : options) {
            if (option.toLowerCase(Locale.ROOT).startsWith(lower)) {
                result.add(option);
            }
        }
        return result;
    }
}
