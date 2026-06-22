package dev.passport.gui;

import dev.passport.PassportPlugin;
import dev.passport.model.PassportData;
import dev.passport.util.TextUtil;
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
import java.util.Collections;
import java.util.List;

/**
 * Строит и открывает GUI паспорта — инвентарь на 54 слота.
 * <p>
 * Голова владельца используется как «фото», остальные слоты — иконки полей.
 * Все тексты раскрашиваются через {@link TextUtil} (поддержка {@code &}-кодов,
 * hex и градиентов).
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
        String ownerName = !isBlank(data.getRpName())
                ? data.getRpName()
                : (owner.getName() != null ? owner.getName() : "???");

        String rawTitle = plugin.getConfig().getString("gui.title", "&3Паспорт — {player}");
        String title = TextUtil.colorize(rawTitle.replace("{player}", ownerName));

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
                "&6РП-имя", data.getRpName()));
        inv.setItem(SLOT_BIRTH_DATE, buildField(Material.CLOCK,
                "&6Дата рождения", data.getBirthDate()));
        inv.setItem(SLOT_BIRTH_PLACE, buildField(Material.FILLED_MAP,
                "&6Место рождения", data.getBirthPlace()));
        inv.setItem(SLOT_MARITAL, buildField(Material.GOLDEN_APPLE,
                "&6Семейное положение", data.getMaritalStatus()));
        inv.setItem(SLOT_PROFESSION, buildField(Material.IRON_PICKAXE,
                "&6Профессия", data.getProfession()));
        inv.setItem(SLOT_CITIZENSHIP, buildField(Material.OAK_SIGN,
                "&6Гражданство", data.getCitizenship()));
        inv.setItem(SLOT_SPOUSE, buildField(Material.POPPY,
                "&6Супруг(а)", data.getSpouseName()));

        viewer.openInventory(inv);
    }

    private ItemStack buildPhoto(OfflinePlayer owner, String ownerName) {
        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        ItemMeta meta = head.getItemMeta();
        if (meta instanceof SkullMeta) {
            SkullMeta skullMeta = (SkullMeta) meta;
            skullMeta.setOwningPlayer(owner);
            skullMeta.setDisplayName(TextUtil.colorize("&b&l" + ownerName));
            skullMeta.setLore(Collections.singletonList(
                    TextUtil.colorize("&7Фотография владельца")));
            head.setItemMeta(skullMeta);
        }
        return head;
    }

    private ItemStack buildField(Material material, String label, String value) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();

        meta.setDisplayName(TextUtil.colorize(label));

        String shown = isBlank(value)
                ? "&8не указано"
                : "&f" + value;
        List<String> lore = new ArrayList<>();
        lore.add(TextUtil.colorize(shown));
        meta.setLore(lore);

        item.setItemMeta(meta);
        return item;
    }

    private ItemStack buildFiller() {
        ItemStack item = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta meta = item.getItemMeta();
        // Пустое имя (один пробел), чтобы не показывать «glass pane».
        meta.setDisplayName(" ");
        item.setItemMeta(meta);
        return item;
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
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
