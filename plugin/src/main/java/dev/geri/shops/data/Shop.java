package dev.geri.shops.data;

import dev.geri.shops.Shops;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Shop {

    private String name;
    private List<String> owners;
    private String description;

    public Shop() {}

    /**
     * Deep clone a shop
     */
    public Shop(Shop shop) {
        this.name = shop.name;
        this.owners = new ArrayList<>(shop.owners);
        this.description = shop.description;
    }

    /**
     * Create a shop from a book item based on its content
     *
     * @return The shop or null if the book was invalid
     */
    public static Shop fromRaw(ItemStack item) {
        if (item.getType() != Material.WRITABLE_BOOK) return null;
        BookMeta meta = (BookMeta) item.getItemMeta();
        StringBuilder sb = new StringBuilder();
        for (Component page : meta.pages()) {
            sb.append(Shops.MINI_MESSAGE.serialize(page));
        }

        String raw = sb.toString();
        String name = extractValue(raw, "Name");
        List<String> owners = extractValue(raw, "Owners") != null ? Arrays.asList(extractValue(raw, "Owners").split(",\\s*")) : new ArrayList<>();
        String description = extractDescription(raw, "Description");
        return new Shop().setName(name).setOwners(owners).setDescription(description);
    }

    private static String extractValue(String data, String key) {
        Matcher m = Pattern.compile(key + ":\\s*(.*?)\\n", Pattern.DOTALL).matcher(data);
        return m.find() ? m.group(1) : null;
    }

    private static String extractDescription(String data, String key) {
        Matcher m = Pattern.compile(key + ":\\s*(.*?)($|\\n\\w+:)", Pattern.DOTALL).matcher(data);
        return m.find() ? m.group(1) : null;
    }

    public String name() {
        return name;
    }

    public Shop setName(String name) {
        this.name = name;
        return this;
    }

    public List<String> owners() {
        return owners;
    }

    public Shop setOwners(List<String> owners) {
        this.owners = owners;
        return this;
    }

    public String description() {
        return description;
    }

    public Shop setDescription(String description) {
        this.description = description;
        return this;
    }

}
