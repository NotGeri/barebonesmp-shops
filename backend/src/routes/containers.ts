import { Request, Response } from 'express';
import type { Container, Shop } from '../index.ts';
import { Database } from '../utils/database.ts';
import { parseLocationId } from '../utils/helpers.ts';

const db = Database.getInstance();

export const handleEdit = async (log: (message: string) => void, args: any) => {

    const body: Record<keyof Container, any> = {
        untracked: Boolean(args.untracked),
        location: args.location,
        shopId: args.shopId,
        price: Number(args.price),
        amount: Number(args.amount),
        customName: args.customName,
        icon: args.icon,
        lastChecked: args.lastChecked,
        per: args.per,
        stocked: args.stocked,
    };

    // See if it already exists
    let location;
    try {
        location = parseLocationId(body.location);
    } catch (error) {
        return log(`Invalid location provided: ${(error as any).message}`);
    }

    // Make sure the location can't be updated
    let container = db.data.containers[location.hash];
    if (container) delete (body['location']);

    const untracking = body.untracked || (container?.untracked && body.untracked === undefined);

    // Make sure the shop exists
    let shop: Shop | undefined;
    if (container) {
        if (body.shopId) shop = db.data.shops.find(shop => shop.id === body.shopId);
        else shop = db.data.shops.find(shop => shop.id === container.shopId);
    } else {
        shop = db.data.shops.find(shop => shop.id === body.shopId);
    }
    if (!untracking && !shop) {
        return log('Shop not found');
    }

    // Update properties
    const updatedContainer = {
        ...container ?? {},
        ...body,
        ...!untracking ? { shopId: shop?.id } : {},
    };

    // Report back
    db.data.containers[location.hash] = updatedContainer;
    await db.save();
};

export const deleteContainer = async (req: Request, res: Response) => {

    let location;
    try {
        location = parseLocationId(req.body.location);
    } catch (error) {
        return res.status(400).json({ error: `Invalid location provided: ${(error as any).message}` });
    }

    const container = db.data.containers[location.hash];
    if (!container) res.status(404).json({ error: 'Could not find container!' });

    delete (db.data.containers[location.hash]);
    await db.save();
    res.status(200).json();
};

