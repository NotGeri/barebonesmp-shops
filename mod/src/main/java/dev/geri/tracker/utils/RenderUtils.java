/*
 * This is pretty much copy-pasted from https://github.com/Wurst-Imperium/ChestESP, all credits to them
 * Copyright (c) 2023 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package dev.geri.tracker.utils;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.ShaderProgram;
import net.minecraft.client.gl.VertexBuffer;
import net.minecraft.client.render.*;
import net.minecraft.client.render.BufferBuilder.BuiltBuffer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.chunk.WorldChunk;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

public class RenderUtils {

    private static final MinecraftClient MC = MinecraftClient.getInstance();

    public static void applyRegionalRenderOffset(MatrixStack matrixStack) {
        Vec3d camPos = getCameraPos();
        BlockPos blockPos = getCameraBlockPos();

        int regionX = (blockPos.getX() >> 9) * 512;
        int regionZ = (blockPos.getZ() >> 9) * 512;

        matrixStack.translate(regionX - camPos.x, -camPos.y, regionZ - camPos.z);
    }

    public static Vec3d getCameraPos() {
        Camera camera = MC.getBlockEntityRenderDispatcher().camera;
        if (camera == null) return Vec3d.ZERO;

        return camera.getPos();
    }

    public static BlockPos getCameraBlockPos() {
        Camera camera = MC.getBlockEntityRenderDispatcher().camera;
        if (camera == null) return BlockPos.ORIGIN;

        return camera.getBlockPos();
    }

    public static void drawSolidBox(Box bb, VertexBuffer vertexBuffer) {
        Tessellator tessellator = RenderSystem.renderThreadTesselator();
        BufferBuilder bufferBuilder = tessellator.getBuffer();

        bufferBuilder.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION);
        drawSolidBox(bb, bufferBuilder);
        BuiltBuffer buffer = bufferBuilder.end();

        vertexBuffer.bind();
        vertexBuffer.upload(buffer);
        VertexBuffer.unbind();
    }

    public static void drawSolidBox(Box bb, BufferBuilder bufferBuilder) {
        bufferBuilder.vertex(bb.minX, bb.minY, bb.minZ).next();
        bufferBuilder.vertex(bb.maxX, bb.minY, bb.minZ).next();
        bufferBuilder.vertex(bb.maxX, bb.minY, bb.maxZ).next();
        bufferBuilder.vertex(bb.minX, bb.minY, bb.maxZ).next();

        bufferBuilder.vertex(bb.minX, bb.maxY, bb.minZ).next();
        bufferBuilder.vertex(bb.minX, bb.maxY, bb.maxZ).next();
        bufferBuilder.vertex(bb.maxX, bb.maxY, bb.maxZ).next();
        bufferBuilder.vertex(bb.maxX, bb.maxY, bb.minZ).next();

        bufferBuilder.vertex(bb.minX, bb.minY, bb.minZ).next();
        bufferBuilder.vertex(bb.minX, bb.maxY, bb.minZ).next();
        bufferBuilder.vertex(bb.maxX, bb.maxY, bb.minZ).next();
        bufferBuilder.vertex(bb.maxX, bb.minY, bb.minZ).next();

        bufferBuilder.vertex(bb.maxX, bb.minY, bb.minZ).next();
        bufferBuilder.vertex(bb.maxX, bb.maxY, bb.minZ).next();
        bufferBuilder.vertex(bb.maxX, bb.maxY, bb.maxZ).next();
        bufferBuilder.vertex(bb.maxX, bb.minY, bb.maxZ).next();

        bufferBuilder.vertex(bb.minX, bb.minY, bb.maxZ).next();
        bufferBuilder.vertex(bb.maxX, bb.minY, bb.maxZ).next();
        bufferBuilder.vertex(bb.maxX, bb.maxY, bb.maxZ).next();
        bufferBuilder.vertex(bb.minX, bb.maxY, bb.maxZ).next();

        bufferBuilder.vertex(bb.minX, bb.minY, bb.minZ).next();
        bufferBuilder.vertex(bb.minX, bb.minY, bb.maxZ).next();
        bufferBuilder.vertex(bb.minX, bb.maxY, bb.maxZ).next();
        bufferBuilder.vertex(bb.minX, bb.maxY, bb.minZ).next();
    }

    public static void drawOutlinedBox(Box bb, VertexBuffer vertexBuffer) {
        Tessellator tessellator = RenderSystem.renderThreadTesselator();
        BufferBuilder bufferBuilder = tessellator.getBuffer();

        bufferBuilder.begin(VertexFormat.DrawMode.DEBUG_LINES, VertexFormats.POSITION);
        drawOutlinedBox(bb, bufferBuilder);
        BuiltBuffer buffer = bufferBuilder.end();

        vertexBuffer.bind();
        vertexBuffer.upload(buffer);
        VertexBuffer.unbind();
    }

    public static void drawOutlinedBox(Box bb, BufferBuilder bufferBuilder) {
        bufferBuilder.vertex(bb.minX, bb.minY, bb.minZ).next();
        bufferBuilder.vertex(bb.maxX, bb.minY, bb.minZ).next();

        bufferBuilder.vertex(bb.maxX, bb.minY, bb.minZ).next();
        bufferBuilder.vertex(bb.maxX, bb.minY, bb.maxZ).next();

        bufferBuilder.vertex(bb.maxX, bb.minY, bb.maxZ).next();
        bufferBuilder.vertex(bb.minX, bb.minY, bb.maxZ).next();

        bufferBuilder.vertex(bb.minX, bb.minY, bb.maxZ).next();
        bufferBuilder.vertex(bb.minX, bb.minY, bb.minZ).next();

        bufferBuilder.vertex(bb.minX, bb.minY, bb.minZ).next();
        bufferBuilder.vertex(bb.minX, bb.maxY, bb.minZ).next();

        bufferBuilder.vertex(bb.maxX, bb.minY, bb.minZ).next();
        bufferBuilder.vertex(bb.maxX, bb.maxY, bb.minZ).next();

        bufferBuilder.vertex(bb.maxX, bb.minY, bb.maxZ).next();
        bufferBuilder.vertex(bb.maxX, bb.maxY, bb.maxZ).next();

        bufferBuilder.vertex(bb.minX, bb.minY, bb.maxZ).next();
        bufferBuilder.vertex(bb.minX, bb.maxY, bb.maxZ).next();

        bufferBuilder.vertex(bb.minX, bb.maxY, bb.minZ).next();
        bufferBuilder.vertex(bb.maxX, bb.maxY, bb.minZ).next();

        bufferBuilder.vertex(bb.maxX, bb.maxY, bb.minZ).next();
        bufferBuilder.vertex(bb.maxX, bb.maxY, bb.maxZ).next();

        bufferBuilder.vertex(bb.maxX, bb.maxY, bb.maxZ).next();
        bufferBuilder.vertex(bb.minX, bb.maxY, bb.maxZ).next();

        bufferBuilder.vertex(bb.minX, bb.maxY, bb.maxZ).next();
        bufferBuilder.vertex(bb.minX, bb.maxY, bb.minZ).next();
    }

    public static BlockState getState(BlockPos pos) {
        return MC.world.getBlockState(pos);
    }

    private static VoxelShape getOutlineShape(BlockPos pos) {
        return getState(pos).getOutlineShape(MC.world, pos);
    }

    public static Box getBoundingBox(BlockPos pos) {
        return getOutlineShape(pos).getBoundingBox().offset(pos);
    }

    public static boolean canBeClicked(BlockPos pos) {
        return getOutlineShape(pos) != VoxelShapes.empty();
    }

    public static Stream<BlockEntity> getLoadedBlockEntities() {
        return getLoadedChunks().flatMap(chunk -> chunk.getBlockEntities().values().stream());
    }

    public static Stream<WorldChunk> getLoadedChunks() {
        int radius = Math.max(2, MC.options.getClampedViewDistance()) + 3;
        int diameter = radius * 2 + 1;

        ChunkPos center = MC.player.getChunkPos();
        ChunkPos min = new ChunkPos(center.x - radius, center.z - radius);
        ChunkPos max = new ChunkPos(center.x + radius, center.z + radius);

        Stream<WorldChunk> stream = Stream.<ChunkPos>iterate(min, pos -> {

            int x = pos.x;
            int z = pos.z;

            x++;

            if (x > max.x) {
                x = min.x;
                z++;
            }

            if (z > max.z) throw new IllegalStateException("Stream limit didn't work.");

            return new ChunkPos(x, z);

        }).limit(diameter * diameter).filter(c -> MC.world.isChunkLoaded(c.x, c.z)).map(c -> MC.world.getChunk(c.x, c.z)).filter(Objects::nonNull);

        return stream;
    }

    public static final class Group {

        public static class Entry {
            public List<BlockPos> positions;
            public Box box;
            public Colour colour;
        }

        private final ArrayList<Entry> entries = new ArrayList<>();

        public Collection<Entry> getEntries() {
            return this.entries;
        }

        public void add(List<BlockPos> positions, Colour colour) {

            Entry entry = new Entry();
            entry.colour = colour;
            entry.positions = positions;

            // Go through each position
            for (BlockPos pos : positions) {

                // Make sure the position is valid
                if (!RenderUtils.canBeClicked(pos)) continue;

                // If there isn't a box already, just set it as this
                // Otherwise, we will get the union of the existing ones
                Box currentBox = RenderUtils.getBoundingBox(pos);
                if (entry.box != null) {
                    entry.box = entry.box.union(currentBox);
                } else {
                    entry.box = currentBox;
                }
            }

            // Start tracking the entry
            if (entry.box == null) return;
            this.entries.add(entry);
        }

        public void clear() {
            this.entries.clear();
        }

    }

    public static final class Renderer {

        private static VertexBuffer solidBox;
        private static VertexBuffer outlinedBox;

        private final MatrixStack matrixStack;
        private final int regionX;
        private final int regionZ;

        public Renderer(MatrixStack matrixStack) {
            this.matrixStack = matrixStack;

            BlockPos camPos = RenderUtils.getCameraBlockPos();
            regionX = (camPos.getX() >> 9) * 512;
            regionZ = (camPos.getZ() >> 9) * 512;
        }

        public void renderBoxes(Group group) {

            for (Group.Entry entry : group.getEntries()) {
                Box box = entry.box;
                if (box == null) return;
                float[] color = entry.colour.colour();
                if (color == null) continue;

                matrixStack.push();
                matrixStack.translate(box.minX - regionX, box.minY, box.minZ - regionZ);
                matrixStack.scale((float) (box.maxX - box.minX), (float) (box.maxY - box.minY), (float) (box.maxZ - box.minZ));

                Matrix4f viewMatrix = matrixStack.peek().getPositionMatrix();
                Matrix4f projMatrix = RenderSystem.getProjectionMatrix();
                ShaderProgram shader = RenderSystem.getShader();

                RenderSystem.setShaderColor(color[0], color[1], color[2], 0.25F);
                solidBox.bind();
                solidBox.draw(viewMatrix, projMatrix, shader);
                VertexBuffer.unbind();

                RenderSystem.setShaderColor(color[0], color[1], color[2], 0.5F);
                outlinedBox.bind();
                outlinedBox.draw(viewMatrix, projMatrix, shader);
                VertexBuffer.unbind();

                matrixStack.pop();
            }
        }

        public static void prepareBuffers() {
            closeBuffers();
            solidBox = new VertexBuffer(VertexBuffer.Usage.STATIC);
            outlinedBox = new VertexBuffer(VertexBuffer.Usage.STATIC);

            Box box = new Box(BlockPos.ORIGIN);
            RenderUtils.drawSolidBox(box, solidBox);
            RenderUtils.drawOutlinedBox(box, outlinedBox);
        }

        public static void closeBuffers() {
            if (solidBox != null) solidBox.close();
            solidBox = null;

            if (outlinedBox != null) outlinedBox.close();
            outlinedBox = null;
        }
    }

}
