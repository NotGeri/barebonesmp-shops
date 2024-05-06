package dev.geri.shops;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import dev.geri.shops.data.Container;
import dev.geri.shops.data.Data;
import dev.geri.shops.gui.GuiManager;
import dev.geri.shops.http.Server;
import dev.geri.shops.utils.Strings;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.*;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.TabExecutor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BlockStateMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

public final class Shops extends JavaPlugin implements Listener, TabExecutor {

    public static final int MINUTE = 20 * 60;
    public static final Gson GSON = new GsonBuilder().serializeNulls().create();
    public static MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();

    private FileConfiguration config;
    private Data data = null;
    private final Server server = new Server();
    private final GuiManager guiManager = new GuiManager(this);

    private final Path dataPath = Paths.get(this.getDataFolder().getAbsolutePath(), "data.json");

    @Override
    public void onEnable() {
        // Ensure the data directory exists
        this.getDataFolder().mkdirs();

        // Load the default config
        this.saveDefaultConfig();
        this.config = getConfig();

        // Read the existing data
        try {
            if (new File(dataPath.toAbsolutePath().toString()).exists()) {
                this.data = Shops.GSON.fromJson(new String(Files.readAllBytes(dataPath)), Data.class);
            }
            if (this.data == null) this.data = new Data();
            this.data.init();
        } catch (Exception exception) {
            this.getLogger().severe("Unable to read data file, disabling plugin!");
            throw new RuntimeException(exception);
        }

        // Register the events we have
        Bukkit.getPluginManager().registerEvents(this, this);
        Bukkit.getPluginManager().registerEvents(this.guiManager, this);

        // Register the commands
        PluginCommand command = this.getCommand("shops");
        if (command != null) command.setExecutor(this);

        // Save the data to disk if anything has changed
        this.getServer().getScheduler().runTaskTimer(this, () -> {
            if (!this.data.hasChanges) return;
            try {
                this.save();
            } catch (Exception exception) {
                this.data.hasChanges = false;
                this.getLogger().severe("Unable to save data: " + exception.getMessage());
            }
        }, 1, this.config.getLong("misc.save-frequency", 5) * MINUTE);

        // Initialise the HTTP server
        this.getServer().getScheduler().runTaskAsynchronously(this, () -> {
            try {
                this.server.start(this, this.config.getInt("api.port", 8000));
            } catch (IOException exception) {
                this.getLogger().severe("Unable to start HTTP API: " + exception.getMessage());
            }
        });
    }

    @Override
    public void onDisable() {
        // Stop the HTTP server
        this.server.stop();

        // Save the data
        this.getLogger().info("Saving data..");
        try {
            this.save();
            this.getLogger().info("Data saved!");
        } catch (IOException exception) {
            this.getLogger().severe("Unable to save data!");
            throw new RuntimeException(exception);
        }
    }

    /**
     * @return Data util
     */
    public Data data() {
        return data;
    }

    /**
     * Save the data to disk
     */
    public void save() throws IOException {
        Files.write(this.dataPath, List.of(Shops.GSON.toJson(this.data)), StandardCharsets.UTF_8);
    }

    /**
     * Get the location of a trackable block
     *
     * @param block The block to check
     * @return Its possibly adjusted location or null if not trackable
     */
    public Location getTrackableBlockLocation(Block block) {
        if (block == null) return null;

        BlockState state = block.getState();
        if (state instanceof ShulkerBox) return block.getLocation();
        if (state instanceof Barrel) return block.getLocation();
        if (state instanceof Chest chester) {
            // For double chests, always return the right side
            if (chester.getInventory().getHolder() instanceof DoubleChest doubleChester) {
                InventoryHolder rightHolder = doubleChester.getRightSide();

                // Yes, this is pretty much the only sane way to get the location for some reason
                if (rightHolder instanceof Chest right) {
                    return right.getLocation();
                }
                return chester.getLocation();
            }
            return block.getLocation();
        }
        return null;
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent e) {
        Player player = e.getPlayer();
        Block block = e.getClickedBlock();

        // Ensure they are using the right item
        if (!e.getAction().isRightClick() || player.getInventory().getItemInMainHand().getType() != Material.TRIPWIRE_HOOK) return;

        // Ensure it's a block we can track
        Location location = this.getTrackableBlockLocation(block);
        if (location == null) return;

        // Cancel the event so the original inventory doesn't open
        e.setCancelled(true);

        // Open our custom GUI
        Container container = this.data.getContainer(location);
        this.guiManager.openEditGui(player, location, container);
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent e) {
        // Get the location of the inventory
        Location eventLocation = e.getInventory().getLocation();
        if (eventLocation == null) return;

        // Get the original block and its adjusted location
        Block block = eventLocation.getBlock();
        Location location = this.getTrackableBlockLocation(block);
        if (location == null) return;

        // Check if it's a container
        Container container = this.data.getContainer(location);
        if (container == null) return;

        // If it's a custom item, we can't track it
        if (container.customName() != null && !container.customName().isEmpty()) return;

        this.recalculateStock(location, container, e.getInventory());
    }

    /**
     * Recalculate the stock of a container based on an inventory
     */
    public void recalculateStock(Location location, Container container, Inventory inventory) {
        int newStock = 0;
        for (ItemStack item : inventory) {
            if (item == null) continue;
            if (item.getType() == container.material()) {
                newStock += item.getAmount();
            }

            // Check if it's a shulker and repeat with the items inside
            if (item.getItemMeta() instanceof BlockStateMeta state) {
                if (state.getBlockState() instanceof ShulkerBox shulker) {
                    for (ItemStack i : shulker.getInventory()) {
                        if (i == null) continue;
                        if (i.getType() == container.material()) {
                            newStock += i.getAmount();
                        }
                    }
                }
            }
        }

        // Update the stock if necessary
        if (newStock != container.stock()) {
            container.setStock(newStock);
            this.data.saveContainer(location, container);
        }
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent e) {
        Block block = e.getBlock();

        // Ensure it's a block we can track
        Location location = this.getTrackableBlockLocation(block);
        if (location == null) return;

        // Check if it exits as a container
        Container container = this.data.getContainer(location);
        if (container == null) return;

        // Make sure the player knows this will untrack it
        Player player = e.getPlayer();
        if (!player.isSneaking()) {
            e.setCancelled(true);
            player.sendMessage(Shops.MINI_MESSAGE.deserialize(this.config.getString("messages.confirm-break", "")));
            return;
        }

        // Delete the container
        this.data.removeContainer(location);
        this.getLogger().info("%s deleted container %s %s %s".formatted(e.getPlayer().getName(), location.getBlockX(), location.getBlockY(), location.getBlockY()));
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (args.length == 0) {
            if (!sender.hasPermission("shops.help")) {
                sender.sendMessage(Shops.MINI_MESSAGE.deserialize(this.config.getString("messages.access-denied", "")));
                return true;
            }

            sender.sendMessage(Shops.MINI_MESSAGE.deserialize(Strings.placeholders(
                    this.config.getString("messages.help", ""),
                    Map.of(
                            "%shop_count%", this.data.shops().size(),
                            "%container_count%", this.data.containers().size()
                    ))
            ));
            return true;
        }

        switch (args[0]) {
            case "reload" -> {
                if (!sender.hasPermission("shops.reload")) {
                    sender.sendMessage(Shops.MINI_MESSAGE.deserialize(this.config.getString("messages.access-denied", "")));
                    return true;
                }

                this.saveDefaultConfig();
                this.reloadConfig();
                this.config = getConfig();
                sender.sendMessage(Shops.MINI_MESSAGE.deserialize(this.config.getString("messages.reloaded", "")));
            }

            default -> {
                sender.sendMessage(Shops.MINI_MESSAGE.deserialize(this.config.getString("messages.invalid-command", "")));
            }
        }

        return true;
    }

}
