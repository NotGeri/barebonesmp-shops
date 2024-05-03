package dev.geri.shops.data;

import org.bukkit.Location;

import java.util.HashMap;
import java.util.List;

public record Data(
        List<Shop> shops,
        HashMap<Location, Container> containers
) {}
