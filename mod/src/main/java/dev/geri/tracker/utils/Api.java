package dev.geri.tracker.utils;

import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;
import dev.geri.tracker.Mod;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.BlockPos;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import org.joml.Vector3i;

import java.net.URI;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

public class Api extends WebSocketClient {

    private final Gson gson = new Gson();

    private int syncId = 0;
    private final Map<Integer, CompletableFuture<String>> pendingResponses = new ConcurrentHashMap<>();

    private Data data = null;

    public enum Command {
        AUTH("auth"),
        UPDATE_CONTAINER("container_update"),
        DELETE_CONTAINER("container_delete"),
        CREATE_SHOP("shop_create"),
        UPDATE_SHOP("shop_update"),
        DELETE_SHOP("shop_delete"),
        UNKNOWN(null);

        public final String raw;

        Command(String raw) {
            this.raw = raw;
        }
    }

    private final Mod mod = Mod.getInstance();
    public Api( URI uri) {
        super(uri);
    }

    @Override
    public void onOpen(ServerHandshake data) {
        this.doubleCheck();
        if (MinecraftClient.getInstance().player == null) return;
        this.command(Command.AUTH, Map.of("uuid", MinecraftClient.getInstance().player.getUuid()));
    }

    @Override
    public void onMessage(String raw) {
        String[] parts = raw.split(" ", 3);

        String rawCommand = parts.length > 0 ? parts[0] : "";
        String rawSyncId = parts.length > 1 ? parts[1] : "";
        String rawArgs = parts.length > 2 ? parts[2] : null;

        Command command = Arrays.stream(Command.values()).filter(msg -> msg.raw.equals(rawCommand)).findFirst().orElse(null);

        Integer syncId = null;
        try {
            syncId = rawSyncId != null && !rawSyncId.equals("null") ? Integer.parseInt(rawSyncId) : null;
        } catch (NumberFormatException exception) {
            Mod.LOGGER.warn("Invalid sync ID received: {}", rawSyncId);
        }

        if (syncId != null) {
            CompletableFuture<String> responseFuture = this.pendingResponses.remove(syncId);
            if (responseFuture != null) {
                responseFuture.complete(rawArgs);
                return;
            }
        }

        // Handle regular commands
        switch (command != null ? command : Command.UNKNOWN) {
            case AUTH -> {
                this.data = this.gson.fromJson(rawArgs, Data.class);
                this.doubleCheck();

                for (Container container : this.data.containers.values()) container.loadShop(this.data.shops);
            }

            case UPDATE_CONTAINER -> {
                Container container = this.gson.fromJson(rawArgs, Container.class);
                container.loadShop(this.data.shops);
                this.data.containers.put(this.formatId(container.location), container);
                this.mod.scanner().fullScan();
            }

            case DELETE_CONTAINER -> {
                Vector3i location = this.gson.fromJson(rawArgs, Vector3i.class);
                this.data.containers.remove(this.formatId(location));
                this.mod.scanner().fullScan();
            }

            default -> {
                Mod.LOGGER.warn("Unknown websocket command: {} {} {}", command, syncId, rawArgs);
            }
        }
    }

    /**
     * Double check to make sure we have all the data or default
     */
    private void doubleCheck() {
        if (this.data == null) this.data = new Data(new Vector3i[]{}, new ArrayList<>(), new HashMap<>());
        if (this.data.spawn == null) this.data.spawn = new Vector3i[]{};
        if (this.data.containers == null) this.data.containers = new HashMap<>();
        if (this.data.shops == null) this.data.shops = new ArrayList<>();
    }

    @Override
    public void onClose(int code, String reason, boolean remote) {
        System.out.println("Closed with exit code " + code + " additional info: " + reason);
        this.data = null;
        // Todo (notgeri): ^
    }

    @Override
    public void onError(Exception exception) {
        System.err.println("An error occurred:" + exception);
    }

    /**
     * Send a websocket command
     *
     * @param command The command to send
     * @param data    The data to serialise as JSON along the command
     * @return The future to track this sync ID. Once resolved, it will give the raw string data
     */
    public CompletableFuture<String> command(Command command, Object data) {
        this.syncId++;
        CompletableFuture<String> responseFuture = new CompletableFuture<>();
        this.pendingResponses.put(this.syncId, responseFuture);
        this.send(command.raw + " " + this.syncId + " " + this.gson.toJson(data));
        return responseFuture;
    }

    /**
     * Create or update a specific container
     *
     * @param container The container to update
     * @return The updated container
     */
    public CompletableFuture<Container> saveContainer(Container container) {
        CompletableFuture<Container> future = new CompletableFuture<>();
        CompletableFuture.runAsync(() -> {
            this.command(Command.UPDATE_CONTAINER, container)
                    .orTimeout(10, TimeUnit.SECONDS)
                    .thenAccept(response -> {
                        Container newData = this.gson.fromJson(response, Container.class);
                        newData.loadShop(this.data.shops);
                        this.data.containers.put(this.formatId(newData.location), newData);
                        future.complete(newData);
                    })
                    .exceptionally(e -> {
                        Mod.LOGGER.error("Error or timeout occurred while updating container", e);
                        future.completeExceptionally(e);
                        return null;
                    });
        });

        return future;
    }

    /**
     * Delete a container
     */
    public CompletableFuture<Void> deleteContainer(Container container) {
        CompletableFuture<Void> future = new CompletableFuture<>();
        CompletableFuture.runAsync(() -> {
            this.command(Command.DELETE_CONTAINER, Map.of("location", container.location))
                    .orTimeout(10, TimeUnit.SECONDS)
                    .thenAccept(response -> {
                        this.data.containers.remove(this.formatId(container.location));
                        future.complete(null);
                    })
                    .exceptionally(e -> {
                        Mod.LOGGER.error("Error or timeout occurred while deleting container", e);
                        future.completeExceptionally(e);
                        return null;
                    });
        });
        return future;
    }


    /**
     * @return The list of available cached shops
     */
    public List<Shop> shops() {
        return this.data.shops;
    }

    /**
     * @return The cached corners of spawn
     */
    public Vector3i[] spawn() {
        if (this.data == null) this.doubleCheck();
        return this.data.spawn;
    }

    /**
     * @return The formatted vector ID
     */
    private String formatId(Vector3i location) {
        return this.formatId(location.x, location.y, location.z);
    }

    /**
     * @return The formatted coordinate ID
     */
    private String formatId(int x, int y, int z) {
        return "%s_%s_%s".formatted(x, y, z);
    }

    /**
     * Get a specific container at a position
     *
     * @return The container or null if not found
     */
    public Container getContainer(int x, int y, int z) {
        return this.data.containers.get(this.formatId(x, y, z));
    }

    public static class Data {
        private Vector3i[] spawn;
        private ArrayList<Shop> shops;
        private HashMap<String, Container> containers;

        public Data(Vector3i[] spawn, ArrayList<Shop> shops, HashMap<String, Container> containers) {
            this.containers = containers;
            this.shops = shops;
            this.spawn = spawn;
        }
    }

    public record Shop(
            String id,
            String name,
            List<String> owners,
            Vector3i location
    ) {
        public String name() {
            return Objects.requireNonNullElse(this.name, "unknown shop");
        }
    }

    public enum Per {
        @SerializedName("piece") PIECE,
        @SerializedName("stack") STACK,
        @SerializedName("shulker") SHULKER
    }

    public static final class Container {
        private boolean untracked;

        private String id;
        private String shopId;
        private String icon;
        private Vector3i location;
        private int price;
        private Per per;
        private int amount;
        private int stocked;
        private String customName;
        private long lastChecked;

        private transient Shop shop;

        public boolean untracked() {
            return untracked;
        }

        public Container setUntracked(boolean untracked) {
            this.untracked = untracked;
            return this;
        }

        public String id() {
            return id;
        }

        public Container setId(String id) {
            this.id = id;
            return this;
        }

        public String shopId() {
            return shopId;
        }

        public Container setShopId(String shopId) {
            this.shopId = shopId;
            return this;
        }

        public String icon() {
            return icon;
        }

        public Container setIcon(String icon) {
            this.icon = icon;
            return this;
        }

        public Vector3i location() {
            return location;
        }

        public Container setLocation(Vector3i location) {
            this.location = location;
            return this;
        }

        public Container setLocation(BlockPos pos) {
            return this.setLocation(new Vector3i(pos.getX(), pos.getY(), pos.getZ()));
        }

        public int price() {
            return price;
        }

        public Container setPrice(int price) {
            this.price = price;
            return this;
        }

        public Per per() {
            return per;
        }

        public Container setPer(Per per) {
            this.per = per;
            return this;
        }

        public int amount() {
            return amount;
        }

        public Container setAmount(int amount) {
            this.amount = amount;
            return this;
        }

        public int stocked() {
            return stocked;
        }

        public Container setStocked(int stocked) {
            this.stocked = stocked;
            return this;
        }

        public String customName() {
            return customName;
        }

        public Container setCustomName(String customName) {
            this.customName = customName;
            return this;
        }

        public long lastChecked() {
            return lastChecked;
        }

        public Container setLastChecked(long lastChecked) {
            this.lastChecked = lastChecked;
            return this;
        }

        public Shop shop() {
            return shop;
        }

        public Container setShop(Shop shop) {
            this.shopId = shop.id;
            this.shop = shop;
            return this;
        }

        public void loadShop(ArrayList<Shop> shops) {
            this.shop = shops.stream().filter(s -> Objects.equals(s.id(), this.shopId)).findFirst().orElse(null);
        }

        public boolean recentlyChecked() {
            return false; // Todo (notgeri):
        }

    }

}
