import express from 'express';
import fs from 'fs/promises';
import { v4 as uuidv4 } from 'uuid';

type Location = { x: number; y: number; z: number };

// Load the database
const database = './data.json';
let data: { shops: Shop[], containers: Record<string, Container>, spawn: Location[], ignored: Location[] } = {
    shops: [],
    containers: {},
    spawn: [ { x: 500, y: 500, z: 500 }, { x: -500, y: -500, z: -500 } ],
    ignored: [],
};

const save = async () => await fs.writeFile(database, JSON.stringify(data, null, 4));

try {
    data = JSON.parse((await fs.readFile(database)).toString());
} catch (error) {
    if ((error as any).code === 'ENOENT') {
        await save();
    } else {
        console.error('Unable to load data: ', error);
        process.exit(1);
    }
}

// Initialise the router
const app = express();
app.use(express.json());

type Shop = {
    id: string
    name: string
    owners: string[]
    location: Location
}

type Container = {
    icon: string
    shopId: string
    location: Location
    price: number
    per: 'piece' | 'stack' | 'shulker'
    amount: number
    stocked: number
    customName: string
    lastChecked: number
}

/**
 * Format a location to be used as an ID
 * Returns an error if it's invalid
 */
const getLocationId = (loc: unknown): { x: number, y: number, z: number, hash: string } => {
    if (!loc) throw Error('none provided');
    if (typeof loc !== 'object') throw new Error('not an object');
    if (!('x' in loc) || typeof loc.x !== 'number' || isNaN(Number(loc.x))) throw new Error('invalid X coordinate');
    if (!('y' in loc) || typeof loc.y !== 'number' || isNaN(Number(loc.y))) throw new Error('invalid Y coordinate');
    if (!('z' in loc) || typeof loc.z !== 'number' || isNaN(Number(loc.z))) throw new Error('invalid Z coordinate');
    return {
        x: loc.x,
        y: loc.y,
        z: loc.z,
        hash: `${loc.x}_${loc.y}_${loc.z}`,
    };
};

/**
 * Endpoint for a master read of all shops and their items
 */
app.get('/', async (req, res) => {
    res.send(data);
});

/**
 * Add a new ignored container
 */
app.post('/ignored', async (req, res) => {
    const { x, y, z } = req.body;
    data.ignored.push({ x, y, z });
    await save();
});

/**
 * Remove an ignored container
 */
app.delete('/ignored', async (req, res) => {
    const { x, y, z } = req.body;
    data.ignored = data.ignored.filter(i => i.x !== x && i.y !== y && i.z !== z);
    await save();
});


/**
 * Create a new shop
 */
app.post('/shops', async (req, res) => {
    const shop = { ...req.body, id: uuidv4() };
    data.shops.push(shop);
    res.status(200).json(shop);
    await save();
});

/**
 * Create or update a container
 */
app.post('/containers', async (req, res) => {

    const body: Record<keyof Container, any> = {
        location: req.body.location,
        shopId: req.body.shopId,
        price: req.body.price,
        amount: req.body.amount,
        customName: req.body.customName,
        icon: req.body.icon,
        lastChecked: req.body.lastChecked,
        per: req.body.per,
        stocked: req.body.stocked,
    };

    // See if it already exists
    let location;
    try {
        location = getLocationId(body.location);
    } catch (error) {
        return res.status(400).json({ error: `Invalid location provided: ${(error as any).message}` });
    }

    let container = data.containers[location.hash];

    // Make sure the shop exists
    let shop: Shop | undefined;
    if (container) {
        if (body.shopId) shop = data.shops.find(shop => shop.id === body.shopId);
        else shop = data.shops.find(shop => shop.id === container.shopId);
    } else {
        shop = data.shops.find(shop => shop.id === body.shopId);
    }
    if (!shop) return res.status(404).json({ error: 'Shop not found' });

    // Make sure the location can't be updated
    if (container) delete (body['location']);

    // Update properties
    const updatedContainer = {
        ...container ?? {},
        ...body,
        shopId: shop.id,
    };

    // Report back
    data.containers[location.hash] = updatedContainer;
    res.status(200).json(updatedContainer);
    await save();
});

/**
 * Delete a container
 */
app.delete('/containers', async (req, res) => {

    let location;
    try {
        location = getLocationId(req.body.location);
    } catch (error) {
        return res.status(400).json({ error: `Invalid location provided: ${(error as any).message}` });
    }

    const container = data.containers[location.hash];
    if (!container) res.status(404).json({ error: 'Could not find container!' });

    delete (data.containers[location.hash]);
    await save();
    res.status(200).json();
});

app.listen(8000, () => {
    console.log('⚡');
});
