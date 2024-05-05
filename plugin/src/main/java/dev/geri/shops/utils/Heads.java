package dev.geri.shops.utils;

import com.destroystokyo.paper.profile.PlayerProfile;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.profile.PlayerTextures;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.UUID;

public class Heads {

    /**
     * Get a textured player head
     *
     * @param url The specific texture URL
     * @return The item stack for the player head
     */
    public static ItemStack getPlayerHeadItem(String url) {
        ItemStack plus = new ItemStack(Material.PLAYER_HEAD);
        if (plus.getItemMeta() instanceof SkullMeta meta) {
            PlayerProfile playerProfile = Bukkit.createProfile(UUID.randomUUID());
            try {
                PlayerTextures textures = playerProfile.getTextures();
                textures.setSkin(new URI("https://textures.minecraft.net/texture/" + url).toURL());
                playerProfile.setTextures(textures);
            } catch (MalformedURLException | URISyntaxException e) {
                return plus;
            }
            meta.setPlayerProfile(playerProfile);
            plus.setItemMeta(meta);
        }
        return plus;
    }

}
