<script setup lang="ts">
import { computed, ref } from 'vue';
import { useStore } from '@/store';
import { romanise } from '@/utils';

export type Per = 'piece' | 'stack' | 'shulker'

export type Attributes = {
    flight_duration?: number;
    potion_type?: string;
    enchantments?: Record<string, number>
}

export type ContainerProps = {
    // The namespace ID of the item or the icon
    id: string
    // The custom name if it's not specifically selling what the icon is
    custom_name?: string
    amount: number
    per?: Per
    price: number
    stock: number
    attributes?: Attributes

    x?: number
    y?: number
    z?: number
    world?: string
}

const store = useStore();
const props = defineProps<ContainerProps>();

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

const worldName = computed((): string => {
    switch (props.world?.toLowerCase()) {
        case undefined:
        case '':
        case 'world':
            return '';
        case 'world_nether':
            return 'Nether';
        case 'world_the_end':
            return 'End';
    }
    return '';
});

const showTooltip = ref<'stock' | 'attributes' | false>(false);
const hasEnchants = computed(() => {
    return Object.keys(props.attributes?.enchantments ?? {})?.length ?? 0 > 0;
});
const hasAttributes = computed(() => {
    if (hasEnchants.value) return true;
    if (props.attributes?.flight_duration) return true;
    if (props.attributes?.potion_type) return true;
    return false;
});
</script>

<template>
    <div class="relative">
        <p class="p-3 -m-3" @mouseover="showTooltip = 'stock'" @mouseleave="showTooltip = false">
            {{ custom_name ? '-' : availablePortions }}
        </p>

        <div v-if="showTooltip === 'stock'"
             class="absolute left-0 top-full mt-1 bg-darkest p-2 border rounded shadow-lg z-10">
            <p>{{ props.stock }} total items</p>
            <p>~{{ (props.stock / STACK).toFixed(2) }} stacks</p>
            <p>{{ (props.stock / STACK / SHULKER).toFixed(2) }} shulkers</p>
        </div>
    </div>

    <div class="flex flex-row gap-3">
        <div :class="['relative', {enchant: hasEnchants}]" @mouseover="showTooltip = 'attributes'"
             @mouseleave="showTooltip = false">
            <img alt="item icon" class="w-8 h-8 object-cover" :src="`/items/${id?.replace('minecraft:', '')}.webp`">

            <div v-if="attributes && hasAttributes && showTooltip === 'attributes'"
                 class="absolute left-0 top-full mt-1 bg-darkest p-2 border rounded shadow-lg z-10">

                <div v-if="attributes.enchantments"
                     v-for="[id, level] in Object.entries(attributes.enchantments)">
                    {{ store.translations.enchantments[id] }} {{ level > 1 ? romanise(level) : '' }}
                </div>

                <div v-if="attributes.flight_duration">
                    Flight duration {{ attributes.flight_duration }}
                </div>
            </div>

        </div>

        <h5>{{ custom_name ?? store.translations.items[id] ?? id }}</h5>
    </div>

    <div>
        {{ price }} diamond{{ price > 1 ? 's' : '' }} / {{ amount != 1 ? amount : '' }}
        {{ per }}{{ amount > 1 ? 's' : '' }}
    </div>

    <span>{{ worldName }} {{ x }} {{ y }} {{ z }}</span>
</template>
