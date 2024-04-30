package dev.geri.tracker;

import dev.geri.tracker.utils.Api;
import dev.geri.tracker.utils.Scanner;
import dev.geri.tracker.vendor.WaypointCreationEvent;
import dev.geri.tracker.vendor.XaerosMinimap;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.block.*;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.ChestBlockEntity;
import net.minecraft.block.enums.ChestType;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.network.ServerInfo;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroups;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3i;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import xaero.common.minimap.waypoints.Waypoint;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public final class Mod implements ModInitializer {

    public static final String ID = "tracker";
    public static final Logger LOGGER = LoggerFactory.getLogger(ID);
    public static final List<String> SERVERS = new ArrayList<>() {{
        this.add("geri.minecraft.best");
        this.add("play.barebonesmp.com");
    }};

    private static Mod instance;

    private final MinecraftClient mc = MinecraftClient.getInstance();
    private Scanner scanner;
    private Api api;

    private boolean isOnServer = false;
    private boolean enabled;

    private BlockPos latestBreak;
    private BlockPos latestInteraction;

    public static final Item WARNING_ITEM = new Item(new Item.Settings());

    /*this.waypointsManager.createTemporaryWaypoints(defaultWorld, 0, 100, 0, true, 1);*/

    @Override
    public void onInitialize() {
        if (instance != null) throw new RuntimeException("onInitialize() ran twice!");
        instance = this;

        // Load items
        Registry.register(Registries.ITEM, new Identifier(Mod.ID, "warning"), WARNING_ITEM);
        ItemGroupEvents.modifyEntriesEvent(ItemGroups.BUILDING_BLOCKS).register(content -> {
            content.add(WARNING_ITEM);
        });

        // Initialise the scanner
        this.scanner = new Scanner();

        // Register the hotkey
        KeyBinding toggleKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.tracker.edit-mode",
                InputUtil.UNKNOWN_KEY.getCode(),
                "category.tracker.main"
        ));

        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {

            // Ensure it only checks specific servers
            ServerInfo serverInfo = handler.getServerInfo();
            if (serverInfo == null || !SERVERS.contains(serverInfo.address.split(":")[0])) return;

            // Open the websocket connection
            try {
                this.api = new Api(new URI("ws://127.0.0.1:8000"));
                CompletableFuture.runAsync(() -> this.api.connect());
            } catch (Exception exception) {
                LOGGER.error("Couldn't connect to tracker", exception);
                this.mc.inGameHud.setOverlayMessage(Text.translatable("text.tracker.api-error"), false);
            }

            if (this.enabled) {
                this.enabled = false;
                this.scanner.disable();
            }

            this.isOnServer = true;
        });

        // Ensure the websocket closes
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            if (!this.isOnServer) return;
            this.api.close();
        });

        // Listen for the hotkey
        ClientTickEvents.END_CLIENT_TICK.register(c -> {
            while (toggleKey.wasPressed()) {

                for (WaypointCreationEvent.Colour colour : WaypointCreationEvent.Colour.values()) {
                    WaypointCreationEvent.EVENT.invoker().onCreateWaypoint(
                            MinecraftClient.getInstance().player.getWorld(),
                            MinecraftClient.getInstance().player.getBlockPos().offset(Direction.EAST, colour.xaerosMinimap),
                            "Test " + colour.xaerosMinimap,
                            "test " + colour.xaerosMinimap,
                            colour
                    );
                }

                // Todo (notgeri):


                if (!this.isOnServer) {
                    this.mc.inGameHud.setOverlayMessage(Text.translatable("text.tracker.unsupported-server"), false);
                    return;
                }

                this.enabled = !enabled;
                if (this.enabled) {
                    scanner.enable();
                    this.mc.inGameHud.setOverlayMessage(Text.translatable("text.tracker.edit-mode-enabled"), false);
                } else {
                    scanner.disable();
                    this.mc.inGameHud.setOverlayMessage(Text.translatable("text.tracker.edit-mode-disabled"), false);
                }
            }
        });

        // Keep track of the latest interaction event, so we can
        // look up the coordinates when we get a specific packet
        UseBlockCallback.EVENT.register((PlayerEntity player, World world, Hand hand, BlockHitResult hitResult) -> {
            this.latestInteraction = hitResult.getBlockPos();
            return ActionResult.PASS;
        });

        // Initialise any third party dependencies
        try {
            Class.forName("xaero.minimap.XaeroMinimapFabric");
            XaerosMinimap.init();
            LOGGER.info("Xaero's Minimap support enabled!");
        } catch (ClassNotFoundException ignored) {
            LOGGER.info("No Xaero's Minimap instance found!");
        }

    }

    /**
     * Set a screen for the main MC instance
     * using a render thread
     *
     * @param screen The screen or null to close it
     */
    public void setScreen(@Nullable Screen screen) {
        this.mc.execute(() -> this.mc.setScreen(screen));
    }

    public boolean doWeCare(BlockState state) {
        Block b = state.getBlock();
        if (b == null) return false;
        if (b instanceof ChestBlock) return true;
        if (b instanceof BarrelBlock) return true;
        if (b instanceof ShulkerBoxBlock) return true;
        return false;
    }

    /**
     * @return Whether the player is at spawn
     */
    public boolean isPlayerAtSpawn() {
        if (this.mc.player == null) return false;
        int playerX = this.mc.player.getBlockX();
        int playerY = this.mc.player.getBlockY();
        int playerZ = this.mc.player.getBlockZ();
        return this.isAtSpawn(playerX, playerY, playerZ);
    }

    /**
     * @return Whether a set of coordinates is at spawn
     */
    public boolean isAtSpawn(int x, int y, int z) {
        if (this.mc.player == null) return false;
        if (this.mc.player.clientWorld.getRegistryKey() != World.OVERWORLD) return false;

        Vector3i[] spawn = this.api.spawn();
        if (spawn.length < 2) return false;

        float x1 = Math.min(spawn[0].x(), spawn[1].x());
        float x2 = Math.max(spawn[0].x(), spawn[1].x());
        float y1 = Math.min(spawn[0].y(), spawn[1].y());
        float y2 = Math.max(spawn[0].y(), spawn[1].y());
        float z1 = Math.min(spawn[0].z(), spawn[1].z());
        float z2 = Math.max(spawn[0].z(), spawn[1].z());

        return (x >= x1 && x <= x2) &&
                (y >= y1 && y <= y2) &&
                (z >= z1 && z <= z2);
    }

    public Scanner scanner() {
        return this.scanner;
    }

    public Api api() {
        return this.api;
    }

    public boolean enabled() {
        return this.enabled;
    }

    /**
     * Get the latest client interaction position
     */
    public BlockPos latestInteraction() {
        return this.latestInteraction;
    }

    /**
     * get the latest client block break position
     */
    public BlockPos latestBreak() {
        return this.latestBreak;
    }

    public void latestBreak(BlockPos pos) {
        this.latestBreak = pos;
    }

    /**
     * @return The main Singleton instance of the mod
     */
    public static Mod getInstance() {
        return instance;
    }

    /**
     * Adjust a block position to account for double chests
     */
    public BlockPos adjustPositionForDoubleChests(BlockPos pos) {
        if (pos == null) return pos;
        if (this.mc.world == null) return pos;

        BlockEntity blockEntity = this.mc.world.getBlockEntity(pos);
        if (!(blockEntity instanceof ChestBlockEntity chester)) return pos;

        BlockState state = chester.getCachedState();
        if (!state.contains(ChestBlock.CHEST_TYPE)) return pos;

        ChestType chestType = state.get(ChestBlock.CHEST_TYPE);
        if (chestType == ChestType.LEFT) {
            pos = pos.offset(ChestBlock.getFacing(state));
        }

        return pos;
    }

}
