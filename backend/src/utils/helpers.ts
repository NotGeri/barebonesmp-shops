/**
 * Format a location to be used as an ID
 * Returns an error if it's invalid
 */
export const parseLocationId = (loc: unknown): { x: number, y: number, z: number, hash: string } => {
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
