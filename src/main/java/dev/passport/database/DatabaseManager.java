package dev.passport.database;

import dev.passport.PassportPlugin;
import dev.passport.model.PassportData;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.UUID;

/**
 * Управляет SQLite-базой паспортов: подключение, создание таблицы,
 * загрузка и сохранение данных.
 * <p>
 * Драйвер {@code org.xerial:sqlite-jdbc} шейдится в jar и регистрируется
 * автоматически через {@link java.sql.DriverManager} (ServiceLoader, JDBC 4.0+),
 * поэтому явный {@code Class.forName(...)} не требуется.
 */
public class DatabaseManager {

    private final PassportPlugin plugin;
    private Connection connection;

    public DatabaseManager(PassportPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Открывает соединение с базой и создаёт таблицу при необходимости.
     *
     * @throws SQLException если не удалось подключиться или создать таблицу
     */
    public void connect() throws SQLException {
        if (!plugin.getDataFolder().exists() && !plugin.getDataFolder().mkdirs()) {
            plugin.getLogger().warning("Не удалось создать папку данных плагина.");
        }

        File dbFile = new File(plugin.getDataFolder(), "passports.db");
        String url = "jdbc:sqlite:" + dbFile.getAbsolutePath();

        this.connection = DriverManager.getConnection(url);
        createTable();
    }

    /**
     * Закрывает соединение с базой. Безопасно вызывать повторно.
     */
    public void disconnect() {
        if (connection == null) {
            return;
        }
        try {
            if (!connection.isClosed()) {
                connection.close();
            }
        } catch (SQLException e) {
            plugin.getLogger().warning("Ошибка при закрытии соединения с базой: " + e.getMessage());
        } finally {
            connection = null;
        }
    }

    private void createTable() throws SQLException {
        String sql = "CREATE TABLE IF NOT EXISTS passports ("
                + "uuid           TEXT PRIMARY KEY,"
                + "rp_name        TEXT NOT NULL DEFAULT '',"
                + "birth_date     TEXT NOT NULL DEFAULT '',"
                + "birth_place    TEXT NOT NULL DEFAULT '',"
                + "marital_status TEXT NOT NULL DEFAULT '',"
                + "profession     TEXT NOT NULL DEFAULT '',"
                + "citizenship    TEXT NOT NULL DEFAULT '',"
                + "spouse_name    TEXT NOT NULL DEFAULT ''"
                + ")";
        try (Statement statement = connection.createStatement()) {
            statement.execute(sql);
        }
    }

    /**
     * Загружает паспорт игрока. Если записи нет — создаёт новую пустую запись
     * в базе и возвращает соответствующий объект.
     *
     * @param uuid UUID игрока
     * @return паспорт игрока, никогда не {@code null}
     */
    public PassportData loadPassport(UUID uuid) {
        String sql = "SELECT * FROM passports WHERE uuid = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, uuid.toString());
            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    return new PassportData(
                            uuid,
                            rs.getString("rp_name"),
                            rs.getString("birth_date"),
                            rs.getString("birth_place"),
                            rs.getString("marital_status"),
                            rs.getString("profession"),
                            rs.getString("citizenship"),
                            rs.getString("spouse_name")
                    );
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Не удалось загрузить паспорт " + uuid + ": " + e.getMessage());
            return new PassportData(uuid);
        }

        // Записи нет — создаём пустую и сохраняем.
        PassportData data = new PassportData(uuid);
        savePassport(data);
        return data;
    }

    /**
     * Проверяет, существует ли запись паспорта в базе.
     *
     * @param uuid UUID игрока
     * @return {@code true}, если запись существует
     */
    public boolean hasPassport(UUID uuid) {
        String sql = "SELECT 1 FROM passports WHERE uuid = ? LIMIT 1";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, uuid.toString());
            try (ResultSet rs = statement.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Не удалось проверить паспорт " + uuid + ": " + e.getMessage());
            return false;
        }
    }

    /**
     * Сохраняет (вставляет или обновляет) паспорт в базе через UPSERT.
     *
     * @param data данные паспорта
     */
    public void savePassport(PassportData data) {
        String sql = "INSERT INTO passports "
                + "(uuid, rp_name, birth_date, birth_place, marital_status, profession, citizenship, spouse_name) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?) "
                + "ON CONFLICT(uuid) DO UPDATE SET "
                + "rp_name        = excluded.rp_name,"
                + "birth_date     = excluded.birth_date,"
                + "birth_place    = excluded.birth_place,"
                + "marital_status = excluded.marital_status,"
                + "profession     = excluded.profession,"
                + "citizenship    = excluded.citizenship,"
                + "spouse_name    = excluded.spouse_name";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, data.getUuid().toString());
            statement.setString(2, data.getRpName());
            statement.setString(3, data.getBirthDate());
            statement.setString(4, data.getBirthPlace());
            statement.setString(5, data.getMaritalStatus());
            statement.setString(6, data.getProfession());
            statement.setString(7, data.getCitizenship());
            statement.setString(8, data.getSpouseName());
            statement.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().severe("Не удалось сохранить паспорт " + data.getUuid() + ": " + e.getMessage());
        }
    }
}