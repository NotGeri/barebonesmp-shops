import fs from 'fs/promises';

const translations: Record<'enchantments' | 'items', Record<string, string>> = {
    enchantments: {},
    items: {},
};

for (const [ key, value ] of Object.entries(JSON.parse((await fs.readFile('assets/minecraft/lang/en_us.json')).toString()))) {
    if (!(typeof value === 'string')) continue;
    const formatted = key.replace(/^.*?.minecraft./, 'minecraft:');

    if (key.startsWith('enchantment.minecraft')) {
        translations.enchantments[formatted] = value;
    } else if (key.startsWith('block.minecraft') || key.startsWith('item.minecraft')) {
        translations.items[formatted] = value;
    }
}

await fs.writeFile('translations.json', JSON.stringify(translations));
