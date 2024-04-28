package dev.geri.tracker.mixin;

import dev.geri.tracker.Mod;
import dev.geri.tracker.utils.Api;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.BlockState;
import net.minecraft.block.ChestBlock;
import net.minecraft.block.entity.BlockEntity;
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

        if (newBlock.isAir()) this.handleBlockBreak(world, pos, oldBlock, newBlock);
        else this.handleBlockPlace(world, pos, oldBlock, newBlock);
    }

    @Unique
    private void handleBlockPlace(World world, BlockPos pos, BlockState oldBlock, BlockState newBlock) {
        Mod mod = Mod.getInstance();

        // Check if it's a tile entity we care about
        if (!mod.doWeCare(newBlock)) return;

        // Ensure we are at spawn
        if (!mod.isAtSpawn(pos.getX(), pos.getY(), pos.getZ())) return;

        // Start tracking it
        mod.scanner().add(newBlock, pos);
    }

    @Unique
    private void handleBlockBreak(World world, BlockPos pos, BlockState oldBlock, BlockState newBlock) {
        Mod mod = Mod.getInstance();

        // Check if it was a block we cared about
        if (!mod.doWeCare(oldBlock)) return;

        // Check if it's a saved container and delete it if so
        Api.Container container = mod.api().getContainer(pos.getX(), pos.getY(), pos.getZ());

        // For other containers, just delete them
        if (container != null) {
            // If the right side of a double chest is broken,
            // migrate the data to the left
            if (oldBlock.contains(ChestBlock.CHEST_TYPE) && oldBlock.get(ChestBlock.CHEST_TYPE) == ChestType.RIGHT) {
                mod.api().deleteContainer(container);
                mod.api().saveContainer(container.setLocation(pos.offset(ChestBlock.getFacing(oldBlock))));
                mod.scanner().remove(pos);
                return;
            }

            // Delete regular containers
            mod.api().deleteContainer(container);
        }

        // Remove them from the scanner
        mod.scanner().remove(pos);

        // If it's a double chest, readd the other side
        if (oldBlock.contains(ChestBlock.CHEST_TYPE) && oldBlock.get(ChestBlock.CHEST_TYPE) != ChestType.SINGLE) {
            BlockPos otherPos = pos.offset(ChestBlock.getFacing(oldBlock));
            mod.scanner().add(world.getBlockState(otherPos), otherPos);
        }
    }
}
