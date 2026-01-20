package com.azukaar.usefulluck;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang3.tuple.Pair;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraftforge.common.ForgeConfigSpec;

public class ULModConfig {
    public static class Server
    {
        public static ForgeConfigSpec.ConfigValue<List<? extends String>> blacklisted_items;

        Server(ForgeConfigSpec.Builder builder) {
            blacklisted_items = builder
            .comment("A list of items to blacklist from being distributed by luck.")
            .defineList("blacklisted_items", List.of(
                "map",
                "ars_additions:exploration_warp_scroll",
                "ars_nouveau:exploration_scroll",
                "the_bumblezone:honey_compass"
            ), itemName -> {
                try {
                    ResourceLocation.parse(itemName.toString());
                    return true;
                } catch (Exception e) {
                    return false;
                }
            });
        }

        public Set<String> getBlacklistedItems() {
            return blacklisted_items.get().stream()
                    .map(itemName -> itemName.toString().toLowerCase())
                    .collect(Collectors.toSet());
        }

        public boolean isBlacklisted(Item item) {
            return isBlacklisted(item.toString());
        }

        public boolean isBlacklisted(String itemName) {
            return getBlacklistedItems().contains(itemName.toLowerCase());
        }
    }

    public static final ForgeConfigSpec serverSpec;
    public static final Server SERVER;

    static {
        final Pair<Server, ForgeConfigSpec> specPair = new ForgeConfigSpec.Builder().configure(Server::new);
        serverSpec = specPair.getRight();
        SERVER = specPair.getLeft();
    }
}