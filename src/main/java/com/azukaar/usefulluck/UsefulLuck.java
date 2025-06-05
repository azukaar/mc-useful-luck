package com.azukaar.usefulluck;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;
import com.mojang.serialization.MapCodec;

import net.minecraft.core.registries.Registries;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.loot.IGlobalLootModifier;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import net.minecraft.world.level.storage.loot.predicates.LootItemConditionType;
import java.util.function.Supplier;

// The value here should match an entry in the META-INF/neoforge.mods.toml file
@Mod(UsefulLuck.MODID)
public class UsefulLuck
{
    // Define mod id in a common place for everything to reference
    public static final String MODID = "usefulluck";
    // Directly reference a slf4j logger
    public static final Logger LOGGER = LogUtils.getLogger();

    public static final DeferredRegister<MapCodec<? extends IGlobalLootModifier>> GLOBAL_LOOT_MODIFIER_SERIALIZERS =
        DeferredRegister.create(NeoForgeRegistries.Keys.GLOBAL_LOOT_MODIFIER_SERIALIZERS, MODID);

    public static final Supplier<MapCodec<LuckLootModifier>> LUCK_MODIFIER =
            GLOBAL_LOOT_MODIFIER_SERIALIZERS.register("luck_modifier", () -> LuckLootModifier.CODEC);    

    // Add this field with your other DeferredRegisters
    public static final DeferredRegister<LootItemConditionType> LOOT_CONDITIONS = 
        DeferredRegister.create(Registries.LOOT_CONDITION_TYPE, MODID);

    // Fix the registration to use a proper supplier
    public static final Supplier<LootItemConditionType> LOOT_TABLE_MATCH = 
        LOOT_CONDITIONS.register("loot_table_match", () -> LootTableMatchCondition.LOOT_TABLE_MATCH_TYPE);

    // The constructor for the mod class is the first code that is run when your mod is loaded.
    // FML will recognize some parameter types like IEventBus or ModContainer and pass them in automatically.
    public UsefulLuck(IEventBus modEventBus, ModContainer modContainer)
    {
        // Register the Deferred Register to the mod event bus so global loot modifiers get registered
        GLOBAL_LOOT_MODIFIER_SERIALIZERS.register(modEventBus);

        // Register the loot conditions
        LOOT_CONDITIONS.register(modEventBus);
        
        modContainer.registerConfig(ModConfig.Type.SERVER, ULModConfig.serverSpec);

        // Register ourselves for server and other game events we are interested in.
        // Note that this is necessary if and only if we want *this* class (UsefulLuck) to respond directly to events.
        // Do not add this line if there are no @SubscribeEvent-annotated functions in this class, like onServerStarting() below.
        NeoForge.EVENT_BUS.register(this);
    }

    // You can use SubscribeEvent and let the Event Bus discover methods to call
    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event)
    {
        // Do something when the server starts
        LOGGER.info("UsefulLuck Mod Loading!");
    }
}
