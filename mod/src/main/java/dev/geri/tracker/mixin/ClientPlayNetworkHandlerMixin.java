package dev.geri.tracker.mixin;

import dev.geri.tracker.Mod;
import dev.geri.tracker.utils.CustomScreen;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientConnectionState;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.network.ClientConnection;
import net.minecraft.network.packet.s2c.play.InventoryS2CPacket;
import net.minecraft.network.packet.s2c.play.OpenScreenS2CPacket;
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
    private ArrayList<Integer> ids;

    @Inject(method = "<init>", at = @At(value = "TAIL"))
    private void init(MinecraftClient client, ClientConnection clientConnection, ClientConnectionState clientConnectionState, CallbackInfo ci) {
        this.ids = new ArrayList<>();
    }

    @Inject(method = "onInventory(Lnet/minecraft/network/packet/s2c/play/InventoryS2CPacket;)V", at = @At(value = "TAIL"))
    private void onInventory(InventoryS2CPacket packet, CallbackInfo ci) {
        System.out.println("onInventory " + packet.getSyncId()); // Todo (notgeri):

        // Ensure only verified IDs are checked
        if (!this.ids.contains(packet.getSyncId())) return;

        // Open our custom GUI on the main render thread
        MinecraftClient.getInstance().execute(() -> {
            CustomScreen screen = new CustomScreen(
                    packet.getSyncId(),
                    packet.getContents(),
                    Mod.getInstance().latestInteraction()
            );
            this.mc.setScreen(screen);
        });
    }

    @Inject(method = "onOpenScreen", at = @At(value = "HEAD"), cancellable = true)
    private void onOpenScreen(OpenScreenS2CPacket packet, CallbackInfo ci) {

        System.out.println("onOpenScreen " + packet.getSyncId()); // Todo (notgeri):

        // Ensure the mod is enabled
        Mod mod = Mod.getInstance();
        if (mod == null || !mod.enabled()) return;

        // Ensure it's using the required item
        if (this.mc.player == null || !this.mc.player.getMainHandStack().isOf(Mod.REQUIRED_ITEM)) return;

        // Cancel the screen opening
        ci.cancel();

        // Save the sync ID so we can make sure we only check
        // packets that were verified to be with our checks
        this.ids.add(packet.getSyncId());
    }

}
