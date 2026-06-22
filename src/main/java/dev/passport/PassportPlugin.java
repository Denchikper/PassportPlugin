package dev.passport;

import dev.passport.api.PassportAPI;
import dev.passport.command.PassportCommand;
import dev.passport.database.DatabaseManager;
import dev.passport.gui.PassportGUI;
import dev.passport.listener.PassportItemListener;
import dev.passport.util.TextUtil;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

import java.sql.SQLException;

/**
 * Главный класс плагина паспортной системы.
 * <p>
 * Отвечает за инициализацию базы данных, GUI, команд, слушателей и публичного API.
 */
public final class PassportPlugin extends JavaPlugin {

    private DatabaseManager databaseManager;
    private PassportGUI passportGUI;

    @Override
    public void onEnable() {
        // Сохраняем config.yml по умолчанию, если его ещё нет.
        saveDefaultConfig();

        // База данных.
        this.databaseManager = new DatabaseManager(this);
        try {
            databaseManager.connect();
        } catch (SQLException e) {
            getLogger().severe("Не удалось подключиться к базе данных. Плагин будет отключён.");
            getLogger().severe(e.getMessage());
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        // GUI.
        this.passportGUI = new PassportGUI(this);

        // Публичный API.
        PassportAPI.init(this);

        // Команда.
        PluginCommand command = getCommand("passport");
        if (command != null) {
            PassportCommand handler = new PassportCommand(this);
            command.setExecutor(handler);
            command.setTabCompleter(handler);
        } else {
            getLogger().warning("Команда 'passport' не найдена в plugin.yml.");
        }

        // Слушатели.
        getServer().getPluginManager().registerEvents(new PassportItemListener(this), this);

        getLogger().info("PassportPlugin включён.");
    }

    @Override
    public void onDisable() {
        PassportAPI.shutdown();
        if (databaseManager != null) {
            databaseManager.disconnect();
        }
        getLogger().info("PassportPlugin отключён.");
    }

    public DatabaseManager getDatabaseManager() {
        return databaseManager;
    }

    public PassportGUI getPassportGUI() {
        return passportGUI;
    }

    /**
     * Формирует сообщение из config.yml по ключу {@code messages.<key>},
     * подставляет префикс и заменяет плейсхолдеры.
     *
     * @param key          ключ сообщения (без {@code messages.})
     * @param replacements пары вида {@code "ключ", "значение", ...}
     *                     для подстановки {@code {ключ}} в тексте
     * @return готовая к отправке строка с цветовыми кодами
     */
    public String message(String key, String... replacements) {
        String prefix = getConfig().getString("messages.prefix", "");
        String raw = getConfig().getString("messages." + key, key);

        // Подстановка плейсхолдеров вида {name} простой заменой строк.
        if (replacements.length > 0) {
            for (int i = 0; i + 1 < replacements.length; i += 2) {
                raw = raw.replace("{" + replacements[i] + "}",
                        String.valueOf(replacements[i + 1]));
            }
        }

        return TextUtil.colorize(prefix + raw);
    }
}
