import { defineStore } from 'pinia';
import index from '@/assets/index.json';

export type Asset = {
    display: string
    path: string
    type: string
}

export type Assets = {
    enchantments: Record<string, string>
    entries: Record<string, Asset>
}

type Data = {
    assets: Assets
}

export const useStore = defineStore('data', {
        state: (): Data => {
            return {
                assets: index as Assets,
            };
        },
    },
);
