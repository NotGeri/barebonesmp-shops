import express, { Request } from 'express';
import WebSocket, { WebSocketServer } from 'ws';
import { Database } from './utils/database.ts';
import { handleAuth } from './routes/auth.ts';
import { handleContainerDelete, handleContainerEdit } from './routes/containers.ts';
import { v4 } from 'uuid';

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

type Command = 'auth' | 'container_update' | 'container_delete';

type Player = { uuid: string };
type CommandSender = (command: Command, syncId: number | null, args?: any) => void;
type ExtendedWebSocket = WebSocket & {
    uuid: string
    player?: Player
    log: (message: string) => void
    command: CommandSender
    commandAll: CommandSender
};

export type Handler = (
    ws: ExtendedWebSocket,
    syncId: number | null,
    args: any,
) => Promise<{ // If we return some results, it will automatically be sent to the client
    command: Command
    args?: any,
    global?: boolean, // Whether to also echo the event to other clients without the sync ID
} | void>;

const handlers: Record<Command, Handler> = {
    'auth': handleAuth,
    'container_update': handleContainerEdit,
    'container_delete': handleContainerDelete,
};

let clients: ExtendedWebSocket[] = [];
wss.on('connection', (ws: ExtendedWebSocket, req: Request) => {
    ws.uuid = v4();
    clients.push(ws);

    ws.log = (message: string) => console.log(`${req.socket.remoteAddress} [WS] ${ws.player?.uuid ? `(${ws.player?.uuid}) ` : ''}${message}`);
    ws.log(`connected (${clients.length} connected)`);

    /**
     * Send a command to the client
     * @param command
     * @param syncId
     * @param args
     */
    ws.command = (command, syncId, args) => {
        ws.send(`${command} ${syncId} ${args ? JSON.stringify(args) : ''}`);
    };

    /**
     * Send a command to all connected clients
     * @param command
     * @param syncId
     * @param args
     */
    ws.commandAll = (command, syncId, args) => {
        clients.forEach(client => {
            client.command(command, syncId, args);
        });
    };

    ws.on('message', async (message) => {
        const parts = message?.toString()?.split(' ');
        const command = parts[0] ?? null;
        const syncId = parts.length > 1 ? Number(parts[1]) : null;
        const args = parts.length > 2 ? JSON.parse(parts[2]) : null;

        // Check if it's a valid command
        if (!(command in handlers)) {
            ws.log(`send unknown command ${command}`);
            return;
        } else {
            ws.log(`${command} (#${syncId})`);
        }

        // Execute the handler and return any data automatically
        const result = await handlers[command as Command](ws, syncId, args);
        if (!result) return;

        ws.command(result.command, syncId, result.args);
        if (result.global) {
            clients.forEach(client => {
                if (client.uuid === ws.uuid) return;
                client.command(result.command, null, result.args);
            });
        }
    });

    /**
     * Clean up closed sockets
     */
    ws.on('close', () => {
        clients = clients.filter(c => c.uuid && c.uuid !== ws.uuid);
        ws.log(`disconnected (${clients.length} connected)`);
    });
});
