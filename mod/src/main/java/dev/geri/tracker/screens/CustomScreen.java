package dev.geri.tracker.screens;

import dev.geri.tracker.Mod;
import io.wispforest.owo.ui.base.BaseUIModelScreen;
import io.wispforest.owo.ui.container.FlowLayout;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.widget.TextFieldWidget;

import java.util.ArrayList;

/**
 * Wrapper to automatically handle:
 * 1. Automatically closing the GUI if inventory is pressed. Add text fields
 * to 'writableFields' to prevent them from escaping then typing
 */
public abstract class CustomScreen extends BaseUIModelScreen<FlowLayout> {

    protected final MinecraftClient mc = MinecraftClient.getInstance();
    protected final Mod mod = Mod.getInstance();

    private boolean init = false;
    private boolean writing = false;
    protected final ArrayList<TextFieldWidget> writableFields = new ArrayList<>();

    protected CustomScreen(Class<FlowLayout> rootComponentClass, DataSource source) {
        super(rootComponentClass, source);
    }

    @Override
    protected void init() {
        super.init();

        // Initialise the text fields
        if (!this.init) {
            this.init = true;
            this.writableFields.forEach(field -> {
                if (field == null) return;
                field.focusGained().subscribe((f) -> this.writing = true);
                field.focusLost().subscribe(() -> this.writing = false);
            });
        }
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        // Ensure the inventory button escapes it too
        if (this.mc.options.inventoryKey.matchesKey(keyCode, scanCode) && !this.writing) {
            this.close();
            return true;
        }

        // Ensure other key presses still get propagated
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    protected void build(FlowLayout rootComponent) {
    }

}
