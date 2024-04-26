package dev.geri.tracker.utils;

import dev.geri.tracker.Mod;
import io.wispforest.owo.ui.base.BaseUIModelScreen;
import io.wispforest.owo.ui.component.*;
import io.wispforest.owo.ui.container.FlowLayout;
import io.wispforest.owo.ui.container.GridLayout;
import io.wispforest.owo.ui.core.Insets;
import net.minecraft.client.MinecraftClient;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.c2s.play.CloseHandledScreenC2SPacket;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import org.joml.Vector3f;
import org.joml.Vector3i;

import java.util.*;
import java.util.function.BiConsumer;

public class CustomScreen extends BaseUIModelScreen<FlowLayout> {

    private final MinecraftClient mc = MinecraftClient.getInstance();

    private final int syncId;
    private final BlockPos pos;
    private final List<Map.Entry<Item, Integer>> items;
    private Api.Container container;

    private final ArrayList<SmallCheckboxComponent> itemCheckboxes = new ArrayList<>();
    private final HashMap<Api.Per, SmallCheckboxComponent> perCheckBoxes = new HashMap<>();
    private boolean writing = false;

    /**
     * Create a new screen
     * @param syncId The sync ID of the original container open so we can safely close it
     * @param pos The position of the container
     */
    public CustomScreen(int syncId, List<ItemStack> items, BlockPos pos) {
        super(FlowLayout.class, DataSource.asset(new Identifier("tracker", "main")));
        this.syncId = syncId;
        this.pos = pos;
        this.container = Mod.getInstance().api().getContainer(pos.getX(), pos.getY(), pos.getZ());

        // Calculate how much of a specific item we have in the chest
        HashMap<Item, Integer> rawItems = new HashMap<>();
        for (ItemStack item : items) {
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
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        // Ensure E escapes too
        if (keyCode == 69 && !this.writing) {
            this.close();
            return true;
        }

        // Ensure other key presses still get propagated
        return super.keyPressed(keyCode, scanCode, modifiers);
    }


    @Override
    protected void build(FlowLayout rootComponent) {
        // Handle updating the title
        LabelComponent managing = rootComponent.childById(LabelComponent.class, "managing");
        managing.text(Text.literal("Managing Container: X: %s, Y: %s, Z: %s".formatted(this.pos.getX(), this.pos.getY(), this.pos.getZ())));

        // Handle building the item container
        this.buildItemContainer(rootComponent);

        // Handle populating the shop list Todo (notgeri):
        // FlowLayout shopList = rootComponent.childById(FlowLayout.class, "shop-container");
        LabelComponent selectedShop = rootComponent.childById(LabelComponent.class, "selected-shop");
        selectedShop.text(Text.literal(this.container != null && this.container.shop() != null ? this.container.shop().name() : "no shop yet"));

        // Add a note in case there are no items
        LabelComponent note = rootComponent.childById(LabelComponent.class, "note");
        if (this.items.isEmpty()) note.text(Text.literal("No items found!"));

        // Handle updating the summary
        LabelComponent summary = rootComponent.childById(LabelComponent.class, "summary");
        summary.text(Text.translatable("text.tracker.summary", this.pos.getX(), this.pos.getY(), this.pos.getZ()));

        // Handle the untruck button
        ButtonComponent untrack = rootComponent.childById(ButtonComponent.class, "untrack");

        // Handle the per checkboxes
        SmallCheckboxComponent perPiece = rootComponent.childById(SmallCheckboxComponent.class, "per-piece");
        this.perCheckBoxes.put(Api.Per.PIECE, perPiece);

        SmallCheckboxComponent perStack = rootComponent.childById(SmallCheckboxComponent.class, "per-stack");
        this.perCheckBoxes.put(Api.Per.STACK, perStack);

        SmallCheckboxComponent perShulker = rootComponent.childById(SmallCheckboxComponent.class, "per-shulker");
        this.perCheckBoxes.put(Api.Per.SHULKER, perShulker);

        this.perCheckBoxes.forEach((type, checkbox) -> {
            checkbox.onChanged().subscribe(checked -> {
                // Ensure other checkboxes are disabled
                if (checked) {
                    this.perCheckBoxes.values().forEach(c -> {
                        if (c != checkbox) c.checked(false);
                    });
                }
            });
        });

        TextBoxComponent price = rootComponent.childById(TextBoxComponent.class, "price");
        TextBoxComponent amount = rootComponent.childById(TextBoxComponent.class, "amount");

        // Handle setting all the values if the container
        // is pulled from the API successfully
        if (this.container != null) {
            if (this.container.per() != null) this.perCheckBoxes.get(this.container.per()).checked(true);
            price.text(this.container.price() + "");
            amount.text(this.container.amount() + "");
        }

        // Handle the save button
        ButtonComponent save = rootComponent.childById(ButtonComponent.class, "save");
        save.onPress(press -> {
            if (this.container == null) this.container = new Api.Container();
            this.container.setLocation(new Vector3i(this.pos.getX(), this.pos.getY(), this.pos.getZ()));
            this.container.setPrice(Integer.parseInt(price.getText())); // Todo (notgeri):
            this.container.setAmount(Integer.parseInt(amount.getText())); // Todo (notgeri):
            this.container.setShop(Mod.getInstance().api().shops().get(0));

            for (Map.Entry<Api.Per, SmallCheckboxComponent> entry : this.perCheckBoxes.entrySet()) {
                Api.Per type = entry.getKey();
                SmallCheckboxComponent checkbox = entry.getValue();
                if (checkbox.checked()) {
                    this.container.setPer(type);
                    break;
                }
            }
            this.container = Mod.getInstance().api().saveContainer(container);
        });
    }

    private void buildItemContainer(FlowLayout rootComponent) {
        GridLayout itemContainer = rootComponent.childById(GridLayout.class, "item-container");

        int row = 0;

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
