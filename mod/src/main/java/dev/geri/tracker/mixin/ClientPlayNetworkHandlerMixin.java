package dev.geri.tracker.mixin;

import dev.geri.tracker.Mod;
import dev.geri.tracker.screens.edit.EditScreen;
import dev.geri.tracker.screens.edit.InventoryInteraction;
import dev.geri.tracker.screens.edit.UntrackedScreen;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.BlockState;
import net.minecraft.block.ChestBlock;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.ChestBlockEntity;
import net.minecraft.block.enums.ChestType;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.network.ClientConnectionState;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.network.ClientConnection;
import net.minecraft.network.packet.s2c.play.InventoryS2CPacket;
import net.minecraft.network.packet.s2c.play.OpenScreenS2CPacket;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;

@Environment(EnvType.CLIENT)
@Mixin(ClientPlayNetworkHandler.class)
public abstract class ClientPlayNetworkHandlerMixin {

    @Unique
    private final MinecraftClient mc = MinecraftClient.getInstance();

    @Unique
    private ArrayList<Integer> ids; // Todo (notgeri): make this expire

    @Inject(method = "<init>", at = @At(value = "TAIL"))
    private void init(MinecraftClient client, ClientConnection clientConnection, ClientConnectionState clientConnectionState, CallbackInfo ci) {
        this.ids = new ArrayList<>();
    }

    @Inject(method = "onInventory(Lnet/minecraft/network/packet/s2c/play/InventoryS2CPacket;)V", at = @At(value = "TAIL"))
    private void onInventory(InventoryS2CPacket packet, CallbackInfo ci) {

        Mod.LOGGER.info("onInventory {}", packet.getSyncId()); // Todo (notgeri):

        // Ensure only verified IDs are checked
        if (!this.ids.contains(packet.getSyncId())) return;

        // Open our custom GUI on the main render thread
        MinecraftClient.getInstance().execute(() -> {

            // If it's a double chest, adjust the position to always be the right side
            BlockPos pos = Mod.getInstance().adjustPositionForDoubleChests(Mod.getInstance().latestInteraction());

            InventoryInteraction interaction = new InventoryInteraction(
                    packet.getSyncId(),
                    packet.getContents(),
                    pos,
                    Mod.getInstance().api().getContainer(pos.getX(), pos.getY(), pos.getZ())
            );

            Screen screen;
            if (interaction.container() != null && interaction.container().untracked()) {
                screen = new UntrackedScreen(interaction);
            } else {
                screen = new EditScreen(interaction);
            }

            this.mc.setScreen(screen);
        });
    }

    @Inject(method = "onOpenScreen", at = @At(value = "HEAD"), cancellable = true)
    private void onOpenScreen(OpenScreenS2CPacket packet, CallbackInfo ci) {

        Mod.LOGGER.info("onOpenScreen {}", packet.getSyncId()); // Todo (notgeri):

        // Ensure the mod is enabled
        Mod mod = Mod.getInstance();
        if (mod == null || !mod.enabled()) return;

        // Ensure it's using the required item
        if (this.mc.player == null || !this.mc.player.isSneaking()) return;

        // Cancel the screen opening
        ci.cancel();

        // Save the sync ID so we can make sure we only check
        // packets that were verified to be with our checks
        this.ids.add(packet.getSyncId());
    }

}
