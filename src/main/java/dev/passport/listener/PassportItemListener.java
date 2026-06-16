package dev.passport.listener;

import dev.passport.PassportPlugin;
import dev.passport.gui.PassportGUI;
import dev.passport.model.PassportData;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;

/**
 * Обрабатывает взаимодействие с предметом-паспортом (ПКМ для открытия)
 * и защищает GUI паспорта от перемещения предметов.
 */
public class PassportItemListener implements Listener {

    private static final MiniMessage MM = MiniMessage.miniMessage();

    private final PassportPlugin plugin;
    private final NamespacedKey passportKey;

    public PassportItemListener(PassportPlugin plugin) {
        this.plugin = plugin;
        this.passportKey = new NamespacedKey(plugin, "passport_item");
    }

    /**
     * Создаёт предмет-паспорт согласно настройкам config.yml.
     *
     * @param plugin экземпляр плагина (для чтения конфига и NamespacedKey)
     * @return готовый предмет-паспорт
     */
    public static ItemStack createPassportItem(PassportPlugin plugin) {
        Material material = Material.matchMaterial(
                plugin.getConfig().getString("item.material", "PAPER"));
        if (material == null) {
            material = Material.PAPER;
        }

        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();

        String name = plugin.getConfig().getString("item.name", "<gold><bold>Паспорт</bold></gold>");
        meta.displayName(MM.deserialize(name).decoration(TextDecoration.ITALIC, false));

        List<String> rawLore = plugin.getConfig().getStringList("item.lore");
        if (!rawLore.isEmpty()) {
            List<Component> lore = new ArrayList<>();
            for (String line : rawLore) {
                lore.add(MM.deserialize(line).decoration(TextDecoration.ITALIC, false));
            }
            meta.lore(lore);
        }

        int cmd = plugin.getConfig().getInt("item.custom-model-data", 0);
        if (cmd > 0) {
            meta.setCustomModelData(cmd);
        }

        // Метка, по которой опознаём паспорт.
        meta.getPersistentDataContainer().set(
                new NamespacedKey(plugin, "passport_item"),
                PersistentDataType.BYTE,
                (byte) 1);

        item.setItemMeta(meta);
        return item;
    }

    /**
     * Проверяет, является ли предмет паспортом (по метке в PersistentDataContainer).
     */
    public boolean isPassportItem(ItemStack item) {
        if (item == null || item.getType() == Material.AIR || !item.hasItemMeta()) {
            return false;
        }
        PersistentDataContainer container = item.getItemMeta().getPersistentDataContainer();
        return container.has(passportKey, PersistentDataType.BYTE);
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        // Реагируем только на правый клик основной рукой, чтобы не открывать GUI дважды.
        if (event.getHand() != EquipmentSlot.HAND) {
            return;
        }
        Action action = event.getAction();
        if (action != Action.RIGHT_CLICK_AIR && action != Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        ItemStack item = event.getItem();
        if (!isPassportItem(item)) {
            return;
        }

        event.setCancelled(true);

        Player player = event.getPlayer();
        if (!player.hasPermission("passport.use")) {
            player.sendMessage(plugin.message("no-permission"));
            return;
        }

        PassportData data = plugin.getDatabaseManager().loadPassport(player.getUniqueId());
        plugin.getPassportGUI().open(player, player, data);
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        // Паспорт — только для просмотра: запрещаем любые перемещения внутри GUI.
        if (event.getInventory().getHolder() instanceof PassportGUI.PassportHolder) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        if (event.getInventory().getHolder() instanceof PassportGUI.PassportHolder) {
            event.setCancelled(true);
        }
    }
}
