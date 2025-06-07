// LuckLootModifier.java
package com.azukaar.usefulluck;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.loot.IGlobalLootModifier;
import net.minecraftforge.common.loot.LootModifier;

public class LuckLootModifier extends LootModifier {
    private final int roll;
    private final int per;
    private final float chance;
    private final int saturation;

    // In Forge 1.20.1, we use Codec instead of MapCodec
    public static final Codec<LuckLootModifier> CODEC = RecordCodecBuilder.create(inst ->
    // LootModifier#codecStart adds the conditions field.
    codecStart(inst).and(inst.group(
            Codec.INT.fieldOf("roll").forGetter(e -> e.roll),
            Codec.INT.fieldOf("per").forGetter(e -> e.per),
            Codec.FLOAT.fieldOf("chance").forGetter(e -> e.chance),
            Codec.INT.fieldOf("saturation").forGetter(e -> e.saturation)
    )).apply(inst, LuckLootModifier::new));

    private static final Random RANDOM = new Random();

    @Override
    public Codec<? extends IGlobalLootModifier> codec() {
        return CODEC;
    }

    // First constructor parameter is the list of conditions. The rest is our extra
    // properties.
    public LuckLootModifier(LootItemCondition[] conditions, int roll, int per, float chance, int saturation) {
        super(conditions);
        this.roll = roll;
        this.per = per;
        this.chance = chance;
        this.saturation = saturation;

        System.out.println("LuckLootModifier created!");
    }

    // This is where the magic happens. Use your extra properties here if needed.
    // Parameters are the existing loot, and the loot context.
    @Override
    protected ObjectArrayList<ItemStack> doApply(ObjectArrayList<ItemStack> generatedLoot, LootContext context) {
        // if player opens chest
        UsefulLuck.LOGGER.debug("LuckLootModifier doApply called!");

        float luckLevel = Math.max(0, Math.max(context.getLuck() - 1,
                (context.getParamOrNull(LootContextParams.THIS_ENTITY) instanceof ServerPlayer player
                        ? player.getLuck() - 1
                        : 0)));

        UsefulLuck.LOGGER.debug("Luck Used: " + luckLevel);

        // Try to get the loottable that was used to generate the loot.
        ResourceLocation lootTableLoc = context.getQueriedLootTableId();
        UsefulLuck.LOGGER.debug("Loot table: " + lootTableLoc);

        // TODO: Position being -1,-1,-1 helps detecting rerolls to not go recursive.
        // Better way to detect rerolls?
        Vec3 pos = context.getParamOrNull(LootContextParams.ORIGIN);
        
        // if player has luck
        if (luckLevel > 0 && lootTableLoc != null 
                && (pos == null || pos.y != -1 || pos.x != -1 || pos.z != -1)) {
            int i = 0;
            
            float maxITemp = (float)Math.floor(luckLevel / per) * roll; 
            float maxI = 0;

            if (chance == 1) {
                maxI = maxITemp;
            } else {
                for (int j = 0; j < maxITemp; j++) {
                    if (RANDOM.nextFloat() < chance) {
                        maxI++;
                    }
                }
            }

            ObjectArrayList<ItemStack> result = new ObjectArrayList<>();

            while (i < maxI) {
                // pick a random item from the loot table
                List<ItemStack> items = unpackLootTable(lootTableLoc, context, maxI);

                if (items != null && !items.isEmpty()) {
                    UsefulLuck.LOGGER.debug("Adding items: " + items.toString());

                    mergeLoot(result, items);
                }

                i++;
            }

            UsefulLuck.LOGGER.debug("generatedLoot: " + result.toString());

            // if size is > saturation, randomly remove unstackable items based on how many
            // duplicates (same DescriptionId) there is in the list
            // This prevent duplicates from being hoarding the space
            // in the inventory
            if (saturation > 0 && result.size() > saturation) {
                // Count duplicates by DescriptionId
                Map<String, List<Integer>> itemIndices = new HashMap<>();

                for (int idx = 0; idx < result.size(); idx++) {
                    ItemStack stack = result.get(idx);
                    if (!stack.isStackable()) {
                        String descId = stack.getItem().getDescriptionId();
                        if (!itemIndices.containsKey(descId)) {
                            itemIndices.put(descId, new ArrayList<>());
                        }
                        itemIndices.get(descId).add(idx);
                    }
                }

                // Calculate removal probabilities based on duplicate count and total items
                List<Integer> itemsToRemove = new ArrayList<>();
                Random random = new Random();

                // Scale base probability based on total items
                // Linear scaling between these points and beyond
                float baseProbability = 0;
                int totalItems = result.size();

                // assuming saturation is 27
                if (totalItems > saturation) {
                    if (totalItems <= saturation * 1.25) {
                        // Scale between 0 and 0.15 for 27-32 items
                        baseProbability = 0.10f;
                    } else if (totalItems <= saturation * 2) {
                        // Scale between 0.15 and 0.30 for 32-50 items
                        baseProbability = 0.20f;
                    } else {
                        // For more than 50 items, cap at 0.45
                        baseProbability = 0.30f;
                    }
                }

                int newTotalItems = totalItems; // Keep one item

                for (Map.Entry<String, List<Integer>> entry : itemIndices.entrySet()) {
                    List<Integer> indices = entry.getValue();
                    if (indices.size() > 1) {
                        // More duplicates = higher chance of removal
                        float removalProbability = baseProbability * (indices.size() - 1);
                        removalProbability = Math.min(removalProbability, 0.95f); // Cap at 95%

                        UsefulLuck.LOGGER.debug("Item " + entry.getKey() + " has " + indices.size() +
                                " duplicates, removal probability: " + removalProbability);


                        // Determine which duplicates to remove (probabilistic)
                        for (int j = 0; j < indices.size() - 1; j++) { // Keep at least one
                            if (random.nextFloat() < removalProbability) {
                                itemsToRemove.add(indices.get(j));
                                newTotalItems--;
                                
                                if (newTotalItems <= saturation) {
                                    break; // Stop if we reach saturation
                                }
                            }
                        }

                        if (newTotalItems <= saturation) {
                            break; // Stop if we reach saturation
                        }
                    }
                }

                // Sort in descending order to avoid index shifting issues when removing
                itemsToRemove.sort(Collections.reverseOrder());

                // Remove the selected items
                for (int idx : itemsToRemove) {
                    result.remove(idx);
                }

                UsefulLuck.LOGGER.debug("After cleanup: " + result.size() + " items, removed "
                        + itemsToRemove.size() + " duplicates");
            }

            // We merge in the original loot to the generated loot after the cleanup
            // to avoid removing the original loot
            mergeLoot(result, generatedLoot);

            // ensure we don't return more items than max item stack for each item
            for (int idx = 0; idx < result.size(); idx++) {
                ItemStack item = result.get(idx);
                if (item.getCount() > item.getMaxStackSize()) {
                    item.setCount(item.getMaxStackSize());
                }
            }
            
            return result;
        }

        // Add your items to generatedLoot here.
        return generatedLoot;
    }

    // Merge the loot into the generated loot.
    static void mergeLoot(ObjectArrayList<ItemStack> destination, List<ItemStack> from) {
        for (ItemStack item : from) {
            if (item != null && !item.isEmpty()) {
                boolean found = false;
                for (int i = 0; i < destination.size(); i++) {
                    ItemStack item2 = destination.get(i);
                    if (areItemsEqual(item, item2) && item2.isStackable()) {
                        item2.grow(item.getCount());
                        found = true;
                        break;
                    } else if (item2.isEmpty()) {
                        destination.set(i, item);
                        found = true;
                        break;
                    }
                }
                if (!found) {
                    destination.add(item);
                }
            }
        }
    }

    // Simplified item comparison for 1.20.1
    static boolean areItemsEqual(ItemStack item1, ItemStack item2) {
        if (item1.isEmpty() || item2.isEmpty()) {
            return false;
        }
        
        // Check if items are the same type
        if (item1.getItem() != item2.getItem()) {
            return false;
        }
        
        // Check if they have the same NBT data
        return ItemStack.isSameItem(item1, item2);
    }

    static List<ItemStack> unpackLootTable(ResourceLocation lootTableLoc, LootContext context, float maxItem) {
        Level level = context.getLevel();

        if (lootTableLoc != null && level != null && level.getServer() != null) {
            LootTable loottable = level.getServer().getLootData().getLootTable(lootTableLoc);

            // In 1.20.1 we use LootParams.Builder directly
            LootParams.Builder lootparams$builder = new LootParams.Builder((ServerLevel) level)
                .withLuck(1.0f)
                .withParameter(LootContextParams.ORIGIN, new Vec3(-1, -1, -1));

            if (context.hasParam(LootContextParams.THIS_ENTITY)) {
                lootparams$builder.withParameter(LootContextParams.THIS_ENTITY,
                        context.getParamOrNull(LootContextParams.THIS_ENTITY));
            }

            if (context.hasParam(LootContextParams.TOOL)) {
                lootparams$builder.withParameter(LootContextParams.TOOL,
                        context.getParamOrNull(LootContextParams.TOOL));
            }

            if (context.hasParam(LootContextParams.BLOCK_ENTITY)) {
                lootparams$builder.withParameter(LootContextParams.BLOCK_ENTITY,
                        context.getParamOrNull(LootContextParams.BLOCK_ENTITY));
            }

            if (context.hasParam(LootContextParams.BLOCK_STATE)) {
                lootparams$builder.withParameter(LootContextParams.BLOCK_STATE,
                        context.getParamOrNull(LootContextParams.BLOCK_STATE));
            }

            DummyContainer block = new DummyContainer();
            loottable.fill(block, lootparams$builder.create(LootContextParamSets.CHEST), 0);

            // pick a random item from the chest
            List<ItemStack> result = new ArrayList<>();
            for (float _mx = maxItem; _mx > 0; _mx--) {
                ItemStack item = block.getRandomItem();
                if (item != null && !item.isEmpty()) {
                    result.add(item);
                }
            }

            return result;
        }

        return List.of();
    }

    static class DummyContainer implements Container {
        List<ItemStack> items = new ArrayList<>();

        @Override
        public void clearContent() {
            items.clear();
        }

        @Override
        public int getContainerSize() {
            return 27;
        }

        @Override
        public ItemStack getItem(int arg0) {
            if (arg0 >= items.size()) {
                return ItemStack.EMPTY;
            }
            return items.get(arg0);
        }

        @Override
        public boolean isEmpty() {
            return items.isEmpty();
        }

        public ItemStack getRandomItem() {
            if (items.isEmpty()) {
                return ItemStack.EMPTY;
            }
            int index = RANDOM.nextInt(items.size());
            ItemStack item = items.get(index);
            items.remove(index);
            return item;
        }

        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("DummyContainer: ");
            sb.append("Size: " + items.size()).append(", ");
            for (ItemStack item : items) {
                sb.append(item.toString()).append(", ");
            }
            return sb.toString();
        }

        @Override
        public ItemStack removeItem(int arg0, int arg1) {
            ItemStack item = items.get(arg0);
            if (item != null) {
                items.remove(arg0);
                return item;
            }
            return ItemStack.EMPTY;
        }

        @Override
        public ItemStack removeItemNoUpdate(int arg0) {
            ItemStack item = items.get(arg0);
            if (item != null) {
                items.remove(arg0);
                return item;
            }
            return ItemStack.EMPTY;
        }

        @Override
        public void setChanged() {
            // nothing
        }

        @Override
        public void setItem(int arg0, ItemStack arg1) {
            if (arg0 >= items.size()) {
                items.add(arg1);
            } else {
                items.set(arg0, arg1);
            }
        }

        @Override
        public boolean stillValid(Player arg0) {
            return true;
        }
    }

}
