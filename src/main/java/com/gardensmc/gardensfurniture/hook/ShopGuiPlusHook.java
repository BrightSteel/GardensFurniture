package com.gardensmc.gardensfurniture.hook;

import com.gardensmc.gardensfurniture.GardensFurniture;
import net.brcdev.shopgui.ShopGuiPlusApi;
import net.brcdev.shopgui.event.ShopGUIPlusPostEnableEvent;
import net.brcdev.shopgui.provider.item.ItemProvider;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;

import java.util.Objects;

public class ShopGuiPlusHook implements Listener {

    @EventHandler
    public void onShopGUIPlusPostEnable(ShopGUIPlusPostEnableEvent event) {
        ShopGuiPlusApi.registerItemProvider(new ItemProvider("gardensfurniture") {
            @Override
            public boolean isValidItem(ItemStack itemStack) {
                return GardensFurniture.itemRegistry.getCustomItem(itemStack) != null;
            }

            @Override
            public ItemStack loadItem(ConfigurationSection configurationSection) {
                var id = configurationSection.getString("gardensfurniture");
                if (id == null) {
                    return null;
                }
                return GardensFurniture.itemRegistry.buildItem(id);
            }

            @Override
            public boolean compare(ItemStack itemStack, ItemStack itemStack1) {
                return Objects.equals(
                        GardensFurniture.itemRegistry.getCustomItem(itemStack),
                        GardensFurniture.itemRegistry.getCustomItem(itemStack1)
                );
            }
        });
    }
}
