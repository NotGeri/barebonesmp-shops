import { defineStore } from 'pinia';
import index from '@/assets/translations.json';

export type Translations = {
    enchantments: Record<string, string>
    items: Record<string, string>
}

type Data = {
    translations: Translations
}

export const useStore = defineStore('data', {
        state: (): Data => {
            return {
                translations: index as Translations,
            };
        },
    },
);

export class Asset {
}
