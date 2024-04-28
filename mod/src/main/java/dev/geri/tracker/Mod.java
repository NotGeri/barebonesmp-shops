package dev.geri.tracker;

import dev.geri.tracker.utils.Api;
import dev.geri.tracker.utils.Scanner;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.block.*;
import net.minecraft.block.entity.BarrelBlockEntity;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.ChestBlockEntity;
import net.minecraft.block.entity.ShulkerBoxBlockEntity;
import net.minecraft.block.enums.ChestType;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ServerInfo;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.joml.Vector3i;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

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
    private BlockPos latestInteraction;

    @Override
    public void onInitialize() {
        if (instance != null) throw new RuntimeException("onInitialize() ran twice!");
        instance = this;

        this.scanner = new Scanner();
        this.api = new Api();

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

            // Get the data
            try {
                this.api.init();
            } catch (IOException exception) {
                this.mc.inGameHud.setOverlayMessage(Text.translatable("text.tracker.api-error"), false);
                return;
            }

            this.isOnServer = true;
        });

        // Listen for the hotkey
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (toggleKey.wasPressed()) {
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
    }

    public boolean doWeCare(BlockEntity be) {
        if (be == null) return false;
        if (be instanceof ChestBlockEntity) return true;
        if (be instanceof BarrelBlockEntity) return true;
        if (be instanceof ShulkerBoxBlockEntity) return true;
        return false;
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

    public BlockPos latestInteraction() {
        return this.latestInteraction;
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
