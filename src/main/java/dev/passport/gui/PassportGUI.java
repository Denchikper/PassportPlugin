package dev.passport.gui;

import dev.passport.PassportPlugin;
import dev.passport.model.PassportData;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.ArrayList;
import java.util.List;

/**
 * Строит и открывает GUI паспорта — инвентарь на 54 слота.
 * <p>
 * Голова владельца используется как «фото», остальные слоты — иконки полей.
 * Все тексты формируются через MiniMessage.
 */
public class PassportGUI {

    private static final int SIZE = 54;

    // Расположение иконок полей в сетке 9x6.
    private static final int SLOT_PHOTO = 4;
    private static final int SLOT_RP_NAME = 19;
    private static final int SLOT_BIRTH_DATE = 21;
    private static final int SLOT_BIRTH_PLACE = 23;
    private static final int SLOT_MARITAL = 25;
    private static final int SLOT_PROFESSION = 29;
    private static final int SLOT_CITIZENSHIP = 31;
    private static final int SLOT_SPOUSE = 33;

    private final PassportPlugin plugin;
    private final MiniMessage mm = MiniMessage.miniMessage();

    public PassportGUI(PassportPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Открывает паспорт {@code owner} для зрителя {@code viewer}.
     *
     * @param viewer игрок, которому показываем инвентарь
     * @param owner  владелец паспорта (может быть оффлайн)
     * @param data   данные паспорта
     */
    public void open(Player viewer, OfflinePlayer owner, PassportData data) {
        String ownerName = !data.getRpName().isBlank()
                ? data.getRpName()
                : (owner.getName() != null ? owner.getName() : "???");

        String rawTitle = plugin.getConfig().getString("gui.title", "<dark_aqua>Паспорт — {player}</dark_aqua>");
        Component title = deserialize(rawTitle.replace("{player}", ownerName));

        PassportHolder holder = new PassportHolder(owner);
        Inventory inv = Bukkit.createInventory(holder, SIZE, title);
        holder.setInventory(inv);

        // Фон — серые стеклянные панели.
        ItemStack filler = buildFiller();
        for (int i = 0; i < SIZE; i++) {
            inv.setItem(i, filler);
        }

        // Фото владельца.
        inv.setItem(SLOT_PHOTO, buildPhoto(owner, ownerName));

        // Иконки полей.
        inv.setItem(SLOT_RP_NAME, buildField(Material.NAME_TAG,
                "<gold>РП-имя</gold>", data.getRpName()));
        inv.setItem(SLOT_BIRTH_DATE, buildField(Material.CLOCK,
                "<gold>Дата рождения</gold>", data.getBirthDate()));
        inv.setItem(SLOT_BIRTH_PLACE, buildField(Material.FILLED_MAP,
                "<gold>Место рождения</gold>", data.getBirthPlace()));
        inv.setItem(SLOT_MARITAL, buildField(Material.GOLDEN_APPLE,
                "<gold>Семейное положение</gold>", data.getMaritalStatus()));
        inv.setItem(SLOT_PROFESSION, buildField(Material.IRON_PICKAXE,
                "<gold>Профессия</gold>", data.getProfession()));
        inv.setItem(SLOT_CITIZENSHIP, buildField(Material.OAK_SIGN,
                "<gold>Гражданство</gold>", data.getCitizenship()));
        inv.setItem(SLOT_SPOUSE, buildField(Material.POPPY,
                "<gold>Супруг(а)</gold>", data.getSpouseName()));

        viewer.openInventory(inv);
    }

    private ItemStack buildPhoto(OfflinePlayer owner, String ownerName) {
        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        ItemMeta meta = head.getItemMeta();
        if (meta instanceof SkullMeta skullMeta) {
            skullMeta.setOwningPlayer(owner);
            skullMeta.displayName(deserialize("<aqua><bold>" + escape(ownerName) + "</bold></aqua>"));
            List<Component> lore = new ArrayList<>();
            lore.add(deserialize("<gray>Фотография владельца</gray>"));
            skullMeta.lore(lore);
            head.setItemMeta(skullMeta);
        }
        return head;
    }

    private ItemStack buildField(Material material, String label, String value) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();

        meta.displayName(deserialize(label));

        List<Component> lore = new ArrayList<>();
        String shown = (value == null || value.isBlank()) ? "<dark_gray><i>не указано</i></dark_gray>" : "<white>" + escape(value) + "</white>";
        lore.add(deserialize(shown));
        meta.lore(lore);

        item.setItemMeta(meta);
        return item;
    }

    private ItemStack buildFiller() {
        ItemStack item = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta meta = item.getItemMeta();
        // Пустое непрозрачное имя, чтобы не показывать «glass pane».
        meta.displayName(Component.empty().decoration(TextDecoration.ITALIC, false));
        item.setItemMeta(meta);
        return item;
    }

    private Component deserialize(String miniMessage) {
        // По умолчанию отключаем курсив, характерный для имён предметов.
        return mm.deserialize(miniMessage).decoration(TextDecoration.ITALIC, false);
    }

    /**
     * Экранирует пользовательский текст, чтобы он не интерпретировался как теги MiniMessage.
     */
    private String escape(String input) {
        return mm.escapeTags(input);
    }

    /**
     * Держатель инвентаря паспорта. Позволяет слушателю однозначно опознать
     * этот GUI и узнать владельца паспорта.
     */
    public static final class PassportHolder implements InventoryHolder {

        private final OfflinePlayer owner;
        private Inventory inventory;

        public PassportHolder(OfflinePlayer owner) {
            this.owner = owner;
        }

        public OfflinePlayer getOwner() {
            return owner;
        }

        void setInventory(Inventory inventory) {
            this.inventory = inventory;
        }

        @Override
        public Inventory getInventory() {
            return inventory;
        }
    }
}
