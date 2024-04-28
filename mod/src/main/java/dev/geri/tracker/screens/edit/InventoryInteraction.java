package dev.geri.tracker.screens.edit;

import dev.geri.tracker.utils.Api;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public record InventoryInteraction(
        int syncId,
        List<ItemStack> items,
        BlockPos pos,
        @Nullable Api.Container container
) {}
