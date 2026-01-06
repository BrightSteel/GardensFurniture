//package com.gardensmc.gardensfurniture.hook;
//
//import com.gardensmc.gardensfurniture.GardensFurniture;
//import org.bukkit.inventory.ItemStack;
//import org.jetbrains.annotations.NotNull;
//import org.jetbrains.annotations.Nullable;
//import su.nightexpress.nightcore.integration.item.adapter.IdentifiableItemAdapter;
//import su.nightexpress.nightcore.integration.item.data.ItemIdData;
//
//public class CratesFurnitureAdapter extends IdentifiableItemAdapter {
//
//    public CratesFurnitureAdapter() {
//        super("GardensFurniture");
//    }
//
//    @Override
//    public @Nullable String getItemId(@NotNull ItemStack itemStack) {
//        var customItem = GardensFurniture.itemRegistry.getCustomItem(itemStack);
//        return customItem == null ? null : customItem.getIdentifier();
//    }
//
//    @Override
//    public @Nullable ItemStack createItem(@NotNull String s) {
//        return GardensFurniture.itemRegistry.buildItem(s);
//    }
//
//    @Override
//    public boolean canHandle(@NotNull ItemStack itemStack) {
//        return GardensFurniture.itemRegistry.getCustomItem(itemStack) != null;
//    }
//
//    @Override
//    public boolean canHandle(@NotNull ItemIdData itemIdData) {
//        return GardensFurniture.itemRegistry.buildItem(itemIdData.getItemId()) != null;
//    }
//}
