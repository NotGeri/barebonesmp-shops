package dev.geri.tracker.vendor;

import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import xaero.common.XaeroMinimapSession;
import xaero.common.core.XaeroMinimapCore;
import xaero.common.minimap.waypoints.Waypoint;
import xaero.common.minimap.waypoints.WaypointsManager;

public class XaerosMinimap {

    /**
     * Initialise the listener for the waypoint creation event
     */
    public static void init() {
        WaypointCreationEvent.EVENT.register((World world, BlockPos pos, String name, String description, WaypointCreationEvent.Colour colour) -> {
            XaeroMinimapSession session = XaeroMinimapCore.currentSession;
            if (session == null) return false;

            WaypointsManager manager = session.getWaypointsManager();
            if (manager == null) return false;

            manager.getWaypoints().getList().add(new Waypoint(pos.getX(), pos.getY(), pos.getZ(), name, description, colour.xaerosMinimap, 0, true, true));
            return true;
        });
    }

}
