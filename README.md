# PassportPlugin

Плагин паспортной системы для РП-серверов **Minecraft (PaperMC 1.21.4)**.
Каждый игрок получает паспорт с РП-данными персонажа, который открывается удобным GUI и хранится в базе данных. Есть публичный API для интеграции с другими плагинами (например, свадебным).

## Возможности

- 📖 Паспорт-предмет (`PAPER`), открывается по ПКМ
- 🖼️ GUI на 54 слота с головой игрока в роли фото
- 💾 Хранение данных в SQLite (без внешней БД)
- 🎨 Полная поддержка [MiniMessage](https://docs.advntr.dev/minimessage/) — все тексты настраиваются
- 🔐 Гибкие права, совместимо с **LuckPerms** и **PermissionsEx**
- 🔌 Публичный API для других плагинов
- ♻️ Горячая перезагрузка конфигурации

## Требования

| | |
|---|---|
| Сервер | PaperMC **1.21.4** |
| Java | **21+** |
| Сборка | Maven |

## Установка

1. Скачайте или соберите `PassportPlugin-1.0.0.jar` (см. [Сборка](#сборка)).
2. Положите jar в папку `plugins/` сервера.
3. Запустите сервер — сгенерируются `config.yml` и база данных.
4. (Опционально) Настройте права в LuckPerms/PermissionsEx.

## Сборка

```bash
mvn clean package
```

Готовый jar появится в `target/PassportPlugin-1.0.0.jar`.
Драйвер SQLite шейдится внутрь jar (`maven-shade-plugin`, релокация `org.sqlite` → `dev.passport.libs.sqlite`), поэтому дополнительных зависимостей на сервере не требуется.

## Поля паспорта

| Поле | Описание |
|------|----------|
| `rpName` | РП-имя персонажа |
| `birthDate` | Дата рождения (напр. `15.03.1995`) |
| `birthPlace` | Место рождения |
| `maritalStatus` | Семейное положение |
| `profession` | Профессия / работа |
| `citizenship` | Гражданство / город |
| `spouseName` | РП-имя супруга / супруги |

## Команды

| Команда | Право | Описание |
|---------|-------|----------|
| `/passport` | `passport.use` | Открыть свой паспорт |
| `/passport <игрок>` | `passport.view` | Открыть паспорт другого игрока |
| `/passport set <поле> <игрок> <значение>` | `passport.edit` | Редактировать поле паспорта |
| `/passport give [игрок]` | `passport.admin` | Выдать предмет-паспорт |
| `/passport reload` | `passport.admin` | Перезагрузить `config.yml` |

Алиасы: `/pass`, `/pasport`.

## Права

| Право | По умолчанию | Описание |
|-------|--------------|----------|
| `passport.use` | `true` | Открыть свой паспорт |
| `passport.view` | `op` | Смотреть паспорта других игроков |
| `passport.edit` | `op` | Редактировать данные (для чиновников/клерков) |
| `passport.admin` | `op` | Полный доступ (включает все права выше) |

## Конфигурация

Файл `config.yml` создаётся при первом запуске. Можно настроить:

- **Предмет-паспорт** — материал, `CustomModelData` для ресурспака, название и lore.
- **GUI** — заголовок инвентаря (плейсхолдер `{player}`).
- **Сообщения** — все тексты плагина с поддержкой MiniMessage.

После изменений примените их командой `/passport reload`.

## API для разработчиков

PassportPlugin предоставляет публичный API для интеграции (например, со свадебным плагином):

```java
PassportAPI api = PassportAPI.getInstance();

// Получить паспорт игрока
PassportData passport = api.getPassport(uuid);

// Обновить семейное положение и супруга
api.setMaritalStatus(uuid, "Женат/Замужем");
api.setSpouseName(uuid, "Мария Иванова");

// Сохранить изменения
api.savePassport(passport);
```

Добавьте PassportPlugin в `softdepend` (или `depend`) в вашем `plugin.yml`.

## Структура проекта

```
dev.passport
├── PassportPlugin.java        — главный класс
├── model/PassportData.java    — модель данных
├── database/DatabaseManager.java — SQLite: load/save/create
├── gui/PassportGUI.java       — инвентарь на 54 слота
├── command/PassportCommand.java  — обработчик /passport
├── listener/PassportItemListener.java — ПКМ по предмету
└── api/PassportAPI.java       — публичный API
```

## Лицензия

Проект распространяется на усмотрение автора.

---

Автор: **Benovich**