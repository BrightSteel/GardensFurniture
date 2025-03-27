package com.gardensmc.gardensfurniture.hook;

import com.gardensmc.gardensfurniture.GardensFurniture;
import me.lojosho.hibiscuscommons.hooks.Hook;
import me.lojosho.hibiscuscommons.hooks.HookFlag;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

public class HookGardensFurniture extends Hook {

    public HookGardensFurniture() {
        super("gardens", HookFlag.ITEM_SUPPORT, HookFlag.LATE_LOAD);
    }

    @Override
    public ItemStack getItem(@NotNull String itemId) {
        return GardensFurniture.itemRegistry.buildItem(itemId);
    }

    @Override
    public String getItemString(@NotNull ItemStack itemStack) {
        if (!itemStack.hasItemMeta()) return null;
        var gardensItem = GardensFurniture.itemRegistry.getCustomItem(itemStack);
        if (gardensItem == null) return null;
        return gardensItem.getIdentifier();
    }
}