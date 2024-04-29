import { type Handler } from '../index.ts';
import { validate } from 'uuid';
import { Database } from '../utils/database.ts';

const db = Database.getInstance();

export const handleAuth: Handler = async (ws, syncId, args) => {
    if (!validate(args?.uuid)) {
        ws.send('Invalid UUID');
        ws.close();
        return;
    }

    ws.player = { uuid: args.uuid };
    ws.log('authenticated successfully');

    ws.command('auth', null, db.data);
};
