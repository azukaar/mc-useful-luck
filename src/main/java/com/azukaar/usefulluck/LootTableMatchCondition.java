package com.azukaar.usefulluck;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraft.world.level.storage.loot.predicates.LootItemConditionType;

import java.util.regex.Pattern;

public class LootTableMatchCondition implements LootItemCondition {
    public static final MapCodec<LootTableMatchCondition> CODEC = RecordCodecBuilder.mapCodec((builder) -> {
        return builder.group(Codec.STRING.fieldOf("loot_table_id").forGetter((condition) -> {
            return condition.pattern.pattern();
        })).apply(builder, LootTableMatchCondition::new);
    });
    
    private final Pattern pattern;
    // Make this public static so it can be accessed directly
    public static final LootItemConditionType LOOT_TABLE_MATCH_TYPE = new LootItemConditionType(CODEC);

    public LootTableMatchCondition(String patternString) {
        this.pattern = Pattern.compile(patternString);
    }

    @Override
    public LootItemConditionType getType() {
        return LOOT_TABLE_MATCH_TYPE;
    }

    @Override
    public boolean test(LootContext lootContext) {
        ResourceLocation lootTableId = lootContext.getQueriedLootTableId();
        if (lootTableId == null) {
            UsefulLuck.LOGGER.debug("No loot table ID found in context, condition cannot match");
            return false;
        }
        boolean matches = pattern.matcher(lootTableId.toString()).matches();
        UsefulLuck.LOGGER.debug("Testing loot table " + lootTableId + " against pattern " + 
                           pattern.pattern() + ": " + matches);
        return matches;
    }
}
