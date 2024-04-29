import { v4 } from 'uuid';
import { Request, Response } from 'express';
import { Database } from '../utils/database.ts';

const db = Database.getInstance();

export const createShop = async (req: Request, res: Response) => {
    const shop = { ...req.body, id: v4() };
    db.data.shops.push(shop);
    res.status(200).json(shop);
    await db.save();
};
