package dev.geri.shops.http;

import com.sun.net.httpserver.HttpServer;
import dev.geri.shops.Shops;
import dev.geri.shops.data.Data;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;

public class Server {

    private HttpServer server;

    /**
     * Initialise and start the HTTP server for serving the data
     * to the frontend and client-side mod
     */
    public void start(Data data, int port) throws IOException {
        this.server = HttpServer.create(new InetSocketAddress(port), 0);
        this.server.createContext("/", exchange -> {
            // Allow CORS
            exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
            exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "GET");
            exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type");

            // We will be returning JSON
            exchange.getResponseHeaders().set("Content-Type", "application/json");

            // Return the data
            exchange.sendResponseHeaders(200, 0);
            OutputStream os = exchange.getResponseBody();
            os.write(Shops.GSON.toJson(data).getBytes());
            os.close();
        });
        this.server.setExecutor(null);
        this.server.start();
    }

    /**
     * Stop the HTTP server
     */
    public void stop() {
        if (this.server != null) this.server.stop(0);
    }

}
