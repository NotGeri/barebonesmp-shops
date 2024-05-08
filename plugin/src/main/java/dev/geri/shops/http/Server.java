package dev.geri.shops.http;

import com.sun.net.httpserver.HttpServer;
import dev.geri.shops.Shops;
import dev.geri.shops.data.Container;
import dev.geri.shops.data.Data;
import dev.geri.shops.data.Shop;
import org.bukkit.Location;

import java.io.IOException;
import java.io.OutputStream;
import java.io.Serializable;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class Server {

    private HttpServer server;

    /**
     * Initialise and start the HTTP server for serving the data
     * to the frontend and client-side mod
     */
    public void start(Shops plugin, int port) throws IOException {
        Data data = plugin.data();

        this.server = HttpServer.create(new InetSocketAddress(port), 0);
        this.server.createContext("/", exchange -> {
            // Allow CORS
            exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
            exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "GET");
            exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type");

            // We will be returning JSON
            exchange.getResponseHeaders().set("Content-Type", "application/json");

            // Assuming Shops class has a way to get all shops with their details
            Map<String, List<Map<String, Serializable>>> shopContainers = new LinkedHashMap<>();
            List<Map<String, Serializable>> danglingContainers = new ArrayList<>();

            for (Map.Entry<String, Container> entry : data.containers().entrySet()) {
                Container container = entry.getValue();
                if (container.material() == null) continue; // Skip containers without material

                Map<String, Serializable> formatted = new LinkedHashMap<>();
                formatted.put("id", container.material().getKey().toString());
                formatted.put("customName", container.customName());
                formatted.put("amount", container.amount());
                formatted.put("per", container.per());
                formatted.put("price", container.price());
                formatted.put("stock", container.stock());

                Location location = data.parseLocation(entry.getKey());
                if (location != null) {
                    formatted.put("x", location.x());
                    formatted.put("y", location.y());
                    formatted.put("z", location.z());
                    formatted.put("world", location.getWorld() != null ? location.getWorld().getName() : null);
                }

                String shopName = container.shopName();
                if (shopName == null || shopName.isEmpty()) {
                    danglingContainers.add(formatted);
                } else {
                    shopContainers.computeIfAbsent(shopName, k -> new ArrayList<>()).add(formatted);
                }
            }

            List<Map<String, Object>> shops = new ArrayList<>();
            shopContainers.forEach((name, containers) -> {
                Shop shop = data.getShop(name);
                Map<String, Object> rawShop = new LinkedHashMap<>();
                rawShop.put("name", shop.name());
                rawShop.put("owners", shop.owners());
                rawShop.put("description", shop.description());
                rawShop.put("containers", containers);
                shops.add(rawShop);
            });

            // Return the data
            exchange.sendResponseHeaders(200, 0);
            OutputStream stream = exchange.getResponseBody();
            stream.write(Shops.GSON.toJson(Map.of(
                    "shops", shops,
                    "dangling_containers", danglingContainers
            )).getBytes());
            stream.close();
        });

        this.server.setExecutor(null);
        this.server.start();
        plugin.getLogger().info("Started HTTP API on port " + port);
    }

    /**
     * Stop the HTTP server
     */
    public void stop() {
        if (this.server != null) this.server.stop(0);
    }

}
