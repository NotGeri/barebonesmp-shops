package dev.geri.tracker.screens.components;

import dev.geri.tracker.Mod;
import io.wispforest.owo.ui.component.SmallCheckboxComponent;
import io.wispforest.owo.ui.core.Color;
import io.wispforest.owo.ui.core.CursorStyle;
import io.wispforest.owo.ui.core.OwoUIDrawContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public class ToggleableSmallCheckBox extends SmallCheckboxComponent {

    public static final Identifier TEXTURE = new Identifier(Mod.ID, "textures/gui/disabled_toggleable_small_checkbox.png");

    private boolean enabled = true;

    public ToggleableSmallCheckBox() {
        super(Text.empty());
    }

    @Override
    public void toggle() {
        if (!this.enabled) return;
        super.toggle();
    }

    public boolean enabled() {
        return enabled;
    }

    public ToggleableSmallCheckBox enabled(boolean enabled) {
        if (!enabled) this.cursorStyle(CursorStyle.NONE);
        this.enabled = enabled;
        return this;
    }

    @Override
    public void draw(OwoUIDrawContext context, int mouseX, int mouseY, float partialTicks, float delta) {
        if (this.enabled) {
            super.draw(context, mouseX, mouseY, partialTicks, delta);
            return;
        }

        context.drawTexture(TEXTURE, this.x, this.y, 13, 13, 0, 0, 13, 13, 32, 16);
        if (this.checked) {
            context.drawTexture(TEXTURE, this.x, this.y, 13, 13, 16, 0, 13, 13, 32, 16);
        }
    }

}
