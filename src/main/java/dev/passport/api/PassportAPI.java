package dev.passport.api;

import dev.passport.PassportPlugin;
import dev.passport.database.DatabaseManager;
import dev.passport.model.PassportData;

import java.util.UUID;

/**
 * Публичный API паспортной системы для интеграции с другими плагинами
 * (например, со свадебным плагином).
 * <p>
 * Доступ — через синглтон {@link #getInstance()}. Экземпляр инициализируется
 * плагином в {@code onEnable()} и обнуляется в {@code onDisable()}.
 *
 * <pre>{@code
 * PassportAPI api = PassportAPI.getInstance();
 * if (api != null) {
 *     api.setMaritalStatus(uuid, "Женат/Замужем");
 *     api.setSpouseName(uuid, "Мария Иванова");
 * }
 * }</pre>
 */
public final class PassportAPI {

    private static PassportAPI instance;

    private final PassportPlugin plugin;
    private final DatabaseManager database;

    private PassportAPI(PassportPlugin plugin) {
        this.plugin = plugin;
        this.database = plugin.getDatabaseManager();
    }

    /**
     * Инициализирует синглтон. Вызывается только из {@link PassportPlugin#onEnable()}.
     *
     * @param plugin экземпляр плагина
     * @return созданный экземпляр API
     */
    public static PassportAPI init(PassportPlugin plugin) {
        if (instance == null) {
            instance = new PassportAPI(plugin);
        }
        return instance;
    }

    /**
     * Сбрасывает синглтон. Вызывается из {@link PassportPlugin#onDisable()}.
     */
    public static void shutdown() {
        instance = null;
    }

    /**
     * @return текущий экземпляр API, либо {@code null}, если плагин ещё не включён
     */
    public static PassportAPI getInstance() {
        return instance;
    }

    /**
     * Загружает паспорт игрока из базы. Если паспорта нет — создаётся пустой.
     *
     * @param uuid UUID игрока
     * @return паспорт игрока, никогда не {@code null}
     */
    public PassportData getPassport(UUID uuid) {
        return database.loadPassport(uuid);
    }

    /**
     * Проверяет наличие записи паспорта в базе.
     *
     * @param uuid UUID игрока
     * @return {@code true}, если паспорт существует
     */
    public boolean hasPassport(UUID uuid) {
        return database.hasPassport(uuid);
    }

    /**
     * Сохраняет переданный паспорт в базу.
     *
     * @param passport данные паспорта
     */
    public void savePassport(PassportData passport) {
        database.savePassport(passport);
    }

    /**
     * Устанавливает семейное положение и сразу сохраняет паспорт.
     *
     * @param uuid          UUID игрока
     * @param maritalStatus новое семейное положение
     */
    public void setMaritalStatus(UUID uuid, String maritalStatus) {
        PassportData data = database.loadPassport(uuid);
        data.setMaritalStatus(maritalStatus);
        database.savePassport(data);
    }

    /**
     * Устанавливает РП-имя супруга/супруги и сразу сохраняет паспорт.
     *
     * @param uuid       UUID игрока
     * @param spouseName РП-имя супруга/супруги
     */
    public void setSpouseName(UUID uuid, String spouseName) {
        PassportData data = database.loadPassport(uuid);
        data.setSpouseName(spouseName);
        database.savePassport(data);
    }

    /**
     * @return экземпляр плагина
     */
    public PassportPlugin getPlugin() {
        return plugin;
    }
}
