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
import org.bukkit.inventory.meta.BookMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class EditGui {

    private final Shops plugin;
    private final FileConfiguration config;
    private final Player player;
    private final Location location;

    @NotNull
    private Container unsavedContainer;
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
        this.copyButton();
        this.pasteButton();
        this.untrackButton();
        this.saveButton();

        // Add the actual items to the inventory
        for (int i = 0; i < inventory.getSize(); i++) {
            SlotHandler handler = this.slots.get(i);

            ItemStack item;
            if (handler != null) {
                item = handler.item;
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
        this.slots.put(4, new SlotHandler(shopBook, ShiftClick.SPECIFIC.setMaterial(Material.WRITABLE_BOOK), (e) -> {
            ItemStack item = e.getClick().isShiftClick() ? e.getCurrentItem() : e.getCursor();

            // If they put air, we will revert
            if (item == null || item.getType().isAir()) {
                this.unsavedContainer.setShop(null);
                this.recalculate();
                return;
            }

            // Otherwise it has to be a book
            if (item.getType() != Material.WRITABLE_BOOK) {
                player.sendMessage(Shops.MINI_MESSAGE.deserialize(config.getString("messages.not-a-book", "")));
                return;
            }

            // Get the book's raw description
            BookMeta meta = (BookMeta) item.getItemMeta();
            StringBuilder sb = new StringBuilder();
            for (Component page : meta.pages()) sb.append(Shops.MINI_MESSAGE.serialize(page));
            String raw = sb.toString();

            // Carve out the specific values
            Pattern pattern = Pattern.compile("Name: (?<name>.+)\\s*(?:Owners: (?<owners>.+))?\\s*(?:Description: (?<description>[\\s\\S]+))?");
            Matcher matcher = pattern.matcher(raw);
            Shop shop = new Shop();
            if (matcher.find()) {
                String name = matcher.group("name");
                String ownersRaw = matcher.group("owners");
                String descriptionRaw = matcher.group("description");
                List<String> owners = ownersRaw != null ? Arrays.asList(ownersRaw.split(",\\s*")) : new ArrayList<>();
                shop.setName(name).setOwners(owners).setDescription(descriptionRaw);
            }

            // Make sure we have a name at least
            if (shop.name() == null || shop.name().isEmpty()) {
                player.sendMessage(Shops.MINI_MESSAGE.deserialize(config.getString("messages.missing-name", "")));
                return;
            }

            if (shop.description() != null && shop.description().length() > 100) {
                player.sendMessage(Shops.MINI_MESSAGE.deserialize(config.getString("messages.description-too-long", "")));
                return;
            }

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
                this.enchant(item);
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

        this.slots.put(22, new SlotHandler(selectedItem, ShiftClick.ANY, (e) -> {
            ItemStack cursorItem = e.getClick().isShiftClick() ? e.getCurrentItem() : e.getCursor();

            // If they put air, we will adjust the amount
            if (cursorItem == null || cursorItem.getType().isAir()) {
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

    private void copyButton() {
        Container pendingCopy = this.plugin.data().getPendingCopy(this.player.getUniqueId());
        ItemStack item = new ItemStack(Material.MAP);
        if (pendingCopy != null) this.enchant(item);

        this.slots.put(45, new SlotHandler(this.applyMeta(
                item,
                config.getString("edit-gui.copy-button.name"),
                config.getString("edit-gui.copy-button.description")
        ), (e) -> {
            if (e.isShiftClick()) {
                this.plugin.data().removePendingCopy(player.getUniqueId());
            } else {
                this.player.sendMessage(Shops.MINI_MESSAGE.deserialize(this.config.getString("messages.container-copied", "")));
                this.plugin.data().setPendingCopy(player.getUniqueId(), new Container(unsavedContainer));
            }
            this.recalculate();
        }));
    }

    private void pasteButton() {
        Container pendingCopy = this.plugin.data().getPendingCopy(this.player.getUniqueId());
        if (pendingCopy == null) return;

        this.slots.put(46, new SlotHandler(this.applyMeta(
                new ItemStack(Material.FILLED_MAP),
                config.getString("edit-gui.paste-button.name"),
                config.getString("edit-gui.paste-button.description")
        ), (e) -> {
            this.unsavedContainer = pendingCopy;
            this.player.sendMessage(Shops.MINI_MESSAGE.deserialize(this.config.getString("messages.container-pasted", "")));
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
            this.plugin.getLogger().info("%s untracked container %s %s %s".formatted(this.player.getName(), location.getBlockX(), location.getBlockY(), location.getBlockY()));
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
            this.plugin.getLogger().info("%s saved container %s %s %s".formatted(this.player.getName(), location.getBlockX(), location.getBlockY(), location.getBlockY()));
            this.close();
        }));
    }

    /**
     * @return Get the map of placeholders for this edit GUI context
     */
    private Map<String, Object> getPlaceholders() {

        HashMap<String, Object> placeholders = new HashMap<>() {{
            this.put("%x%", location.getBlockX());
            this.put("%y%", location.getBlockY());
            this.put("%z%", location.getBlockZ());
        }};

        BiConsumer<Container, String> applyContainerPlaceholders = (container, prefix) -> {
            placeholders.put("%" + prefix + "price%", container != null ? container.price() : "?");
            placeholders.put("%" + prefix + "amount%", container != null ? container.amount() : "");
            placeholders.put("%" + prefix + "per%", container != null && container.per() != null ? container.per().format(container.amount()) : "?");
            placeholders.put("%" + prefix + "item%", container != null ? container.formattedName() : "?");

            Shop shop = container != null ? container.shop() : null;
            placeholders.put("%" + prefix + "shop_name%", shop != null ? shop.name() : "?");
            placeholders.put("%" + prefix + "shop_description%", shop != null ? shop.name() : "?");
            placeholders.put("%" + prefix + "shop_owners%", shop != null && shop.owners() != null ? String.join(", ", shop.owners()) : "-");
        };

        // First add the container itself
        applyContainerPlaceholders.accept(this.unsavedContainer, "");

        // Then attempt to add the copied one
        applyContainerPlaceholders.accept(this.plugin.data().getPendingCopy(this.player.getUniqueId()), "paste_");

        return placeholders;
    }

    /**
     * Add an enchant glint to a GUI item
     */
    private void enchant(ItemStack item) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;
        meta.addEnchant(Enchantment.BINDING_CURSE, 1, true);
        item.setItemMeta(meta);
    }

    /**
     * Apply a name and description item meta to an item
     * This will also parse the available placeholders
     */
    private ItemStack applyMeta(ItemStack item, String name, String description) {

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

        item.addItemFlags(ItemFlag.HIDE_ITEM_SPECIFICS, ItemFlag.HIDE_ENCHANTS, ItemFlag.HIDE_ATTRIBUTES);

        return item;
    }

    /**
     * Handle clicking in the GUI
     */
    public void onInventoryClick(InventoryClickEvent e) {

        // Handle shift clicking items in the GUI for speed
        ItemStack item = e.getCurrentItem();
        if (item != null && !item.getType().isAir() && e.isShiftClick()) {
            // First see if there is one that accepts
            // this specific material
            for (SlotHandler handler : this.slots.values()) {
                if (handler.shiftClick != ShiftClick.SPECIFIC) continue;
                if (handler.shiftClick.material == item.getType()) {
                    handler.action.accept(e);
                    return;
                }
            }

            // If not, find any that accept all other
            for (SlotHandler handler : this.slots.values()) {
                if (handler.shiftClick == ShiftClick.ANY) {
                    handler.action.accept(e);
                    return;
                }
            }
            return;
        }

        // Otherwise, ensure they are clicking in the GUI and not in their inventory
        if (e.getClickedInventory() != e.getInventory()) return;

        // It doesn't matter what happens after this; we want to cancel the event
        e.setCancelled(true);

        SlotHandler handler = this.slots.get(e.getSlot());
        if (handler != null && handler.action != null) {
            handler.action.accept(e);
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

    private enum ShiftClick {
        ANY(),
        SPECIFIC(),
        NONE();

        Material material;

        public ShiftClick setMaterial(Material material) {
            this.material = material;
            return this;
        }
    }

    private static final class SlotHandler {
        public final ItemStack item;
        public final Consumer<InventoryClickEvent> action;
        public ShiftClick shiftClick;

        public SlotHandler(ItemStack item, Consumer<InventoryClickEvent> action) {
            this.item = item;
            this.action = action;
        }

        public SlotHandler(ItemStack item, ShiftClick shiftClick, Consumer<InventoryClickEvent> action) {
            this.item = item;
            this.shiftClick = shiftClick;
            this.action = action;
        }
    }

}
