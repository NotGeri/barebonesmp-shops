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
