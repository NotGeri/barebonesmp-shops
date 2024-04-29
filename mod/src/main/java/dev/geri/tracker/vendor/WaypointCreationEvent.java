package dev.geri.tracker.vendor;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public interface WaypointCreationEvent {
    Event<WaypointCreationEvent> EVENT = EventFactory.createArrayBacked(WaypointCreationEvent.class,
            (listeners) -> (world, pos, id, name) -> {
                boolean result = false;
                for (WaypointCreationEvent listener : listeners) {
                    result |= listener.onCreateWaypoint(world, pos, id, name);
                }
                return result;
            }
    );

    boolean onCreateWaypoint(World world, BlockPos pos, String id, String name) throws Exception;
}
