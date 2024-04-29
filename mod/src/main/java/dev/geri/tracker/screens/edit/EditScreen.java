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
import java.util.function.Supplier;

public class EditScreen extends CustomScreen {

    private final int syncId;
    private final BlockPos pos;
    private final List<Map.Entry<Item, Integer>> items;
    private Api.Container container;

    private final ArrayList<SmallCheckboxComponent> itemCheckboxes = new ArrayList<>();
    private final HashMap<Api.Per, ToggleableSmallCheckBox> perCheckBoxes = new HashMap<>();

    private LabelComponent selectedShop;
    private ButtonComponent save;
    private ItemStack customItemValue;

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

        // Handle building the item container
        GridLayout itemContainer = rootComponent.childById(GridLayout.class, "item-container");
        this.buildItemContainer(itemContainer);

        // Handle populating the shop list Todo (notgeri):
        ButtonComponent shopSelector = rootComponent.childById(ButtonComponent.class, "shop-selector");
        shopSelector.onPress(press -> {
            this.mc.setScreen(new ShopSelectorScreens(this, this.container));
        });

        // FlowLayout shopList = rootComponent.childById(FlowLayout.class, "shop-container");
        this.selectedShop = rootComponent.childById(LabelComponent.class, "selected-shop");

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

                if (checked) {
                    // Ensure the last checkbox can't be unchecked
                    checkbox.enabled(false);

                    // Ensure other checkboxes are unchecked
                    this.perCheckBoxes.values().forEach(c -> {
                        if (c != checkbox) {
                            c.checked(false);
                            c.enabled(true);
                        }
                    });
                }
            });
        });

        TextBoxComponent price = rootComponent.childById(TextBoxComponent.class, "price");
        price.onChanged().subscribe((change) -> this.recalculateSaveButton());

        TextBoxComponent amount = rootComponent.childById(TextBoxComponent.class, "amount");
        amount.onChanged().subscribe((change) -> this.recalculateSaveButton());

        this.selectShop(this.container != null ? this.container.shop() : null);

        // Handle setting all the values if the container is pulled from the API successfully
        if (this.container != null) {
            if (this.container.per() != null) this.perCheckBoxes.get(this.container.per()).checked(true);
            price.text(this.container.price() + "");
            amount.text(this.container.amount() + "");
        }

        Supplier<Api.Per> getSelectedPer = () -> {
            for (Map.Entry<Api.Per, ToggleableSmallCheckBox> entry : this.perCheckBoxes.entrySet()) {
                Api.Per type = entry.getKey();
                SmallCheckboxComponent checkbox = entry.getValue();
                if (checkbox.checked()) {
                    return type;
                }
            }
            return null;
        };

        // Default to some values
        if (getSelectedPer.get() == null) this.perCheckBoxes.get(Api.Per.STACK).checked(true);
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
            this.container.setPer(getSelectedPer.get());
            this.close();

            Mod.getInstance().api().saveContainer(container).thenAccept(container -> {
                this.container = container;
                this.mod.scanner().refresh(this.pos);
            });
        });
    }

    private GridLayout buildItemContainer(GridLayout itemContainer) {

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
        SmallCheckboxComponent customItemCheckbox = Components.smallCheckbox(Text.empty());
        this.itemCheckboxes.add(customItemCheckbox);
        customItemCheckbox.onChanged().subscribe(checked -> onCheckbox.accept(customItemCheckbox, checked));
        itemContainer.child(customItemCheckbox, row, 0);

        // Add the item preview
        ItemComponent customItem = Components.item(Items.APPLE.getDefaultStack());
        itemContainer.child(customItem, row, 1);

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
            String rawId = rawCustomItem.getText();
            if (rawId.startsWith("minecraft:")) {
                rawId = rawId.substring("minecraft:".length());
                rawCustomItem.text(rawId);
            }

            // Todo (notgeri): filer non a-z0-9/.-_

            Identifier id = new Identifier("minecraft:" + rawId);
            Optional<Item> parse = Registries.ITEM.getOrEmpty(id);

            // If it's not valid, make sure it can't be saved
            if (parse.isEmpty()) {
                customItemCheckbox.checked(false);
                this.save.active(false);
                return;
            }

            this.customItemValue = parse.get().getDefaultStack();
            customItem.stack(this.customItemValue);
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
            SmallCheckboxComponent checkbox = Components.smallCheckbox(Text.empty());
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

        return itemContainer;
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

    @Override
    public void close() {
        if (this.mc.player == null) return;

        // Send the packet to tell the server we can close the original container
        this.mc.player.networkHandler.sendPacket(new CloseHandledScreenC2SPacket(this.syncId));

        // Actually close it
        super.close();
    }

    /**
     * Select a specific shop for this container
     * This will not save the choice yet
     *
     * @param shop
     */
    public void selectShop(Api.Shop shop) {
        if (shop != null) {
            if (this.container == null) this.container = new Api.Container();
            this.container.setShop(shop);
        }
        this.selectedShop.text(this.container != null && this.container.shop() != null ? Text.literal(this.container.shop().name()) : Text.translatable("text.tracker.no-shop-selected"));
        this.recalculateSaveButton();
    }

}
