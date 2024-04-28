package dev.geri.tracker.utils;

import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;
import dev.geri.tracker.Mod;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.util.math.BlockPos;
import okhttp3.*;
import org.jetbrains.annotations.NotNull;
import org.joml.Vector3i;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Api {

    private final String BASE_URL = "http://127.0.0.1:8000/api";

    private final OkHttpClient client = new OkHttpClient.Builder().addInterceptor(new Interceptor() {
        @NotNull
        @Override
        public Response intercept(@NotNull Interceptor.Chain chain) throws IOException {
            Request original = chain.request();
            Request.Builder builder = original.newBuilder()
                    .header("Content-Type", "application/json")
                    .header("Accept", "application/json");

            ClientPlayerEntity player = MinecraftClient.getInstance().player;
            if (player != null) builder.header("Player", player.getUuidAsString());

            return chain.proceed(builder.method(original.method(), original.body()).build());
        }
    }).build();

    private final Gson gson = new Gson();
    public final ExecutorService executor = Executors.newFixedThreadPool(2); // Todo (notgeri):

    private Data data = null;

    /**
     * Run the initial API call which will collect
     * all the current data from the API to be cached
     * @throws IOException
     */
    public void init() throws IOException {

        // Do the initial API call
        Request request = new Request.Builder().url(BASE_URL).build();
        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) throw new IOException("Unexpected API response " + response);

            // Parse the response
            this.data = this.gson.fromJson(response.body().string(), Data.class);

            // Double check to make sure we have all the data or default
            if (this.data == null) this.data = new Data(new Vector3i[]{}, new ArrayList<>(), new HashMap<>());
            if (this.data.spawn == null) this.data.spawn = new Vector3i[]{};
            if (this.data.containers == null) this.data.containers = new HashMap<>();
            if (this.data.shops == null) this.data.shops = new ArrayList<>();

            // Add a reference to the shop
            for (Container container : this.data.containers.values()) {
                container.loadShop(this.data.shops);
            }
        }
    }

    /**
     * Create or update a specific container
     * @param container The container to update
     * @return The updated container
     */
    public Container saveContainer(Container container) {
        try (Response response = client.newCall(new Request.Builder().url(BASE_URL + "/containers")
                .post(RequestBody.create(gson.toJson(container).getBytes()))
                .build()
        ).execute()) {

            Container newData = this.gson.fromJson(response.body().string(), Container.class);
            newData.loadShop(this.data.shops);
            this.data.containers.put(this.formatId(newData.location), newData);
            return newData;

        } catch (IOException exception) {
            Mod.LOGGER.error("unable to save container", exception);
            return null;
        }
    }

    /**
     * Delete a container
     */
    public void deleteContainer(Container container) {
        this.executor.submit(() -> {
            try (Response response = client.newCall(new Request.Builder().url(BASE_URL + "/containers")
                    .delete(RequestBody.create(gson.toJson(container).getBytes()))
                    .build()
            ).execute()) {
                this.data.containers.remove(this.formatId(container.location));
            } catch (IOException exception) {
                Mod.LOGGER.error("unable to delete container", exception);
            }
        });
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
