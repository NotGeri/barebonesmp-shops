import fs from 'fs/promises';
import { join } from 'path';
import { Readable } from 'stream';
import { createWriteStream } from 'fs';
import { finished } from 'stream/promises';
import decompress from 'decompress';

// Remove all previous files
try {
    await fs.rm('build/', { recursive: true });
    await fs.rm('src/assets/item/', { recursive: true });
    await fs.rm('src/assets/block/', { recursive: true });
} catch (e) {
    console.error('Unable to remove previous files', e);
}

// Create a new build directory
await fs.mkdir('build/', { recursive: true });


// Download the client JAR
await (async () => {
    const res = await fetch('https://piston-data.mojang.com/v1/objects/fd19469fed4a4b4c15b2d5133985f0e3e7816a8a/client.jar');
    const fileStream = createWriteStream('build/client.jar', { flags: 'wx' });
    if (!res.body) return;
    await finished(Readable.fromWeb(res.body).pipe(fileStream));
})();

// Decompress the JAR
await decompress('build/client.jar', 'build/', {
    filter: file => {
        for (const start of [ 'assets/minecraft/textures/block', 'assets/minecraft/textures/item', 'assets/minecraft/lang' ]) {
            if (file.path.startsWith(start)) {
                return true;
            }
        }
        return false;
    },
});

const generate: { enchantments: Record<string, any>, entries: Record<string, any> } = {
    enchantments: {},
    entries: {},
};

// Go through all the translation keys
for (const [ key, value ] of Object.entries(JSON.parse((await fs.readFile('build/assets/minecraft/lang/en_us.json')).toString()))) {

    // Save enchantment names
    if (key.startsWith('enchantment.minecraft')) {
        generate.enchantments[key.replace('enchantment.minecraft.', 'minecraft:')] = value;
        continue;
    }

    // Check if they are a block or item
    let block = false;
    let id = null;
    if (key.startsWith('block.minecraft')) {
        id = key.replace('block.minecraft.', '');
        block = true;
    } else if (key.startsWith('item.minecraft')) {
        id = key.replace('item.minecraft.', '');
    }

    const exists = async (p: string) => await fs.access(p).then(() => true).catch(() => false);

    if (!id) continue;
    const tmpPath = 'build/assets/minecraft/textures';
    let texturePath = `${block ? 'block' : 'item'}/${id}.png`;

    // Try the block top if it doesn't exist as a regular one
    if (!await exists((join(tmpPath, texturePath)))) {
        if (block) {
            texturePath = `block/${id}_top.png`;
            if (!await exists(join(tmpPath, texturePath))) {
                continue;
            }
        } else {
            continue;
        }
    }

    generate.entries[id] = { display: value, path: texturePath, type: block ? 'block' : 'item' };
}

await fs.rename('build/assets/minecraft/textures/block', 'src/assets/block');
await fs.rename('build/assets/minecraft/textures/item', 'src/assets/item');
await fs.writeFile('src/assets/index.json', JSON.stringify(generate));
await fs.rm('build/', { recursive: true });
