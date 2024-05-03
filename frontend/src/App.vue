<script setup lang="ts">
import index from '@/assets/index.json';
import { computed, onMounted, ref } from 'vue';
import Item, { type ItemProps, type Per } from '@/Item.vue';

export type Asset = { display: string, path: string };
const assets = index as Record<string, Asset>;

type Shop = {
    name: string
    x: number
    y: number
    z: number
    owner: string
    description?: string
    items: ItemProps[]
}

const url = 'https://docs.google.com/spreadsheets/d/e/2PACX-1vRwi_3q0C-mhjYLNzRsDzj8aZ-7CfnKi6LN6VPxSEsv8fDmXOzIqhd3uJpQGyPDZUNGZF7ojpaicLdl/pub?output=tsv';
const shops = ref<Shop[]>([]);
const input = ref();
const query = ref<string>('');
const loading = ref<boolean>(true);

onMounted(async () => {
    // Focus the search input
    if (input.value) input.value.focus();
    window.addEventListener('keydown', event => {
        if (event.ctrlKey && event.key === 'f') {
            event.preventDefault();
            input.value.focus();
        }
    });

    // Fetch the sheet
    const response = await fetch(url);
    const rows = (await response.text()).replace(/\r/g, '').split('\n');
    rows.shift(); // yeet header

    shops.value = [];
    let currentShop: Shop | null = null;
    for (const row of rows) {
        const columns = row.split('\t');
        if (!columns[0]) continue; // Skip empty rows

        // Shop headers
        if (row.startsWith('#')) {

            // Push the previous value
            if (currentShop) shops.value.push(currentShop);

            const matches = /#(\d+) (\d+) (\d+)/.exec(row);
            if (!matches) continue;

            currentShop = {
                name: columns[1],
                x: Number(matches[1]),
                y: Number(matches[2]),
                z: Number(matches[3]),
                owner: columns[2] ?? undefined,
                description: columns[3] ?? undefined,
                items: []
            };
            continue;
        }

        if (!currentShop) continue;

        // Items
        const [ id, amount, per, price, noStock, info, icon ] = columns;
        currentShop.items.push({
            id,
            amount: Number(amount),
            per: (per as Per),
            price: Number(price),
            stocked: noStock === 'FALSE',
            info,
            icon,
            asset: assets[id.replace('minecraft:', '')] ?? undefined,
            iconAsset: icon ? assets[icon.replace('minecraft:', '')] ?? undefined : undefined,
        });

    }

    // Last shop
    if (currentShop) shops.value.push(currentShop);
    loading.value = false;
});

const filteredShops = computed((): Shop[] => {
    if (!query.value) return shops.value;
    const lower = query.value.toLowerCase();

    return shops.value.reduce<Shop[]>((acc, shop) => {

        const matchingItems = shop.items.filter(item =>
            item.id.toLowerCase().includes(lower) || item?.asset?.display?.toLowerCase().includes(lower) ||
            (item.info && item.info.toLowerCase().includes(lower))
        );

        if (matchingItems.length > 0) {
            acc.push({
                ...shop,
                items: matchingItems
            });
        }

        return acc;
    }, []);
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
            You can update any incorrect values or create new entries
            <a target="_blank"
               href="https://docs.google.com/spreadsheets/d/1CC2esYy1qUmtMAwdrMtVq322qAGbunvTAD0qjOBCdCU/edit?usp=sharing">here</a>!
        </p>
        <input class="text-black p-2 w-full" ref="input" v-model="query" placeholder="Search for items...">
    </div>

    <p v-if="loading" class="text-blue-400 text-center">Loading...</p>
    <div v-else class="flex flex-col gap-3 max-w-6xl m-auto mb-10">
        <div v-if="filteredShops.length > 0" v-for="(shop, index) in filteredShops" :key="index"
             class="bg-darkest rounded-xl p-5 relative">
            <div class="mb-3">
                <h1>{{ shop.name }}</h1>
                <p class="text-xs" v-if="shop.owner">
                    Owned by:
                    <span class="text-green-400">{{ shop.owner }}</span>
                </p>
                <div class="absolute right-3 top-3 flex flex-col items-end">
                    <h3 class="text-green-400">
                        X: {{ shop.x }}, Y: {{ shop.x }}, Z: {{ shop.z }}
                    </h3>
                    <a class="flex flex-row items-center gap-1" target="_blank"
                       :href="`https://map.barebonesmp.com/#world:${shop.x}:${shop.y}:${shop.z}:21:0:0:0:0:perspective`">
                        <img src="/item/filled_map.png" alt="map" class="h-8 w-8"/>
                        View in Bluemap
                    </a>
                </div>

            </div>
            <p>{{ shop.description }}</p>
            <div class="grid grid-cols-[6em,0.5fr,1fr] gap-3">
                <h3>Stocked</h3>
                <h3>Item/Service</h3>
                <h3>Pricing</h3>
                <Item v-for="item in shop.items" :key="item.id" v-bind="item" :assets="assets"/>
            </div>
        </div>
        <p v-else class="text-red-400 text-center">No results found! Time to start selling it?!</p>
    </div>
</template>

