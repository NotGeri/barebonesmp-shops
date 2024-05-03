package dev.geri.shops;

import com.google.gson.Gson;
import dev.geri.shops.data.Data;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public final class Shops extends JavaPlugin implements Listener {

    private final Gson gson = new Gson();
    private Data data = null;
    private final Path dataPath = Paths.get(this.getDataFolder().getAbsolutePath(), "data.json");

    @Override
    public void onEnable() {
        // Ensure the data directory is created
        this.getDataFolder().mkdirs();

        // Read the existing data
        try {
            if (new File(dataPath.toAbsolutePath().toString()).exists()) {
                this.data = this.gson.fromJson(new String(Files.readAllBytes(dataPath)), Data.class);
            }
            if (this.data == null) this.data = new Data(new ArrayList<>(), new ArrayList<>());
        } catch (Exception exception) {
            this.getLogger().severe("Unable to read data file, disabling plugin!");
            throw new RuntimeException(exception);
        }
    }

    @Override
    public void onDisable() {
        this.getLogger().info("Saving data..");
        try {
            this.save();
            this.getLogger().info("Data saved!");
        } catch (IOException exception) {
            this.getLogger().severe("Unable to save data!");
            throw new RuntimeException(exception);
        }
    }

    public void save() throws IOException {
        Files.write(this.dataPath, List.of(this.gson.toJson(this.data)), StandardCharsets.UTF_8);
    }


}
