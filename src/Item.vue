<script setup lang="ts">
import type { Asset } from '@/App.vue';

export type Per = 'piece' | 'stack' | 'shulker'

export type ItemProps = {
    id: string
    amount: number
    per: Per
    price: number
    stocked: boolean
    info?: string
    icon?: string
    asset?: Asset
    iconAsset?: Asset
}

const props = defineProps<ItemProps>();
const iconPath = props.iconAsset?.path ?? props.asset?.path;
</script>

<template>
    <input type="checkbox" :checked="stocked" disabled class="h-6 w-6" />
    <div class="flex flex-row gap-3">
        <img alt="item icon" v-if="iconPath" class="w-8 h-8 object-cover" :src="iconPath">
        <h5>{{ asset?.display ?? props.id }}</h5>
    </div>
    <div>{{ price }} diamond{{ price > 1 ? 's' : '' }} / {{ amount > 1 ? amount : '' }} {{ per }}{{ amount > 1 ? 's' : ''}}</div>
</template>
