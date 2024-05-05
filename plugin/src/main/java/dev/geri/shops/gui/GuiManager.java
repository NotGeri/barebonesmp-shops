package dev.geri.shops.gui;

import dev.geri.shops.Shops;
import dev.geri.shops.data.Container;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;

import java.util.HashMap;

public class GuiManager implements Listener {

    private final HashMap<Inventory, EditGui> activeGuis = new HashMap<>();
    private final Shops plugin;

    public GuiManager(Shops plugin) {
        this.plugin = plugin;
    }

    /**
     * Open the custom edit GUI to a player
     */
    public void openEditGui(Player player, Location location, Container container) {

        // Initialise the GUI
        EditGui gui = new EditGui(this.plugin, player, location, container);

        // Cache the inventory
        this.activeGuis.put(gui.inventory(), gui);

        // Open it for the player
        player.openInventory(gui.inventory());
    }

    /**
     * Handle forwarding the click events to the custom GUI
     */
    @EventHandler
    public void onInventoryClick(InventoryClickEvent e) {
        Inventory inventory = e.getInventory();

        // Ensure it's a custom GUI
        EditGui gui = this.activeGuis.get(inventory);
        if (gui == null) return;

        // Forward event
        gui.onInventoryClick(e);
    }

    /**
     * Clean up closed inventories
     */
    @EventHandler
    public void onInventoryClose(InventoryCloseEvent e) {
        Inventory inventory = e.getInventory();
        this.activeGuis.remove(inventory);
    }

}
