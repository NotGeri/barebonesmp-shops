package dev.geri.tracker.screens.components;

import io.wispforest.owo.ui.component.SmallCheckboxComponent;
import net.minecraft.text.Text;

public class ToggleableSmallCheckBox extends SmallCheckboxComponent {

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
        this.enabled = enabled;
        return this;
    }

}
