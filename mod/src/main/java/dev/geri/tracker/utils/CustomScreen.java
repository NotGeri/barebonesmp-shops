package dev.geri.tracker.utils;

import dev.geri.tracker.Mod;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.MultilineText;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.c2s.play.CloseHandledScreenC2SPacket;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

@Environment(EnvType.CLIENT)
public class CustomScreen extends Screen {

    private final MinecraftClient mc = MinecraftClient.getInstance();

    private final int syncId;
    private final List<ItemStack> items;
    private final BlockPos pos;

    /**
     * Create a new screen
     * @param syncId The sync ID of the original container open so we can safely close it
     * @param pos The position of the container
     */
    public CustomScreen(int syncId, List<ItemStack> items, BlockPos pos) {
        super(Text.literal("Test screen"));
        this.syncId = syncId;
        this.items = items;
        this.pos = pos;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        // Ensure E escapes too
        if (keyCode == 69) {
            this.close();
            return true;
        }

        // Ensure other key presses still get propagated
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    protected void init() {
        addDrawableChild(ButtonWidget.builder(Text.literal("Ignore " + pos), button -> {
                    Mod.getInstance().getData().ignored().add(new Vector3f(pos.getX(), pos.getY(), pos.getZ()));
                })
                .dimensions(width / 2 - 405, 20, 200, 20)
                .build());

        addDrawableChild(ButtonWidget.builder(Text.literal("Rescan"), button -> Mod.getInstance().scanForContainers())
                .dimensions(width / 2 - 205, 20, 200, 20)
                .build());
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);

        // Todo (notgeri):
        final MultilineText multilineText = MultilineText.create(textRenderer, Text.literal(this.items.toString()), width - 20);
        multilineText.drawWithShadow(context, 10, height / 2, 16, 0xffffff);
    }

    @Override
    public void close() {
        if (this.mc.player == null) return;

        // Send the packet to tell the server we can close the original container
        this.mc.player.networkHandler.sendPacket(new CloseHandledScreenC2SPacket(this.syncId));

        // Actually close it
        super.close();
    }
}
