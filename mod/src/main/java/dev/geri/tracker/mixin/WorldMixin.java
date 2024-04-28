package dev.geri.tracker.mixin;

import dev.geri.tracker.Mod;
import dev.geri.tracker.utils.Api;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.BlockState;
import net.minecraft.block.ChestBlock;
import net.minecraft.block.enums.ChestType;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Environment(EnvType.CLIENT)
@Mixin(World.class)
public abstract class WorldMixin {

    @Inject(method = "onBlockChanged", at = @At("TAIL"))
    private void place(BlockPos pos, BlockState oldBlock, BlockState newBlock, CallbackInfo ci) {
        // We do not care about block state changes, just if the whole block was changed
        if (newBlock.getBlock().equals(oldBlock.getBlock())) return;

        World world = MinecraftClient.getInstance().world;
        if (world == null) return;

        if (newBlock.isAir()) this.handleBlockBreak(world, pos, oldBlock);
        else this.handleBlockPlace(world, pos, newBlock);
    }

    @Unique
    private void handleBlockPlace(World world, BlockPos pos, BlockState newBlock) {
        Mod mod = Mod.getInstance();

        // Check if it's a tile entity we care about
        if (!mod.doWeCare(newBlock)) return;

        // Ensure we are at spawn
        if (!mod.isAtSpawn(pos.getX(), pos.getY(), pos.getZ())) return;

        // Handle regular containers
        if (!(newBlock.getBlock() instanceof ChestBlock) || (newBlock.contains(ChestBlock.CHEST_TYPE) && newBlock.get(ChestBlock.CHEST_TYPE) == ChestType.SINGLE)) {
            mod.scanner().add(newBlock, pos);
            return;
        }

        // Handle double chests
        BlockPos otherPos = pos.offset(ChestBlock.getFacing(newBlock));
        switch (newBlock.get(ChestBlock.CHEST_TYPE)) {

            // If we just placed the right side, migrate the container
            // from the left side to the right one
            case RIGHT -> {
                Api.Container container = mod.api().getContainer(otherPos.getX(), otherPos.getY(), otherPos.getZ());

                // If there isn't a container, just track it
                if (container == null) {
                    mod.scanner().add(newBlock, pos);
                    return;
                }

                // Delete the original container
                mod.api().deleteContainer(container).thenRun(() -> {
                    mod.scanner().remove(otherPos);

                    // Migrate it to the new position
                    mod.api().saveContainer(container.setLocation(pos)).thenRun(() -> {
                        mod.scanner().add(newBlock, pos);
                    });
                });
            }

            // Simply track it and remove the other
            case LEFT -> {
                mod.scanner().remove(otherPos);
                mod.scanner().add(world.getBlockState(otherPos), otherPos);
            }
        }
    }

    @Unique
    private void handleBlockBreak(World world, BlockPos pos, BlockState oldBlock) {
        Mod mod = Mod.getInstance();

        // Check if it was a block we cared about
        if (!mod.doWeCare(oldBlock)) return;

        // Handle regular containers
        if (!(oldBlock.getBlock() instanceof ChestBlock) || !oldBlock.contains(ChestBlock.CHEST_TYPE) || oldBlock.get(ChestBlock.CHEST_TYPE) == ChestType.SINGLE) {
            // Check if it's a container and delete it
            Api.Container container = mod.api().getContainer(pos.getX(), pos.getY(), pos.getZ());
            if (container != null) {
                mod.api().deleteContainer(container).thenRun(() -> mod.scanner().remove(pos));
            } else {
                mod.scanner().remove(pos);
            }
            return;
        }

        // Handle double chests
        BlockPos otherPos = pos.offset(ChestBlock.getFacing(oldBlock));
        switch (oldBlock.get(ChestBlock.CHEST_TYPE)) {

            // If the right side is broken, migrate the container to the left side
            case RIGHT -> {
                Api.Container container = mod.api().getContainer(pos.getX(), pos.getY(), pos.getZ());

                // If there isn't a container, just untrack it
                if (container == null) {
                    mod.scanner().add(world.getBlockState(otherPos), otherPos);
                    return;
                }

                // Delete the original container
                mod.api().deleteContainer(container).thenRun(() -> {
                    // Migrate it to the new position
                    mod.api().saveContainer(container.setLocation(otherPos)).thenRun(() -> {
                        mod.scanner().add(world.getBlockState(otherPos), otherPos);
                    });
                });
            }

            // Simply untrack it and track the left one
            case LEFT -> {
                mod.scanner().add(world.getBlockState(otherPos), otherPos);
            }
        }
    }
}
