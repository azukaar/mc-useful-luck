package com.azukaar.usefulluck;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.data.PackOutput;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.neoforged.neoforge.common.data.GlobalLootModifierProvider;

public class MyGlobalLootModifierProvider extends GlobalLootModifierProvider {
    public MyGlobalLootModifierProvider(PackOutput output, CompletableFuture<Provider> registries) {
    super(output, registries, UsefulLuck.MODID);
    }
  

    @Override
    protected void start() {
        // Call #add to add a new GLM. This also adds a corresponding entry in global_loot_modifiers.json.
        add(
                // The name of the modifier. This will be the file name.
                "luck_modifier",

                // The loot modifier to add. For the sake of example, we add a weather loot condition.
                new LuckLootModifier(new LootItemCondition[] {}, 1, 1,  1, 27),

                // A list of data load conditions. Note that these are unrelated to the loot conditions
                // specified on the modifier itself. For the sake of example, we add a mod loaded condition.
                // An overload of #add is available that accepts a vararg of conditions instead of a list.
                List.of()
        );
    }
}
