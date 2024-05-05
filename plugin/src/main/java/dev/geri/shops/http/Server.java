package dev.geri.shops.http;

import com.sun.net.httpserver.HttpServer;
import dev.geri.shops.Shops;
import dev.geri.shops.data.Container;
import dev.geri.shops.data.Data;
import org.bukkit.Location;

import java.io.IOException;
import java.io.OutputStream;
import java.io.Serializable;
import java.net.InetSocketAddress;
import java.util.*;

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

            // Transform containers for the frontend
            ArrayList<Map<String, Serializable>> containers = new ArrayList<>();
            for (Map.Entry<String, Container> entry : data.containers().entrySet()) {
                Container container = entry.getValue();

                Map<String, Serializable> formatted = new LinkedHashMap<>();
                formatted.put("id", container.material() != null ? container.material().getKey().toString() : null);
                formatted.put("shopName", container.shopName());
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
                }

                containers.add(formatted);
            }

            // Return the data
            exchange.sendResponseHeaders(200, 0);
            OutputStream stream = exchange.getResponseBody();
            stream.write(Shops.GSON.toJson(Map.of(
                    "shops", data.shops(),
                    "containers", containers
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
