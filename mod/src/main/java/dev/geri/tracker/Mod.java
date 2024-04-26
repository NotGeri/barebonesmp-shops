package dev.geri.tracker;

import com.mojang.blaze3d.systems.RenderSystem;
import dev.geri.tracker.utils.Api;
import dev.geri.tracker.utils.CustomScreen;
import dev.geri.tracker.utils.RenderUtils;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.block.BlockState;
import net.minecraft.block.ChestBlock;
import net.minecraft.block.entity.*;
import net.minecraft.block.enums.ChestType;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ServerInfo;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.util.InputUtil;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.joml.Vector3f;
import org.joml.Vector3i;
import org.lwjgl.opengl.GL11;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public final class Mod implements ModInitializer {

    public static final Item REQUIRED_ITEM = Items.GOLDEN_HOE;
    public static final List<String> SERVERS = new ArrayList<>() {{
        this.add("geri.minecraft.best");
        this.add("play.barebonesmp.com");
    }};

    private static Mod instance;
    private final MinecraftClient mc = MinecraftClient.getInstance();

    public RenderUtils.Group basicChests;
    public RenderUtils.Group trapChests;
    public RenderUtils.Group barrels;
    public RenderUtils.Group shulkerBoxes;
    public List<RenderUtils.Group> allGroups;

    private Api api = new Api();
    private boolean isOnServer = false;
    private boolean enabled;
    private BlockPos latestInteraction;

    @Override
    public void onInitialize() {
        if (instance != null) throw new RuntimeException("onInitialize() ran twice!");
        instance = this;

        // Register our desired groups
        this.basicChests = new RenderUtils.Group();
        this.trapChests = new RenderUtils.Group();
        this.barrels = new RenderUtils.Group();
        this.shulkerBoxes = new RenderUtils.Group();
        this.allGroups = Arrays.asList(basicChests, trapChests, barrels, shulkerBoxes);

        // Register the hotkey
        KeyBinding toggleKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "toggle",
                InputUtil.UNKNOWN_KEY.getCode(),
                "test"
        ));

        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {

            // Ensure it only checks specific servers
            ServerInfo serverInfo = handler.getServerInfo();
            if (serverInfo == null || !SERVERS.contains(serverInfo.address)) return;

            // Get the data
            try {
                this.api.init();
            } catch (IOException exception) {
                this.mc.inGameHud.setOverlayMessage(Text.literal("§cUnable to fetch tracker API!"), false);
                return;
            }

            this.isOnServer = true;
        });

        // Listen for the hotkey
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (toggleKey.wasPressed()) {
                if (!this.isOnServer) {
                    this.mc.inGameHud.setOverlayMessage(Text.literal("§cYou are not on a tracked server!"), false);
                    return;
                }

                if (!this.isPlayerAtSpawn()) {
                    this.mc.inGameHud.setOverlayMessage(Text.literal("§cYou are not at spawn!"), false);
                    return;
                }

                this.enabled = !enabled;
                if (this.enabled) {
                    this.scanForContainers();
                    RenderUtils.Renderer.prepareBuffers();
                } else {
                    this.allGroups.forEach(RenderUtils.Group::clear);
                    RenderUtils.Renderer.closeBuffers();
                }
            }
        });

        // Keep track of the latest interaction event, so we can
        // look up the coordinates when we get a specific packet
        UseBlockCallback.EVENT.register((PlayerEntity player, World world, Hand hand, BlockHitResult hitResult) -> {

            BlockPos blockPos = hitResult.getBlockPos();
            BlockEntity blockEntity = world.getBlockEntity(blockPos);
            if (blockEntity instanceof ChestBlockEntity || blockEntity instanceof ShulkerBoxBlockEntity) {  // Todo (notgeri): expand this to all after refactor
                this.latestInteraction = blockPos;
            }
            return ActionResult.PASS;
        });
    }

    /**
     * Handle scanning for specific tile entities
     */
    public void scanForContainers() {
        if (!this.enabled || !isPlayerAtSpawn()) return;

        // Clear all existing groups
        this.allGroups.forEach(RenderUtils.Group::clear);

        // Get all loaded tile entities within the render distance
        RenderUtils.getLoadedBlockEntities().forEach(blockEntity -> {
            BlockPos pos = blockEntity.getPos();

            // Double check to make sure the block is within spawn
            if (!isAtSpawn(pos.getX(), pos.getY(), pos.getZ())) return;

            // See if it's already ignored
            if (this.api.ignored().contains(new Vector3f(pos.getX(), pos.getY(), pos.getZ()))) return;

            // Start tracking them
            if (blockEntity instanceof TrappedChestBlockEntity) this.trapChests.add(blockEntity);
            else if (blockEntity instanceof ChestBlockEntity chester) {
                // Ignore the left side of double chests
                BlockState state = chester.getCachedState(); // Todo (notgeri): de-duplicate this
                if (!state.contains(ChestBlock.CHEST_TYPE)) return;
                ChestType chestType = state.get(ChestBlock.CHEST_TYPE);
                if (chestType == ChestType.LEFT) return;
                this.basicChests.add(blockEntity);
            } else if (blockEntity instanceof ShulkerBoxBlockEntity) this.shulkerBoxes.add(blockEntity);
            else if (blockEntity instanceof BarrelBlockEntity) this.barrels.add(blockEntity);
        });
    }

    /**
     * Handle setting all the GL filters, running the custom render
     * and then resetting everything to how it was
     */
    public void onRender(MatrixStack matrixStack) {
        if (!enabled || !isPlayerAtSpawn()) return;

        // GL sets
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glEnable(GL11.GL_CULL_FACE);
        GL11.glDisable(GL11.GL_DEPTH_TEST);
        GL11.glEnable(GL11.GL_LINE_SMOOTH);

        matrixStack.push();
        RenderUtils.applyRegionalRenderOffset(matrixStack);

        RenderUtils.Renderer renderer = new RenderUtils.Renderer(matrixStack);
        RenderSystem.setShader(GameRenderer::getPositionProgram);
        this.allGroups.forEach(group -> {
            renderer.renderBoxes(group, (box) -> {
                return new float[]{255, 0, 0};
            });
        });

        matrixStack.pop();

        // GL resets
        RenderSystem.setShaderColor(1, 1, 1, 1);
        GL11.glEnable(GL11.GL_DEPTH_TEST);
        GL11.glDisable(GL11.GL_BLEND);
        GL11.glDisable(GL11.GL_LINE_SMOOTH);
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
}
