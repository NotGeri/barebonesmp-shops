import fs from 'fs/promises';
import { join } from 'path';
import { Readable } from 'stream';
import { createWriteStream } from 'fs';
import { finished } from 'stream/promises';
import decompress from 'decompress';

const downloadFile = (async (url, fileName) => {
    const res = await fetch(url);
    const fileStream = createWriteStream(fileName, {flags: 'wx'});
    await finished(Readable.fromWeb(res.body).pipe(fileStream));
});

try {
    await fs.rm('build/', {recursive: true});
    await fs.rm('public/item/', {recursive: true});
    await fs.rm('public/block/', {recursive: true});
} catch (e) {

}

await fs.mkdir('build/', {recursive: true});
await downloadFile('https://piston-data.mojang.com/v1/objects/fd19469fed4a4b4c15b2d5133985f0e3e7816a8a/client.jar', 'build/tmp.jar');

await decompress('build/tmp.jar', 'build/', {
    filter: file => {
        for (const start of ['assets/minecraft/textures/block', 'assets/minecraft/textures/item', 'assets/minecraft/lang']) {
            if (file.path.startsWith(start)) {
                return true;
            }
        }
        return false;
    }
});

const assets = {};

const lang = JSON.parse((await fs.readFile('build/assets/minecraft/lang/en_us.json')).toString());
for (const [key, value] of Object.entries(lang)) {

    let block = false;
    let id = null;
    if (key.startsWith('block.minecraft')) {
        id = key.replace('block.minecraft.', '');
        block = true;
    } else if (key.startsWith('item.minecraft')) {
        id = key.replace('item.minecraft.', '');
    }

    const exists = async (p) => await fs.access(p).then(() => true).catch(() => false);

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

    assets[id] = {display: value, path: texturePath};
}

await fs.rename('build/assets/minecraft/textures/block', 'public/block');
await fs.rename('build/assets/minecraft/textures/item', 'public/item');
await fs.writeFile('src/assets/index.json', JSON.stringify(assets));
