<script setup lang="ts">
import type { Asset } from '@/routes/List.vue';
import { computed } from 'vue';

export type Per = 'piece' | 'stack' | 'shulker'

export type ContainerProps = {
    shopName?: string
    // The namespace ID of the item or the icon
    id: string
    // The custom name if it's not specifically selling what the icon is
    customName?: string
    amount: number
    per?: Per
    price: number
    stock: number

    x?: number
    y?: number
    z?: number
    world?: string

    asset?: Asset
}

const props = defineProps<ContainerProps>();
const iconPath = props.asset?.path;

const STACK = 64;
const SHULKER = 27;
const itemsPerPortion = computed(() => {
    switch (props.per) {
        case 'shulker':
            return SHULKER * STACK;
        case 'stack':
            return STACK;
        default:
            return 1;
    }
});

// Compute the number of available portions
const availablePortions = computed(() => {
    const totalItems = props.stock;
    if (!totalItems || totalItems === 0) return totalItems;
    const portionSize = itemsPerPortion.value * props.amount;
    if (portionSize === 0) return 0;
    return Math.floor(totalItems / portionSize);
});

const itemNumberTooltip = computed((): string => {
    return `${props.stock} total items, ~${(props.stock / STACK).toFixed(2)} stacks, ${(props.stock / STACK / SHULKER).toFixed(2)} shulkers`;
});
</script>

<template>
    <p :title="customName ? 'Unable to track custom named item/service' : itemNumberTooltip">
        {{ customName ? '-' :  availablePortions }}
    </p>
    <div class="flex flex-row gap-3">
        <img alt="item icon" v-if="iconPath" class="w-8 h-8 object-cover" :src="iconPath">
        <h5 :title="`${world} ${x} ${y} ${z}`">{{ customName ?? asset?.display ?? id }}</h5>
    </div>
    <div>
        {{ price }} diamond{{ price > 1 ? 's' : '' }} / {{ amount != 1 ? amount : '' }}
        {{ per }}{{ amount > 1 ? 's' : '' }}
    </div>
</template>
