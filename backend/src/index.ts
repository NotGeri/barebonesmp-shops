import express, { Request } from 'express';
import WebSocket, { WebSocketServer } from 'ws';
import { Database } from './utils/database.ts';
import { validate } from 'uuid';
import { handleEdit } from './routes/containers.ts';

export type Location = { x: number; y: number; z: number };

export type Shop = {
    id: string
    name: string
    owners: string[]
    location: Location
}

export type Container = {
    location: Location
    untracked?: true
    icon?: string
    shopId?: string
    price?: number
    per?: 'piece' | 'stack' | 'shulker'
    amount?: number
    stocked?: number
    customName?: string
    lastChecked?: number
}

// Load the database
const db = Database.getInstance();
try {
    await db.load();
} catch (err) {
    console.error('Unable to load database: ', err);
    process.exit(1);
}

// Initialise the router
const app = express();

// Use JSON body
app.use(express.json());

// Log requests
app.use((req, res, next) => {
    console.log(`[${req.ip}] [${req.method}] ${req.originalUrl}`);
    next();
});

// Add the routes
app.get('/api', async (req, res) => res.send(db.data));

// Start the HTTP Server
const server = app.listen(8000, () => {
    console.log('HTTP API started');
});

// Set up the websocket server
const wss = new WebSocketServer({ server });

type Player = {
    uuid: string
}

type ExtendedWebSocket = WebSocket & { player: Player | null }

wss.on('connection', (ws: ExtendedWebSocket, req: Request) => {
    const log = (message: string) => {
        console.log(`${req.socket.remoteAddress} [WS] ${ws.player?.uuid ? `(${ws.player?.uuid}) ` : ''}${message}`);
    };

    log('connected');

    ws.player = null;

    ws.on('message', async (message) => {

        // Parse the command and arguments
        const raw = message?.toString()?.split(' ');
        const command = raw[0] ?? null;
        delete (raw[0]);
        const rawArgs = raw?.join('');
        const args = rawArgs ? JSON.parse(rawArgs) : null;

        switch (command) {
            // Handle authenticating the user
            case 'auth':
                if (!validate(args?.uuid)) {
                    ws.send('Invalid UUID');
                    ws.close();
                    return;
                }
                ws.player = { uuid: args.uuid };
                log('authenticated successfully');

                ws.send(`authenticated ${JSON.stringify(db.data)}`);
                break;

            case 'update':
                break;

            case 'edit':
                await handleEdit(log, args)
                break;

            default:
                log(`send unknown command ${command}`);
                break;
        }
    });

    ws.on('close', () => {
        log('disconnected');
    });
});
