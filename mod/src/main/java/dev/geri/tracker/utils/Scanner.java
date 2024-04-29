package dev.geri.tracker.utils;

import com.mojang.blaze3d.systems.RenderSystem;
import dev.geri.tracker.Mod;
import net.minecraft.block.BarrelBlock;
import net.minecraft.block.BlockState;
import net.minecraft.block.ChestBlock;
import net.minecraft.block.ShulkerBoxBlock;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.enums.ChestType;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.BlockPos;
import org.lwjgl.opengl.GL11;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Scanner {

    private final MinecraftClient mc = MinecraftClient.getInstance();
    private final Mod mod = Mod.getInstance();

    private boolean enabled;
    public RenderUtils.Group chests;
    public RenderUtils.Group barrels;
    public RenderUtils.Group shulkerBoxes;

    public List<RenderUtils.Group> allGroups;

    public Scanner() {
        this.chests = new RenderUtils.Group();
        this.barrels = new RenderUtils.Group();
        this.shulkerBoxes = new RenderUtils.Group();
        this.allGroups = Arrays.asList(chests, barrels, shulkerBoxes);
    }

    /**
     * Handle scanning for specific tile entities
     */
    public void fullScan() {
        if (!this.enabled) return;

        // Clear all existing groups
        this.allGroups.forEach(RenderUtils.Group::clear);

        // Get all loaded tile entities within the render distance
        RenderUtils.getLoadedBlockEntities().forEach((blockEntity) -> {
            this.add(blockEntity.getCachedState(), blockEntity.getPos());
        });
    }

    /**
     * Start tracking a position
     * @param state The state of the block
     * @param pos The position
     */
    public void add(BlockState state, BlockPos pos) {

        // Double check to make sure the block is within spawn
        if (!this.mod.isAtSpawn(pos.getX(), pos.getY(), pos.getZ())) return;

        // See if it's tracked
        Api.Container container = this.mod.api().getContainer(pos.getX(), pos.getY(), pos.getZ());
        Colour colour;
        if (container != null) {
            if (container.untracked()) return;
            if (container.recentlyChecked()) colour = Colour.RECENTLY_CHECKED;
            else colour = Colour.CHECK_EXPIRED;
        } else {
            colour = Colour.UNKNOWN;
        }

        // Start tracking them
        if (state.getBlock() instanceof ChestBlock) {

            // Collect the positions of the chest if it's a double
            List<BlockPos> positions = new ArrayList<>(List.of(pos));
            if (state.contains(ChestBlock.CHEST_TYPE)) {
                ChestType chestType = state.get(ChestBlock.CHEST_TYPE);
                if (chestType != ChestType.SINGLE) {
                    positions.add(pos.offset(ChestBlock.getFacing(state)));
                }
            }

            // Make sure to remove any existing ones
            for (BlockPos p : positions) this.remove(p);

            // Add the new one
            this.chests.add(positions, colour);

        } else if (state.getBlock() instanceof ShulkerBoxBlock) this.shulkerBoxes.add(List.of(pos), colour);
        else if (state.getBlock() instanceof BarrelBlock) this.barrels.add(List.of(pos), colour);
    }

    /**
     * Refresh a tracked position
     */
    public void refresh(BlockPos pos) {
        if (this.mc.world == null) return;
        BlockEntity be = this.mc.world.getBlockEntity(pos);
        this.remove(pos);
        if (be != null) this.add(be.getCachedState(), pos);
    }

    /**
     * Remove a tracked position
     */
    public void remove(BlockPos pos) {
        this.allGroups.forEach(group -> {
            group.getEntries().removeIf(entry -> entry.positions.stream().anyMatch(p -> p.equals(pos)));
        });
    }

    /**
     * Enable scanning and run a full scan
     */
    public void enable() {
        this.enabled = true;
        this.fullScan();
        RenderUtils.Renderer.prepareBuffers();
    }

    /**
     * Disable and hide scanning
     */
    public void disable() {
        this.enabled = false;
        this.allGroups.forEach(RenderUtils.Group::clear);
        RenderUtils.Renderer.closeBuffers();
    }

    /**
     * Handle setting all the GL filters, running the custom render
     * and then resetting everything to how it was
     */
    public void onRender(MatrixStack matrixStack) {
        if (!this.enabled) return;

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
        this.allGroups.forEach(renderer::renderBoxes);

        matrixStack.pop();

        // GL resets
        RenderSystem.setShaderColor(1, 1, 1, 1);
        GL11.glEnable(GL11.GL_DEPTH_TEST);
        GL11.glDisable(GL11.GL_BLEND);
        GL11.glDisable(GL11.GL_LINE_SMOOTH);
    }

}
