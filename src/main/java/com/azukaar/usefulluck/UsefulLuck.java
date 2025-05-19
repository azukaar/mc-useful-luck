package com.azukaar.usefulluck;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;

import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.loot.IGlobalLootModifier;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import net.minecraft.world.level.storage.loot.predicates.LootItemConditionType;
import java.util.function.Supplier;
import net.minecraft.core.registries.Registries;

// The value here should match an entry in the META-INF/mods.toml file
@Mod(UsefulLuck.MODID)
public class UsefulLuck
{
    // Define mod id in a common place for everything to reference
    public static final String MODID = "usefulluck";
    // Directly reference a slf4j logger
    public static final Logger LOGGER = LogUtils.getLogger();

    // In Forge 1.20.1, we use Codec instead of MapCodec
    public static final DeferredRegister<Codec<? extends IGlobalLootModifier>> GLOBAL_LOOT_MODIFIER_SERIALIZERS =
        DeferredRegister.create(ForgeRegistries.Keys.GLOBAL_LOOT_MODIFIER_SERIALIZERS, MODID);

    public static final Supplier<Codec<LuckLootModifier>> LUCK_MODIFIER =
            GLOBAL_LOOT_MODIFIER_SERIALIZERS.register("luck_modifier", () -> LuckLootModifier.CODEC);    

    // In Forge 1.20.1, we use the Registry directly
    public static final DeferredRegister<LootItemConditionType> LOOT_CONDITIONS = 
        DeferredRegister.create(Registries.LOOT_CONDITION_TYPE, MODID);

    // Fix the registration to use a proper supplier
    public static final Supplier<LootItemConditionType> LOOT_TABLE_MATCH = 
        LOOT_CONDITIONS.register("loot_table_match", () -> new LootItemConditionType(new LootTableMatchCondition.Serializer()));

    // The constructor for the mod class is the first code that is run when your mod is loaded.
    public UsefulLuck()
    {
        // Get the event bus
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
        
        // Register the Deferred Register to the mod event bus so global loot modifiers get registered
        GLOBAL_LOOT_MODIFIER_SERIALIZERS.register(modEventBus);

        // Register the loot conditions
        LOOT_CONDITIONS.register(modEventBus);

        // Register ourselves for server and other game events we are interested in.
        MinecraftForge.EVENT_BUS.register(this);
    }

    // You can use SubscribeEvent and let the Event Bus discover methods to call
    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event)
    {
        // Do something when the server starts
        LOGGER.info("UsefulLuck Mod Loading!");
    }
}
