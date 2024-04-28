package dev.geri.tracker.screens.edit;

import dev.geri.tracker.Mod;
import dev.geri.tracker.screens.CustomScreen;
import dev.geri.tracker.screens.components.ToggleableSmallCheckBox;
import dev.geri.tracker.utils.Api;
import io.wispforest.owo.ui.component.ButtonComponent;
import io.wispforest.owo.ui.component.Components;
import io.wispforest.owo.ui.container.Containers;
import io.wispforest.owo.ui.container.FlowLayout;
import io.wispforest.owo.ui.core.Sizing;
import io.wispforest.owo.ui.core.VerticalAlignment;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.List;

public class ShopSelectorScreens extends CustomScreen {

    private final EditScreen parent;
    private Api.Container container;
    private Api.Shop selectedShop;

    public ShopSelectorScreens(EditScreen parent, Api.Container container) {
        super(FlowLayout.class, DataSource.asset(new Identifier(Mod.ID, "shop_selector")));
        this.parent = parent;
        this.container = container;
        if (container != null) this.selectedShop = container.shop();
    }

    @Override
    protected void build(FlowLayout rootComponent) {
        FlowLayout shopsContainer = rootComponent.childById(FlowLayout.class, "shops");

        List<Api.Shop> shops = this.mod.api().shops();
        if (!shops.isEmpty()) {

            ArrayList<ToggleableSmallCheckBox> checkboxes = new ArrayList<>();
            for (Api.Shop shop : shops) {
                FlowLayout shopContainer = Containers.horizontalFlow(Sizing.content(), Sizing.content());

                // Add the checkbox
                ToggleableSmallCheckBox checkbox = new ToggleableSmallCheckBox();
                if (this.selectedShop == null) this.selectedShop = shop;
                if (this.selectedShop == shop) checkbox.checked(true);

                checkboxes.add(checkbox);
                shopContainer.child(checkbox);
                checkbox.onChanged().subscribe(checked -> {
                    if (checked) {
                        this.selectedShop = shop;

                        // Make sure the current checkbox can't be disabled
                        checkbox.enabled(false);

                        // Make sure if one checkbox is ticked, the others are unticked
                        checkboxes.forEach(c -> {
                            if (c != checkbox) {
                                c.checked(false);
                                c.enabled(true);
                            }
                        });
                    }
                });

                // Add the label
                shopContainer.child(Components.label(Text.literal(shop.name()).formatted(Formatting.DARK_GRAY)));

                // Add to the container
                shopsContainer.child(shopContainer.gap(1).verticalAlignment(VerticalAlignment.CENTER));
            }
        } else {
            shopsContainer.child(Components.label(Text.literal("No shops available")));
        }

        // Handle the select button
        ButtonComponent select = rootComponent.childById(ButtonComponent.class, "select");
        select.onPress(press -> {
            if (this.parent != null && this.selectedShop != null) this.parent.selectShop(this.selectedShop);
            this.close();
        });
    }

    /**
     * Return to the parent if one was passed
     */
    @Override
    public void close() {
        if (this.parent != null) {
            this.mc.setScreen(parent);
            return;
        }
        super.close();
    }
}
