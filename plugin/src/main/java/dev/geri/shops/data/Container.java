package dev.geri.shops.data;

import java.util.UUID;

public class Container {

    private UUID shopId;
    private String item;
    private int amount;
    private Per per;
    private double price;
    private int stock;

    public UUID shopId() {
        return shopId;
    }

    public Container setShopId(UUID shopId) {
        this.shopId = shopId;
        return this;
    }

    public String item() {
        return item;
    }

    public Container setItem(String item) {
        this.item = item;
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

    public double price() {
        return price;
    }

    public Container setPrice(double price) {
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

    public enum Per {
        PIECE,
        STACK,
        SHULKER
    }

}



