package com.azukaar.usefulluck.mixins;

import net.minecraft.core.Holder;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import com.azukaar.usefulluck.ULModConfig;
import com.azukaar.usefulluck.UsefulLuck;

import java.util.function.Consumer;

@Mixin(LootItem.class)
public class LootItemMixin {
    
    @Shadow
    private Holder<Item> item;
    
    @Inject(method = "createItemStack", at = @At("HEAD"), cancellable = true)
    private void preventBlacklistedItems(Consumer<ItemStack> stackConsumer, LootContext lootContext, CallbackInfo ci) {
        // Only filter when called from our mod (detected by the -1,-1,-1 origin marker)
        Vec3 origin = lootContext.getParamOrNull(LootContextParams.ORIGIN);
        if (origin != null && origin.x == -1 && origin.y == -1 && origin.z == -1) {
            if (ULModConfig.SERVER.isBlacklisted(this.item.value())) {
                // Cancel the method entirely - this prevents filled_map generation
                UsefulLuck.LOGGER.debug("Prevented blacklisted item: {}", this.item.value().getDescriptionId());
                ci.cancel();
                return;
            }
        }
    }
}