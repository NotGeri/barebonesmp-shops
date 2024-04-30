package dev.geri.tracker.vendor;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public interface WaypointCreationEvent {
    Event<WaypointCreationEvent> EVENT = EventFactory.createArrayBacked(WaypointCreationEvent.class,
            (listeners) -> (world, pos, name, description, colour) -> {
                boolean result = false;
                for (WaypointCreationEvent listener : listeners) {
                    result |= listener.onCreateWaypoint(world, pos, name, description, colour);
                }
                return result;
            }
    );

    enum Colour {
        BLACK(0x000000, 0),
        DARK_BLUE(0x0000AA, 1),
        DARK_GREEN(0x00AA00, 2),
        DARK_AQUA(0x00AAAA, 3),
        DARK_RED(0xAA0000, 4),
        DARK_PURPLE(0xAA00AA, 5),
        GOLD(0xFFAA00, 6),
        GRAY(0xAAAAAA, 7),
        DARK_GRAY(0x555555, 8),
        BLUE(0x5555FF, 9),
        GREEN(0x55FF55, 10),
        AQUA(0x55FFFF, 11),
        RED(0xFF0000, 12),
        LIGHT_PURPLE(0xFF55FF, 13),
        YELLOW(0xFFFF55, 14),
        WHITE(0xFFFFFF, 15);

        public final int journeyMap;
        public final int xaerosMinimap;

        Colour(int journeyMap, int xaerosMinimap) {
            this.journeyMap = journeyMap;
            this.xaerosMinimap = xaerosMinimap;
        }
    }

    boolean onCreateWaypoint(World world, BlockPos pos, String name, String description, Colour colour);
}
