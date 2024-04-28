package dev.geri.tracker.utils;

import com.mojang.blaze3d.systems.RenderSystem;
import dev.geri.tracker.Mod;
import net.minecraft.block.BlockState;
import net.minecraft.block.ChestBlock;
import net.minecraft.block.entity.*;
import net.minecraft.block.enums.ChestType;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.BlockPos;
import org.joml.Vector3i;
import org.lwjgl.opengl.GL11;

import java.util.Arrays;
import java.util.List;

public class Scanner {

    private final Mod mod = Mod.getInstance();

    public RenderUtils.Group basicChests;
    public RenderUtils.Group trapChests;
    public RenderUtils.Group barrels;
    public RenderUtils.Group shulkerBoxes;

    public List<RenderUtils.Group> allGroups;

    public Scanner() {
        this.basicChests = new RenderUtils.Group();
        this.trapChests = new RenderUtils.Group();
        this.barrels = new RenderUtils.Group();
        this.shulkerBoxes = new RenderUtils.Group();
        this.allGroups = Arrays.asList(basicChests, trapChests, barrels, shulkerBoxes);
    }

    /**
     * Handle scanning for specific tile entities
     */
    public void fullScan() {

        // Clear all existing groups
        this.allGroups.forEach(RenderUtils.Group::clear);

        // Get all loaded tile entities within the render distance
        RenderUtils.getLoadedBlockEntities().forEach(this::add);
    }

    public void add(BlockEntity blockEntity) {
        BlockPos pos = blockEntity.getPos();

        // Double check to make sure the block is within spawn
        if (!this.mod.isAtSpawn(pos.getX(), pos.getY(), pos.getZ())) return;

        // See if it's already ignored on an API level
        if (this.mod.api().ignored().contains(new Vector3i(pos.getX(), pos.getY(), pos.getZ()))) return;

        // See if it's tracked
        Api.Container container = this.mod.api().getContainer(pos.getX(), pos.getY(), pos.getZ());
        Colour colour;
        if (container != null) {
            if (container.recentlyChecked()) colour = Colour.RECENTLY_CHECKED;
            else colour = Colour.CHECK_EXPIRED;
        } else {
            colour = Colour.UNKNOWN;
        }

        // Start tracking them
        if (blockEntity instanceof TrappedChestBlockEntity) this.trapChests.add(blockEntity, colour);
        else if (blockEntity instanceof ChestBlockEntity chester) {
            // Ignore the left side of double chests
            BlockState state = chester.getCachedState();
            if (!state.contains(ChestBlock.CHEST_TYPE)) return;
            ChestType chestType = state.get(ChestBlock.CHEST_TYPE);
            if (chestType == ChestType.LEFT) return;
            this.basicChests.add(blockEntity, colour);
        } else if (blockEntity instanceof ShulkerBoxBlockEntity) this.shulkerBoxes.add(blockEntity, colour);
        else if (blockEntity instanceof BarrelBlockEntity) this.barrels.add(blockEntity, colour);
    }

    public void remove(BlockPos pos) {
        this.allGroups.forEach(group -> {
            group.getEntries().removeIf(entry -> entry.pos().equals(pos));
        });
    }

    public void clear() {
        this.allGroups.forEach(RenderUtils.Group::clear);
        RenderUtils.Renderer.closeBuffers();
    }

    public void enable() {
        this.fullScan();
        RenderUtils.Renderer.prepareBuffers();
    }

    /**
     * Handle setting all the GL filters, running the custom render
     * and then resetting everything to how it was
     */
    public void onRender(MatrixStack matrixStack) {
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
