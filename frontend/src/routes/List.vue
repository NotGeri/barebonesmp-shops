<script setup lang="ts">
import { computed, onMounted, ref } from 'vue';
import { type ContainerProps } from '@/components/Container.vue';
import Shop, { type ShopProps } from '@/components/Shop.vue';
import { useStore } from '@/store';

const store = useStore();
const shops = ref<ShopProps[]>([]);

const input = ref();
const query = ref<string>('');
const groupSimilar = ref<boolean>(true);
const loading = ref<boolean>(true);

const refetch = async () => {
    const response = await fetch(import.meta.env.VITE_BACKEND);
    const data = await response.json() as { containers: ContainerProps[], shops: ShopProps[] };
    shops.value = data.shops;
};

onMounted(async () => {
    // Focus the search input
    if (input.value) input.value.focus();
    window.addEventListener('keydown', event => {
        if (event.ctrlKey && event.key === 'f') {
            event.preventDefault();
            input.value.focus();
        }
    });

    // Fetch the API initially
    await refetch();
    loading.value = false;

    // Refetch every minute
    setInterval(refetch, 1000 * 60);
});

const filteredShops = computed((): ShopProps[] => {
    let filtered: ShopProps[] = [];

    if (query.value) {
        const lower = query.value.toLowerCase();
        filtered = shops.value.reduce<ShopProps[]>((acc, shop) => {

            const matchingContainers = shop.containers.filter(container => // Todo (notgeri):
                container.id?.toLowerCase().includes(lower) || store.translations.items[container.id]?.toLowerCase().includes(lower),
            ) ?? [];

            if (matchingContainers.length > 0) {
                acc.push({
                    ...shop,
                    containers: matchingContainers,
                });
            }

            return acc;
        }, []);
    } else {
        filtered = shops.value;
    }

    return filtered.sort((a, b) => a.name.localeCompare(b.name));
});
</script>

<template>
    <div class="m-auto flex flex-col items-center gap-3 max-w-6xl bg-darkest p-5 rounded-xl my-5">
        <h1>BarebonesMP Shops</h1>
        <p>
            This is a community maintained list of shops for
            <a target="_blank" href="https://barebonesmp.com">BarebonesMP</a>.
        </p>
        <p>
            Learn how you can set up your shops
            <router-link to="/tutorial">here.</router-link>
        </p>
        <input class="text-black p-1.5 w-1/3 rounded" ref="input" v-model="query" placeholder="Search for items...">
        <div class="flex flex-row gap-1">
            <input v-model="groupSimilar" type="checkbox"/>
            <label>Group similar</label>
        </div>
    </div>

    <p v-if="loading" class="text-blue-400 text-center">Loading...</p>
    <div v-else class="flex flex-col gap-3 w-2/3 max-w-6xl m-auto mb-10">
        <Shop v-if="filteredShops.length > 0"
              v-for="(shop, index) in filteredShops" :key="index"
              v-bind="shop"
              :groupSimilar="groupSimilar"
        />
        <p v-else class="text-red-400 text-center">No results found! Time to start selling it?!</p>
    </div>
</template>

