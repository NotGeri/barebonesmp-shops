import { Container, Location, Shop } from '../index';
import fs from 'fs/promises';

type State = {
    shops: Shop[]
    containers: Record<string, Container>
    spawn: Location[]
}

export class Database {

    private static instance: Database;

    private readonly path: string = './data.json';
    public data: State = { shops: [], containers: {}, spawn: [] };

    private constructor() {
    }

    /**
     * Access the singleton instance
     */
    public static getInstance(): Database {
        if (!Database.instance) {
            Database.instance = new Database();
        }

        return Database.instance;
    }

    /**
     * Load the data from the disk
     */
    public async load() {
        try {
            this.data = JSON.parse((await fs.readFile(this.path)).toString());
        } catch (error) {
            if ((error as any).code === 'ENOENT') {
                await this.save();
            } else {
                throw error;
            }
        }
        if (this.data) this.data.spawn = [ { x: -1, y: -256, z: -26 }, { x: -7, y: 256, z: -32 } ];
    }

    /**
     * Save the data to the disk
     */
    public async save() {
        return await fs.writeFile(this.path, JSON.stringify(this.data, null, 4));
    }

}


