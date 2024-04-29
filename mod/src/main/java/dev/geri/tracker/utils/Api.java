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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Api extends WebSocketClient {

    private final Gson gson = new Gson();
    private final ExecutorService executor = Executors.newFixedThreadPool(2);

    private Data data = null;

    public Api(URI uri) {
        super(uri);
    }

    @Override
    public void onOpen(ServerHandshake data) {
        this.doubleCheck();
        if (MinecraftClient.getInstance().player == null) return;
        this.send("auth", Map.of("uuid", MinecraftClient.getInstance().player.getUuid()));
    }

    @Override
    public void onMessage(String message) {
        String[] parts = message.split(" ", 2);

        String command = parts.length > 0 ? parts[0] : "";
        String rawArgs = parts.length > 1 ? parts[1] : null;

        switch (command) {
            case "authenticated" -> this.authenticated(rawArgs);
            default -> {
                Mod.LOGGER.warn("Unknown websocket command: {} {}", command, rawArgs);
            }
        }
    }

    public void authenticated(String rawArgs) {
        // Parse the response
        this.data = this.gson.fromJson(rawArgs, Data.class);
        this.doubleCheck();

        // Add a reference to the shop
        for (Container container : this.data.containers.values()) {
            container.loadShop(this.data.shops);
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
    }

    @Override
    public void onError(Exception exception) {
        System.err.println("An error occurred:" + exception);
    }

    public void send(String message, Object data) {
        this.send(message + " " + this.gson.toJson(data));
    }

    @Override
    public void close() {
        this.data = null;
        super.close();
    }

    /**
     * Create or update a specific container
     *
     * @param container The container to update
     * @return The updated container
     */
    public CompletableFuture<Container> saveContainer(Container container) {
        return CompletableFuture.supplyAsync(() -> {
/*            try (Response response = client.newCall(new Request.Builder().url(BASE_URL + "/containers")
                    .post(RequestBody.create(gson.toJson(container).getBytes()))
                    .build()
            ).execute()) {

                Container newData = this.gson.fromJson(response.body().string(), Container.class);
                newData.loadShop(this.data.shops);
                this.data.containers.put(this.formatId(newData.location), newData);
                return newData;

            } catch (IOException exception) {
                throw new RuntimeException("Container save failed", exception);
            }*/
            return null;
        });
    }

    // Currently doing: set these up with the websocket ()

    /**
     * Delete a container
     */
    public CompletableFuture<Void> deleteContainer(Container container) {
        return CompletableFuture.runAsync(() -> {
     /*       try (Response response = client.newCall(new Request.Builder().url(BASE_URL + "/containers")
                    .delete(RequestBody.create(gson.toJson(container).getBytes()))
                    .build()
            ).execute()) {
                this.data.containers.remove(this.formatId(container.location));
            } catch (IOException exception) {
                throw new RuntimeException("Unable to delete container", exception);
            }*/
        }, this.executor);
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
    ) {}

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
