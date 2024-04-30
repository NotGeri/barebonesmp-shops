package dev.geri.tracker.screens.edit;

import dev.geri.tracker.Mod;
import dev.geri.tracker.screens.CustomScreen;
import dev.geri.tracker.screens.components.ToggleableSmallCheckBox;
import dev.geri.tracker.utils.Api;
import io.wispforest.owo.ui.component.*;
import io.wispforest.owo.ui.container.Containers;
import io.wispforest.owo.ui.container.FlowLayout;
import io.wispforest.owo.ui.container.GridLayout;
import io.wispforest.owo.ui.core.Insets;
import io.wispforest.owo.ui.core.Sizing;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.CloseHandledScreenC2SPacket;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import org.joml.Vector3i;

import java.util.*;
import java.util.function.BiConsumer;

public class EditScreen extends CustomScreen {

    private final int syncId;
    private final BlockPos pos;
    private final List<Map.Entry<Item, Integer>> items;
    private Api.Container container;

    // Keep track of certain UI elements values
    private final ArrayList<ToggleableSmallCheckBox> itemCheckboxes = new ArrayList<>();
    private final HashMap<Api.Per, ToggleableSmallCheckBox> perCheckBoxes = new HashMap<>();
    private LabelComponent selectedShop;
    private ItemComponent customItem;
    private ButtonComponent save;

    public EditScreen(InventoryInteraction interaction) {
        super(FlowLayout.class, DataSource.asset(new Identifier(Mod.ID, "edit")));
        this.syncId = interaction.syncId();
        this.pos = interaction.pos();
        this.container = interaction.container();

        // Calculate how much of a specific item we have in the chest
        HashMap<Item, Integer> rawItems = new HashMap<>();
        for (ItemStack item : interaction.items()) {
            // Ignore the player's items
            if (this.mc.player != null && this.mc.player.getInventory().contains(item)) continue;

            // Ignore air
            if (item.isEmpty()) continue;

            // Sum up the items
            rawItems.put(item.getItem(), rawItems.getOrDefault(item.getItem(), 0) + item.getCount());
        }

        // Sort items
        this.items = rawItems.entrySet().stream().sorted(Map.Entry.comparingByValue(Comparator.reverseOrder())).toList();
    }

    @Override
    protected void build(FlowLayout rootComponent) {
        // Handle updating the title
        LabelComponent managing = rootComponent.childById(LabelComponent.class, "managing");
        managing.text(Text.translatable("text.tracker.managing", this.pos.getX(), this.pos.getY(), this.pos.getZ()));

        // Handle tracking and switching to the shop selector screen
        this.selectedShop = rootComponent.childById(LabelComponent.class, "selected-shop");
        ButtonComponent shopSelector = rootComponent.childById(ButtonComponent.class, "shop-selector");
        shopSelector.onPress(press -> {
            this.mc.setScreen(new ShopSelectorScreens(this, this.container));
        });

        // Handle building the item container
        GridLayout itemContainer = rootComponent.childById(GridLayout.class, "item-container");
        this.buildItemContainer(itemContainer);

        // Handle updating the summary
        LabelComponent summary = rootComponent.childById(LabelComponent.class, "summary");
        summary.text(Text.translatable("text.tracker.summary", this.pos.getX(), this.pos.getY(), this.pos.getZ()));

        // Handle the untrack button
        ButtonComponent untrack = rootComponent.childById(ButtonComponent.class, "untrack");
        untrack.onPress(press -> {
            this.container = new Api.Container();
            this.container.setUntracked(true);
            this.container.setLocation(new Vector3i(this.pos.getX(), this.pos.getY(), this.pos.getZ()));
            this.close();
            this.mod.api().saveContainer(this.container).thenRun(() -> {
                this.mod.scanner().refresh(this.pos);
            });
        });

        // Handle the price/amount checkboxes
        TextBoxComponent price = rootComponent.childById(TextBoxComponent.class, "price");
        TextBoxComponent amount = rootComponent.childById(TextBoxComponent.class, "amount");

        List.of(price, amount).forEach(numberTextBox -> {
            numberTextBox.onChanged().subscribe((change) -> {
                // Ensure all characters are digits
                String numericText = change.replaceAll("\\D", "");
                if (!numericText.equals(change)) numberTextBox.setText(numericText);

                // Recalculate the save button
                this.recalculateSaveButton();
            });
        });

        // Handle the per checkboxes
        FlowLayout perPieceWrapper = rootComponent.childById(FlowLayout.class, "per-piece");
        ToggleableSmallCheckBox perPiece = new ToggleableSmallCheckBox();
        perPieceWrapper.child(perPiece);

        FlowLayout perStackWrappe = rootComponent.childById(FlowLayout.class, "per-stack");
        ToggleableSmallCheckBox perStack = new ToggleableSmallCheckBox();
        perStackWrappe.child(perStack);

        FlowLayout perShulkerWrapper = rootComponent.childById(FlowLayout.class, "per-shulker");
        ToggleableSmallCheckBox perShulker = new ToggleableSmallCheckBox();
        perShulkerWrapper.child(perShulker);

        this.perCheckBoxes.put(Api.Per.PIECE, perPiece);
        this.perCheckBoxes.put(Api.Per.STACK, perStack);
        this.perCheckBoxes.put(Api.Per.SHULKER, perShulker);
        this.perCheckBoxes.forEach((type, checkbox) -> {
            checkbox.onChanged().subscribe(checked -> {
                this.recalculateSaveButton();
                if (!checked) return;

                // Ensure the last checkbox can't be unchecked
                checkbox.enabled(false);

                // Ensure other checkboxes are unchecked
                this.perCheckBoxes.values().forEach(c -> {
                    if (c != checkbox) {
                        c.checked(false);
                        c.enabled(true);
                    }
                });
            });
        });

        // Handle loading the initial settings
        this.selectShop(this.container != null ? this.container.shop() : null);
        if (this.container != null) {
            if (this.container.per() != null) this.perCheckBoxes.get(this.container.per()).checked(true);
            price.text(this.container.price() + "");
            amount.text(this.container.amount() + "");
        }

        // Set some defaults for missing values
        if (this.getSelectedPer() == null) this.perCheckBoxes.get(Api.Per.STACK).checked(true);
        if (price.getText().isEmpty()) price.text("0");
        if (amount.getText().isEmpty()) amount.text("0");

        // Handle the save button
        this.save = rootComponent.childById(ButtonComponent.class, "save");
        this.recalculateSaveButton();
        this.save.onPress(press -> {
            this.container.setUntracked(false);
            this.container.setLocation(new Vector3i(this.pos.getX(), this.pos.getY(), this.pos.getZ()));
            this.container.setPrice(Integer.parseInt(price.getText())); // Todo (notgeri):
            this.container.setAmount(Integer.parseInt(amount.getText())); // Todo (notgeri):
            this.container.setPer(this.getSelectedPer());
            this.close();

            Mod.getInstance().api().saveContainer(container).thenAccept(container -> {
                this.container = container;
                this.mod.scanner().refresh(this.pos);
            });
        });
    }

    /**
     * Get the selected per checkbox's value
     */
    private Api.Per getSelectedPer() {
        for (Map.Entry<Api.Per, ToggleableSmallCheckBox> entry : this.perCheckBoxes.entrySet()) {
            Api.Per type = entry.getKey();
            SmallCheckboxComponent checkbox = entry.getValue();
            if (checkbox.checked()) {
                return type;
            }
        }
        return null;
    }

    /**
     * Build the item container with the custom option and
     * all the available container items
     */
    private void buildItemContainer(GridLayout itemContainer) {

        // Make sure if one checkbox is checked, the rest are unchecked
        this.itemCheckboxes.clear();
        BiConsumer<SmallCheckboxComponent, Boolean> onCheckbox = (checkbox, checked) -> {
            if (!checked) return;
            this.itemCheckboxes.forEach(c -> {
                if (c != checkbox) {
                    c.checked(false);
                }
            });
        };

        int row = 0;

        // Custom item checkbox
        ToggleableSmallCheckBox customItemCheckbox = new ToggleableSmallCheckBox();
        this.itemCheckboxes.add(customItemCheckbox);
        customItemCheckbox.onChanged().subscribe(checked -> onCheckbox.accept(customItemCheckbox, checked));
        itemContainer.child(customItemCheckbox, row, 0);

        // Add the item preview
        this.customItem = Components.item(Items.APPLE.getDefaultStack());
        itemContainer.child(this.customItem, row, 1);

        FlowLayout textBoxes = Containers.horizontalFlow(Sizing.content(), Sizing.content());
        itemContainer.child(textBoxes.margins(Insets.left(5)), row, 2);

        // Custom item label
        TextBoxComponent rawCustomItem = Components.textBox(Sizing.fixed(45)).text("apple");
        textBoxes.child(rawCustomItem.sizing(Sizing.fixed(50), Sizing.fixed(15)));
        this.writableFields.add(rawCustomItem);

        // Custom item name
        TextBoxComponent customItemName = Components.textBox(Sizing.fixed(45)).text("Custom Name");
        textBoxes.child(customItemName.sizing(Sizing.fixed(70), Sizing.fixed(15)));

        Runnable parseCustomItem = () -> {
            // Remove the namespace
            String rawId = rawCustomItem.getText().toLowerCase();
            if (rawId.startsWith("minecraft:")) {
                rawId = rawId.substring("minecraft:".length());
            }

            // Strip all invalid characters
            String stripped = rawId.replaceAll("[^[a-z0-9]/-_.]", "");

            // Parse it as a registry ID
            Identifier id = new Identifier("minecraft:" + stripped);
            rawCustomItem.text(id.getPath());

            // Attempt to get the item
            Optional<Item> parse = Registries.ITEM.getOrEmpty(id);

            // If it's not valid, make sure it can't be saved
            if (parse.isEmpty()) { // Currently doing: continue hooking this up ()
                Text text = Text.translatable("text.tracker.invalid-item-id");
                this.customItem.stack(Mod.WARNING_ITEM.getDefaultStack()).tooltip(text);
                customItemCheckbox.enabled(false).checked(false).tooltip(text);
                this.save.active(false);
            } else {
                this.customItem.stack(parse.get().getDefaultStack()).tooltip(Text.empty());
                customItemCheckbox.enabled(true).checked(true).tooltip(Text.empty());
            }

            this.recalculateSaveButton();
        };

        // Recalculate if the focus is lost
        rawCustomItem.focusLost().subscribe(parseCustomItem::run);

        // Recalculate if enter is pressed
        rawCustomItem.keyPress().subscribe((keyCode, scanCode, modifiers) -> {
            if (keyCode == 335 || keyCode == 257) {
                parseCustomItem.run();
            }
            return true;
        });

        row++;

        // Add the rest of the items
        for (Map.Entry<Item, Integer> entry : this.items) {

            // Add the checkbox
            ToggleableSmallCheckBox checkbox = new ToggleableSmallCheckBox();
            this.itemCheckboxes.add(checkbox);
            checkbox.onChanged().subscribe(checked -> onCheckbox.accept(checkbox, checked));
            itemContainer.child(checkbox, row, 0);

            // Add the item preview
            itemContainer.child(Components.item(
                    entry.getKey().getDefaultStack()
            ), row, 1);

            // Add the label
            itemContainer.child(Components.label(
                    entry.getKey()
                            .getName()
                            .copy()
                            .formatted(Formatting.DARK_GRAY)
            ).margins(Insets.left(7)), row, 2);

            row++;
        }

    }

    /**
     * Recalculate whether the save button can be pressed
     */
    private void recalculateSaveButton() {
        if (this.save == null) return;

        boolean valid = true;
        if (this.container == null) {
            valid = false;
        } else {
            if (this.container.shop() == null) valid = false;
        }

        this.save.active(valid);
    }

    /**
     * Select a specific shop for this container
     * This will not save the choice yet
     */
    public void selectShop(Api.Shop shop) {
        if (shop != null) {
            if (this.container == null) this.container = new Api.Container();
            this.container.setShop(shop);
        }
        this.selectedShop.text(this.container != null && this.container.shop() != null ? Text.literal(this.container.shop().name()) : Text.translatable("text.tracker.no-shop-selected"));
        this.recalculateSaveButton();
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
