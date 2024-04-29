import type { Container, Handler, Shop } from '../index.ts';
import { Database } from '../utils/database.ts';
import { parseLocationId } from '../utils/helpers.ts';

const db = Database.getInstance();

export const handleContainerEdit: Handler = async (ws, syncId, args) => {

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
        return ws.log(`Invalid location provided: ${(error as any).message}`);
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
        return ws.log('Shop not found');
    }

    // Update properties
    const updated = {
        ...container ?? {},
        ...body,
        ...!untracking ? { shopId: shop?.id } : {},
    };
    db.data.containers[location.hash] = updated;
    await db.save();

    return { command: 'container_update', args: updated, global: true };
};

export const handleContainerDelete: Handler = async (ws, syncId, args) => {
    let location;
    try {
        location = parseLocationId(args.location);
    } catch (error) {
        return ws.log(`Invalid location provided: ${(error as any).message}`);
    }

    const container = db.data.containers[location.hash];
    if (!container) return ws.log(`Could not find container: ${location}`);

    delete (db.data.containers[location.hash]);
    await db.save();

    return { command: 'container_delete', args: location, global: true };
};

