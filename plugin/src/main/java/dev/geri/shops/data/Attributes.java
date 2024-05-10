package dev.geri.shops.data;

import com.google.gson.annotations.SerializedName;

import java.util.HashMap;
import java.util.Map;

public class Attributes {

    // A list of enchantments
    public Map<String, Integer> enchantments = new HashMap<>();

    // Flight duration for rockets
    @SerializedName("flight_duration")
    public Integer flightDuration = null;

    // Potion type for arrows and potions
    @SerializedName("potion_type")
    public String potionType = null;

}
