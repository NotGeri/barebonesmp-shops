package dev.geri.tracker.mixin;

import dev.geri.tracker.Mod;
import dev.geri.tracker.utils.Api;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Environment(EnvType.CLIENT)
@Mixin(World.class)
public abstract class WorldMixin {

    @Inject(method = "onBlockChanged", at = @At("TAIL"))
    private void place(BlockPos pos, BlockState oldBlock, BlockState newBlock, CallbackInfo ci) {
        Mod mod = Mod.getInstance();

        World world = MinecraftClient.getInstance().world;
        if (world == null) return;

        if (newBlock.isAir()) {
            // Check if it was a block we cared about
            if (!mod.doWeCare(oldBlock)) return;

            // Check if it's a saved container and delete it if so
            Api.Container container = mod.api().getContainer(pos.getX(), pos.getY(), pos.getZ());
            if (container != null) {
                mod.api().deleteContainer(container);
            }

            mod.scanner().remove(pos);
        } else {
            // Check if it's a tile entity we care about
            BlockEntity blockEntity = world.getBlockEntity(pos);
            if (blockEntity == null) return;
            if (!mod.doWeCare(blockEntity)) return;
            if (mod.isAtSpawn(pos.getX(), pos.getY(), pos.getZ())) {
                mod.scanner().add(blockEntity);
            }
        }
    }

}
