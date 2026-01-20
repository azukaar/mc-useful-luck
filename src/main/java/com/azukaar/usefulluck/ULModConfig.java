package com.azukaar.usefulluck;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang3.tuple.Pair;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.common.ModConfigSpec;

public class ULModConfig {
    public static class Server
    {
        public static ModConfigSpec.ConfigValue<List<? extends String>> blacklisted_items;

        Server(ModConfigSpec.Builder builder) {
            blacklisted_items = builder
            .comment("A list of items to blacklist from being distributed by luck.")
            .defineList("blacklisted_items", List.of(
                "minecraft:map",
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

    public static final ModConfigSpec serverSpec;
    public static final Server SERVER;

    static {
        final Pair<Server, ModConfigSpec> specPair = new ModConfigSpec.Builder().configure(Server::new);
        serverSpec = specPair.getRight();
        SERVER = specPair.getLeft();
    }
}