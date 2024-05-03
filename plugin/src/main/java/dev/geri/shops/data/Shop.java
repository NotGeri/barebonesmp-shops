package dev.geri.shops.data;

import java.util.List;
import java.util.UUID;

public class Shop {

    private UUID id;
    private String name;
    private List<UUID> owners;

    public UUID id() {
        return id;
    }

    public Shop setId(UUID id) {
        this.id = id;
        return this;
    }

    public String name() {
        return name;
    }

    public Shop setName(String name) {
        this.name = name;
        return this;
    }

    public List<UUID> owners() {
        return owners;
    }

    public Shop setOwners(List<UUID> owners) {
        this.owners = owners;
        return this;
    }

}
