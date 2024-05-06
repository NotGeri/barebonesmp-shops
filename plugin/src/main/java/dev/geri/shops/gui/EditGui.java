package dev.geri.shops.gui;

import dev.geri.shops.Shops;
import dev.geri.shops.data.Container;
import dev.geri.shops.data.Per;
import dev.geri.shops.data.Shop;
import dev.geri.shops.utils.Heads;
import dev.geri.shops.utils.Strings;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.function.Consumer;

public class EditGui {

    private final Shops plugin;
    private final FileConfiguration config;
    private final Player player;
    private final Location location;

    @NotNull
    private final Container unsavedContainer;
    private final Inventory inventory;
    private final HashMap<Integer, SlotHandler> slots = new HashMap<>();

    public EditGui(Shops plugin, Player player, Location location, Container container) {
        this.plugin = plugin;
        this.config = plugin.getConfig();
        this.player = player;
        this.location = location;

        // Create a copy of the original container,
        // so we can store all the changes to it without
        // actually updating it globally
        this.unsavedContainer = container != null ? new Container(container) : new Container();

        // Initialise the underlying inventory
        this.inventory = Bukkit.createInventory(null, 54, Shops.MINI_MESSAGE.deserialize(
                Strings.placeholders(config.getString("edit-gui.title", ""), this.getPlaceholders())
        ));

        // Do the initial calculation
        this.recalculate();
    }

    /**
     * Recalculate the entire GUI based on the current unsaved inventory,
     * This is not very efficient, but in the grand scheme of things,
     * it won't ever matter and this saves a lot of additional logic
     * that could introduce bugs
     */
    private void recalculate() {
        this.slots.clear();

        // Re-initialise all the slots
        this.shopItem();
        this.perButtons();
        this.item();
        this.priceButton();
        this.untrackButton();
        this.saveButton();

        // Add the actual items to the inventory
        for (int i = 0; i < inventory.getSize(); i++) {
            SlotHandler handler = this.slots.get(i);

            ItemStack item;
            if (handler != null) {
                item = handler.item();
            } else {  // No handler, fill it with a filler
                item = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
                ItemMeta meta = item.getItemMeta();
                if (meta != null) meta.displayName(Component.empty());
                item.setItemMeta(meta);
            }

            // Attempt a default meta. Otherwise we just skip this item
            ItemMeta itemMeta = item.getItemMeta();
            if (itemMeta == null) itemMeta = Bukkit.getItemFactory().getItemMeta(item.getType());
            if (itemMeta == null) continue;

            // Set the custom item tag, just in case
            NamespacedKey key = NamespacedKey.fromString("is_gui", this.plugin);
            if (key == null) continue;
            itemMeta.getPersistentDataContainer().set(key, PersistentDataType.BOOLEAN, true);
            item.setItemMeta(itemMeta);

            // Add to the container
            this.inventory.setItem(i, item);
        }
    }

    private void shopItem() {
        ItemStack shopBook;
        if (this.unsavedContainer.shop() != null) {
            shopBook = this.applyMeta(
                    new ItemStack(Material.WRITABLE_BOOK),
                    config.getString("edit-gui.shop-button-active.name"),
                    config.getString("edit-gui.shop-button-active.description")
            );
        } else {
            shopBook = this.applyMeta(
                    new ItemStack(Material.BARRIER),
                    config.getString("edit-gui.shop-button-inactive.name"),
                    config.getString("edit-gui.shop-button-inactive.description")
            );
        }

        // Add the shop book
        this.slots.put(4, new SlotHandler(shopBook, (e) -> {
            ItemStack item = e.getCursor();

            // If they put air, we will revert
            if (item.getType().isAir()) {
                this.unsavedContainer.setShop(null);
                this.recalculate();
                return;
            }

            // Otherwise it has to be a book
            if (item.getType() != Material.WRITABLE_BOOK) {
                player.sendMessage(Shops.MINI_MESSAGE.deserialize(config.getString("messages.not-a-book", "")));
                return;
            }

            Shop shop = Shop.fromRaw(item);
            this.unsavedContainer.setShop(shop);
            this.recalculate();
        }));
    }

    private void perButtons() {

        LinkedHashMap<Per, ItemStack> perItems = new LinkedHashMap<>() {{
            ItemStack stack = applyMeta(new ItemStack(Material.APPLE), config.getString("edit-gui.per-buttons.stack.name"), config.getString("edit-gui.per-buttons.stack.description"));
            stack.setAmount(64);
            this.put(Per.PIECE, applyMeta(new ItemStack(Material.APPLE), config.getString("edit-gui.per-buttons.piece.name"), config.getString("edit-gui.per-buttons.piece.description")));
            this.put(Per.STACK, stack);
            this.put(Per.SHULKER, applyMeta(new ItemStack(Material.RED_SHULKER_BOX), config.getString("edit-gui.per-buttons.shulker.name"), config.getString("edit-gui.per-buttons.shulker.description")));
        }};

        // Default to piece
        if (unsavedContainer.per() == null) this.unsavedContainer.setPer(Per.PIECE);

        int perIndex = 0;
        for (Map.Entry<Per, ItemStack> entry : perItems.entrySet()) {
            ItemStack item = entry.getValue();
            if (unsavedContainer.per() == entry.getKey()) {
                ItemMeta meta = item.getItemMeta();
                meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
                meta.addEnchant(Enchantment.BINDING_CURSE, 1, true);
                item.setItemMeta(meta);
            }

            this.slots.put(10 + perIndex * 9, new SlotHandler(item, (e) -> {
                this.unsavedContainer.setPer(entry.getKey());
                this.recalculate();
            }));
            perIndex++;
        }
    }

    private void item() {
        ItemStack selectedItem;
        if (unsavedContainer.material() != null) {
            selectedItem = this.applyMeta(
                    new ItemStack(unsavedContainer.material()),
                    unsavedContainer.customName() != null && !unsavedContainer.customName().isEmpty() ? unsavedContainer.customName() : this.config.getString("edit-gui.item.not-custom-name"),
                    this.config.getString("edit-gui.item.description")
            );

        } else { // Revert to the placeholder item
            selectedItem = this.applyMeta(
                    new ItemStack(Material.BARRIER),
                    config.getString("edit-gui.item-placeholder.name"),
                    config.getString("edit-gui.item-placeholder.description")
            );
        }

        // Default to 1
        if (unsavedContainer.amount() < 1) this.unsavedContainer.setAmount(1);

        this.slots.put(22, new SlotHandler(selectedItem, (e) -> {
            ItemStack cursorItem = e.getCursor();

            // If they put air, we will adjust the amount
            if (cursorItem.getType().isAir()) {
                int newCount = this.unsavedContainer.amount() + (e.isShiftClick() ? 10 : 1) * (e.getClick().isRightClick() ? 1 : -1);
                if (newCount < 1) return;
                this.unsavedContainer.setAmount(newCount);
                this.recalculate();
                return;
            }

            // Copy the custom name if there is one
            ItemStack newItem = new ItemStack(cursorItem.getType());
            ItemMeta newItemMeta = newItem.getItemMeta();
            if (cursorItem.getItemMeta() != null) {
                Component displayName = cursorItem.getItemMeta().displayName();
                if (displayName != null) newItemMeta.displayName(displayName);
            }

            ItemMeta meta = cursorItem.getItemMeta();
            Component displayName = meta.displayName();
            this.unsavedContainer.setCustomName(displayName != null ? Shops.MINI_MESSAGE.serialize(displayName) : null);
            this.unsavedContainer.setMaterial(newItem.getType());
            this.recalculate();
        }));
    }

    private void priceButton() {
        this.slots.put(25, new SlotHandler(this.applyMeta(
                new ItemStack(Material.DIAMOND),
                config.getString("edit-gui.price-button.name"),
                config.getString("edit-gui.price-button.description")
        ), (e) -> {
            int newPrice = this.unsavedContainer.price() + (e.isShiftClick() ? 10 : 1) * (e.getClick().isRightClick() ? 1 : -1);
            if (newPrice < 0) return;
            this.unsavedContainer.setPrice(newPrice);
            this.recalculate();
        }));
    }

    private void untrackButton() {
        this.slots.put(48, new SlotHandler(this.applyMeta(
                Heads.getPlayerHeadItem("beb588b21a6f98ad1ff4e085c552dcb050efc9cab427f46048f18fc803475f7"),
                config.getString("edit-gui.untrack-button.name"),
                config.getString("edit-gui.untrack-button.description")
        ), (e) -> {
            this.plugin.data().removeContainer(location);
            this.player.sendMessage(Shops.MINI_MESSAGE.deserialize(this.config.getString("messages.container-untracked", "")));
            player.closeInventory();
        }));
    }

    private void saveButton() {

        this.slots.put(50, new SlotHandler(this.applyMeta(
                Heads.getPlayerHeadItem("a92e31ffb59c90ab08fc9dc1fe26802035a3a47c42fee63423bcdb4262ecb9b6"),
                config.getString("edit-gui.save-button.name", ""),
                config.getString("edit-gui.save-button.description", "")
        ), (e) -> {

            // Save the shop first
            if (unsavedContainer.shop() != null) this.plugin.data().saveShop(unsavedContainer.shop());

            // Update the container
            this.plugin.data().saveContainer(location, unsavedContainer);
            this.player.sendMessage(Shops.MINI_MESSAGE.deserialize(this.config.getString("messages.container-saved", "")));
            this.close();
        }));
    }

    /**
     * @return Get the map of placeholders for this edit GUI context
     */
    private Map<String, Object> getPlaceholders() {
        Shop shop = this.unsavedContainer.shop();
        return Map.of(
                "%x%", location.getBlockX(),
                "%y%", location.getBlockY(),
                "%z%", location.getBlockZ(),

                "%price%", this.unsavedContainer.price(),
                "%amount%", this.unsavedContainer.amount(),
                "%per%", this.unsavedContainer.per() != null ? this.unsavedContainer.per().format(this.unsavedContainer.amount()) : "?",
                "%item%", this.unsavedContainer.formattedName(),

                "%shop_name%", shop != null ? shop.name() : "?",
                "%shop_description%", shop != null ? this.unsavedContainer.shop().name() : "?",
                "%shop_owners%", shop != null && shop.owners() != null ? String.join(", ", this.unsavedContainer.shop().owners()) : "-"
        );
    }

    /**
     * Apply a name and description item meta to an item
     * This will also parse the available placeholders
     */
    public ItemStack applyMeta(ItemStack item, String name, String description) {

        Map<String, Object> placeholders = this.getPlaceholders();

        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;
        if (description != null && !description.isEmpty()) {
            List<Component> lore = new ArrayList<>();
            for (String line : description.split("\n")) {
                lore.add(Shops.MINI_MESSAGE.deserialize(Strings.placeholders(line, placeholders)));
            }
            meta.lore(lore);
        }
        meta.displayName(name != null ? Shops.MINI_MESSAGE.deserialize(Strings.placeholders(name, placeholders)) : Component.empty());
        item.setItemMeta(meta);
        return item;
    }

    /**
     * Handle clicking in the GUI
     */
    public void onInventoryClick(InventoryClickEvent e) {
        // Ensure they are clicking in the GUI and not in their inventory
        if (e.getClickedInventory() != e.getInventory()) return;

        // It doesn't matter what happens after this; we want to cancel the event
        e.setCancelled(true);

        SlotHandler handler = this.slots.get(e.getSlot());
        if (handler != null && handler.action() != null) {
            handler.action().accept(e);
        }
    }

    /**
     * Recalculate the stock when they close the custom inventory
     */
    public void close() {
        this.player.closeInventory();

        // Attempt to recalculate the stock of the underlying container
        Location location = this.plugin.getTrackableBlockLocation(this.location.getWorld().getBlockAt(this.location));
        if (location == null) return;

        Block block = location.getBlock();
        if (block.getState() instanceof org.bukkit.block.Container c) {
            this.plugin.recalculateStock(this.location, this.unsavedContainer, c.getInventory());
        }
    }

    /**
     * @return The underlying GUI
     */
    public Inventory inventory() {
        return this.inventory;
    }

    public record SlotHandler(ItemStack item, Consumer<InventoryClickEvent> action) {}

}
