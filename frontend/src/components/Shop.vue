<script setup lang="ts">
import Container, { type ContainerProps } from '@/components/Container.vue';
import { computed } from 'vue';

export type ShopProps = {
    name: string
    owners: string[]
    description?: string
    containers: ContainerProps[]
}

const props = defineProps<ShopProps & { groupSimilar: boolean }>();

const containers = computed(() => {
    if (!props.groupSimilar) return props.containers;
    const groupedContainers: { [key: string]: ContainerProps } = {};
    for (const container of props.containers ?? []) {
        const key = `${container.id}-${container.custom_name}-${container.amount}-${container.per}-${container.price}`;
        if (groupedContainers[key]) {
            groupedContainers[key].stock += container.stock;
        } else {
            groupedContainers[key] = { ...container };
        }
    }
    return Object.values(groupedContainers);
});

</script>

<template>
    <div class="bg-darkest rounded-xl p-5 relative">
        <div class="mb-3 flex flex-col gap-1">
            <h1>{{ name }}</h1>
            <p class="text-muted">{{ description }}</p>
            <p class="text-xs" v-if="owners">
                Owned by: <span class="text-green-400">{{ owners.join(', ') }}</span>
            </p>
        </div>

        <div v-if="containers && containers.length > 0" class="grid grid-cols-[0.3fr,4fr,4fr,2fr] gap-3 items-center whitespace-nowrap">
            <h3>Stock</h3>
            <h3>Item/Service</h3>
            <h3>Pricing</h3>
            <h3>Location</h3>
            <Container v-for="(container, index) in containers"
                       :key="index"
                       v-bind="container"
            />
        </div>
        <p v-else class="text-muted text-center text-xs">No items available!</p>
    </div>
</template>
