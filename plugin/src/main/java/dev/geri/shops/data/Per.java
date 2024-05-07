package dev.geri.shops.data;

import com.google.gson.annotations.SerializedName;

public enum Per {
    @SerializedName("piece") PIECE,
    @SerializedName("stack") STACK,
    @SerializedName("shulker") SHULKER;

    /**
     * Format it as a human-readable string
     * based on the number of X
     */
    public String format(int amount) {
        return this.name().toLowerCase() + (amount == 1 ? "" : "s");
    }

}
