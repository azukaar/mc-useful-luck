package com.azukaar.usefulluck;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.Serializer;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraft.world.level.storage.loot.predicates.LootItemConditionType;

import java.util.regex.Pattern;

public class LootTableMatchCondition implements LootItemCondition {
    private final Pattern pattern;
    public static final LootItemConditionType LOOT_TABLE_MATCH_TYPE = new LootItemConditionType(new LootTableMatchCondition.Serializer());

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

    // In Forge 1.20.1, we need to implement a proper Serializer
    public static class Serializer implements net.minecraft.world.level.storage.loot.Serializer<LootTableMatchCondition> {
        @Override
        public void serialize(JsonObject json, LootTableMatchCondition condition, JsonSerializationContext context) {
            json.addProperty("loot_table_id", condition.pattern.pattern());
        }

        @Override
        public LootTableMatchCondition deserialize(JsonObject json, JsonDeserializationContext context) {
            String pattern = GsonHelper.getAsString(json, "loot_table_id");
            return new LootTableMatchCondition(pattern);
        }
    }
}
