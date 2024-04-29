package dev.geri.tracker.screens.edit;

import dev.geri.tracker.Mod;
import dev.geri.tracker.screens.CustomScreen;
import dev.geri.tracker.utils.Api;
import io.wispforest.owo.ui.component.ButtonComponent;
import io.wispforest.owo.ui.container.FlowLayout;
import net.minecraft.client.MinecraftClient;
import net.minecraft.network.packet.c2s.play.CloseHandledScreenC2SPacket;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

public class UntrackedScreen extends CustomScreen {

    private final InventoryInteraction interaction;

    public UntrackedScreen(InventoryInteraction interaction) {
        super(FlowLayout.class, DataSource.asset(new Identifier(Mod.ID, "untracked")));
        this.interaction = interaction;
    }

    @Override
    protected void build(FlowLayout rootComponent) {
        ButtonComponent remove = rootComponent.childById(ButtonComponent.class, "track");
        remove.onPress(press -> {
            // In order to start tracking it again, just delete the empty placeholder
            // The user will be prompted with the regular GUI so they can set it up
            BlockPos pos = this.interaction.pos();
            if (interaction.container() != null) {
                this.mod.api().deleteContainer(interaction.container()).thenRun(() -> {
                    this.mod.setScreen(new EditScreen(this.interaction));
                    this.mod.scanner().refresh(pos);
                });
            } else {
                this.close();
            }
        });
    }

    @Override
    public void close() {
        if (this.mc.player == null) return;

        // Send the packet to tell the server we can close the original container
        this.mc.player.networkHandler.sendPacket(new CloseHandledScreenC2SPacket(this.interaction.syncId()));

        // Actually close it
        super.close();
    }

}
