package com.gardensmc.gardensfurniture.custom.block;

import com.gardensmc.gardensfurniture.GardensFurniture;
import com.gardensmc.gardensfurniture.custom.item.CustomItem;
import com.gardensmc.gardensfurniture.custom.item.CustomPlaceableItem;
import com.gardensmc.gardensfurniture.events.FurniturePlaceEvent;
import com.gardensmc.gardensfurniture.util.ItemFrameUtil;
import de.tr7zw.nbtapi.NBTEntity;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.BlockFace;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class CustomItemFrameBlock extends CustomBlock {

    public CustomItemFrameBlock(ConfigurationSection blockSection) {
        super(blockSection);
    }

    @Override
    public boolean placeBlock(CustomPlaceableItem customItem, Player player, Location location, BlockFace blockFace, boolean persistent) {
        World world = location.getWorld();
        if (world != null) {

            ItemFrame itemFrame = (ItemFrame) location.getWorld().spawnEntity(location, EntityType.ITEM_FRAME);
            itemFrame.setVisible(false);
            itemFrame.setFacingDirection(blockFace, true);
            ItemStack display = GardensFurniture.itemRegistry.buildItem(customItem);
            String custom_name = null;
            ItemMeta itemMeta = display.getItemMeta();
            if (itemMeta != null) {
                custom_name = itemMeta.getDisplayName();
                itemMeta.setDisplayName(null);
                display.setItemMeta(itemMeta);
            }
            itemFrame.setItem(display);
            if (blockFace == BlockFace.UP || blockFace == BlockFace.DOWN) {
                itemFrame.setRotation(ItemFrameUtil.getRotationFromFacing(player));
            }
            if (custom_name != null) {
                NBTEntity nbtEntity = new NBTEntity(itemFrame);
                nbtEntity.getPersistentDataContainer().setString("custom_name", custom_name);
            }

            if (hasBarrier()) {
                location.getBlock().setType(Material.BARRIER);
            }

            Bukkit.getPluginManager().callEvent(new FurniturePlaceEvent(null, customItem, itemFrame));

            return true;
        }
        return false;
    }
}
