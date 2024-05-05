package dev.geri.shops.data;

import org.bukkit.Location;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;

public class Data {

    private ArrayList<Shop> shops;
    private HashMap<String, Container> containers;
    public transient boolean hasChanges = false;

    /**
     * @return The string formatted location that we can use a sort of hash
     */
    public String formatLocation(Location location) {
        return "%s:%s,%s,%s".formatted(location.getWorld() != null ? location.getWorld().getName() : null, location.getX(), location.getY(), location.getZ());
    }

    /**
     * Save a container at a specific location
     */
    public void saveContainer(Location location, Container container) {
        this.hasChanges = true;
        container.setShop(getShop(container.shopName()));
        this.containers.put(this.formatLocation(location), container);
    }

    /**
     * Get a container at a specific location
     * Note that this will NOT adjust for any
     * special cases, such as double chests.
     *
     * @return The container or null if not found
     */
    public Container getContainer(Location location) {
        return this.containers.get(this.formatLocation(location));
    }

    /**
     * Delete a container at a given location. If the shop does
     * not have any other containers linked, it will also be deleted
     */
    public void removeContainer(Location location) {
        this.hasChanges = true;
        String key = this.formatLocation(location);
        Container container = this.containers.get(key);
        if (container == null) return;

        // If this was the last container for the shop,
        // untrack that as well
        List<Container> shopContainers = this.containers.values().stream().filter(c -> c.shopName() != null && c.shopName().equals(container.shopName())).toList();
        if (shopContainers.isEmpty()) {
            this.removeShop(container.shopName());
        }

        this.containers.remove(key);
    }

    /**
     * Get a shop by its name
     *
     * @return The shop or null if not found
     */
    public Shop getShop(String name) {
        return this.shops.stream().filter(s -> Objects.equals(s.name(), name)).findFirst().orElse(null);
    }

    /**
     * Save a shop by its name
     */
    public void saveShop(Shop shop) {
        this.removeShop(shop.name());
        this.shops.add(shop);
    }

    /**
     * Delete a shop by its name
     */
    public void removeShop(String name) {
        this.shops.removeIf(s -> s.name().equals(name));
    }

    /**
     * Initialise all the containers with their shops
     */
    public void init() {
        if (this.shops == null) this.shops = new ArrayList<>();
        if (this.containers == null) this.containers = new HashMap<>();
        this.containers.values().forEach(container -> {
            container.setShop(getShop(container.shopName()));
        });
    }

}
