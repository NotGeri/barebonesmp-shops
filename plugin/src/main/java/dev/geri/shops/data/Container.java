package dev.geri.shops.data;

import com.google.gson.annotations.SerializedName;
import dev.geri.shops.utils.Strings;
import org.bukkit.Material;

import java.util.List;

public class Container {

    private String shopName;
    private transient Shop shop;

    private Material material;
    private String customName;
    private int amount;
    private Per per;
    private int price;
    private int stock;
    private Attributes attributes;

    public Container() {}

    /**
     * Deep clone a container
     */
    public Container(Container container) {
        this.shopName = container.shopName;
        this.shop = container.shop != null ? new Shop(container.shop) : null;
        this.material = container.material;
        this.customName = container.customName;
        this.amount = container.amount;
        this.per = container.per;
        this.price = container.price;
        this.stock = container.stock;
        this.attributes = container.attributes;
    }

    /**
     * @return The formatted name of what the container sells
     */
    public String formattedName() {
        if (this.material == null) return "?";
        if (this.customName == null || this.customName.isEmpty()) return Strings.capitalise(this.material.name().toLowerCase().replace("_", " "));
        return this.customName;
    }


    public String shopName() {
        return shopName;
    }

    public Shop shop() {
        return shop;
    }

    public Container setShop(Shop shop) {
        this.shop = shop;
        if (shop != null) this.shopName = shop.name();
        return this;
    }

    public Material material() {
        return material;
    }

    public Container setMaterial(Material material) {
        this.material = material;
        return this;
    }

    public String customName() {
        return customName;
    }

    public Container setCustomName(String customName) {
        this.customName = customName;
        return this;
    }

    public int amount() {
        return amount;
    }

    public Container setAmount(int amount) {
        this.amount = amount;
        return this;
    }

    public Per per() {
        return per;
    }

    public Container setPer(Per per) {
        this.per = per;
        return this;
    }

    public int price() {
        return price;
    }

    public Container setPrice(int price) {
        this.price = price;
        return this;
    }

    public int stock() {
        return stock;
    }

    public Container setStock(int stock) {
        this.stock = stock;
        return this;
    }

    public Attributes attributes() {
        return attributes;
    }

    public Container setAttributes(Attributes attributes) {
        this.attributes = attributes;
        return this;
    }

}



