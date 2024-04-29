package dev.geri.tracker.vendor;

import dev.geri.tracker.Mod;
import journeymap.client.api.IClientAPI;
import journeymap.client.api.IClientPlugin;
import journeymap.client.api.display.DisplayType;
import journeymap.client.api.display.Waypoint;
import journeymap.client.api.event.ClientEvent;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class JourneyMap implements IClientPlugin {

    private IClientAPI api = null;

    @Override
    public void initialize(IClientAPI api) {
        this.api = api;
        WaypointCreationEvent.EVENT.register(this::createWaypoint);
    }

    @Override
    public String getModId() {
        return Mod.ID;
    }

    @Override
    public void onEvent(ClientEvent event) {

    }

    /**
     * Create a new JourneyMap waypoint
     *
     * @throws Exception In case it fails for any reason
     */
    public boolean createWaypoint(World world, BlockPos pos, String id, String name) throws Exception {
        if (this.api == null || !this.api.playerAccepts(Mod.ID, DisplayType.Waypoint)) return false;
        this.api.show(new Waypoint(Mod.ID, id, name, world.getRegistryKey(), pos).setColor(0x00ffff));
        return true;
    }

}
