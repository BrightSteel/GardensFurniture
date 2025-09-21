package com.gardensmc.gardensfurniture.backpack;

import com.gardensmc.gardensfurniture.GardensFurniture;
import com.gardensmc.gardensfurniture.custom.item.CustomBackpackItem;
import com.gardensmc.gardensfurniture.menu.BackpackGui;
import de.tr7zw.nbtapi.NBT;
import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.ItemContainerContents;
import lombok.AllArgsConstructor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.jetbrains.annotations.Nullable;

import java.util.*;

@AllArgsConstructor
public class BackpackHandler {

    // player uuid -> backpack id
    private static final Map<UUID, UUID> openBackpacks = new HashMap<>();

    private static final String BACKPACK_KEY = "GardensBackpack";
    private static final String BACKPACK_ID_KEY = "Id";
    private static final String BACKPACK_ROWS_KEY = "Rows";

    private final ItemStack itemStack;
    private final CustomBackpackItem backpackItem;

    public static void closeBackpackIfOpen(Player player, ItemStack backpack) {
        var backpackId = NBT.get(backpack, nbt -> {
            var compound = nbt.getCompound(BACKPACK_KEY);
            if (compound != null) {
                return compound.getUUID(BACKPACK_ID_KEY);
            }
            return null;
        });
        if (backpackId == null) {
            return;
        }
        var openBackpack = openBackpacks.get(player.getUniqueId());
        if (openBackpack != null && openBackpack.equals(backpackId) && player.getOpenInventory().getTopInventory().getHolder() instanceof BackpackGui backpackGui) {
            onClose(player, backpack, backpackGui.getItemContents());
            player.getOpenInventory().close();
        }
    }

    public ItemStack getUpgradeRowsResult() {
        var backpackData = getOrCreateBackpackData();
        // check if max met
        if (backpackData.rows == 6) {
            return null;
        }
        var clone = itemStack.clone();
        NBT.modify(clone, nbt -> {
            Objects.requireNonNull(nbt.getCompound(BACKPACK_KEY)).setInteger(BACKPACK_ROWS_KEY, backpackData.rows + 1);
        });
        return clone;
    }

    @Nullable
    public static BackpackHandler create(ItemStack itemStack) {
        var customItem = GardensFurniture.itemRegistry.getCustomItem(itemStack);
        if (customItem instanceof CustomBackpackItem customBackpackItem) {
            return new BackpackHandler(itemStack, customBackpackItem);
        }
        return null;
    }

    public void openBackpack(Player player) {
        var backpackData = getOrCreateBackpackData();
        new BackpackGui(player, BackpackGui.RowConfig.fromRows(backpackData.rows), itemStack, backpackData.items).open();
        openBackpacks.put(player.getUniqueId(), backpackData.id);
    }

    private record BackpackData(UUID id, int rows, ItemStack[] items) {
    }

    private BackpackData getOrCreateBackpackData() {
        var data = itemStack.getData(DataComponentTypes.CONTAINER);
        return NBT.modify(itemStack, nbt -> {
            var compound = nbt.getOrCreateCompound(BACKPACK_KEY);
            if (!compound.hasTag(BACKPACK_ID_KEY)) {
                compound.setUUID(BACKPACK_ID_KEY, UUID.randomUUID());
            }
            if (!compound.hasTag(BACKPACK_ROWS_KEY)) {
                compound.setInteger(BACKPACK_ROWS_KEY, backpackItem.getRows());
            }
            return new BackpackData(compound.getUUID(BACKPACK_ID_KEY), compound.getInteger(BACKPACK_ROWS_KEY), data == null ? new ItemStack[0] : data.contents().toArray(ItemStack[]::new));
        });
    }

    public static boolean isOpenBackpack(PlayerInventory inventory, ItemStack itemStack) {
        if (itemStack == null || itemStack.getType() == Material.AIR) {
            return false;
        }
        var openBackpack = openBackpacks.get(Objects.requireNonNull(inventory.getHolder()).getUniqueId());
        if (openBackpack == null) {
            return false;
        }
        if (!(GardensFurniture.itemRegistry.getCustomItem(itemStack) instanceof CustomBackpackItem)) {
            return false;
        }
        var backpackId = NBT.get(itemStack, nbt -> {
           var compound = nbt.getCompound(BACKPACK_KEY);
           if (compound != null) {
               return compound.getUUID(BACKPACK_ID_KEY);
           }
           return null;
        });
        return Objects.equals(openBackpack, backpackId);
    }

    public static void onClose(Player player, ItemStack backpack, ItemStack[] contents) {
        if (backpack == null || backpack.getType() == Material.AIR) {
            // AIR if closed after dropping item, but fine since we run this prior manually
            return;
        }

        serializeItems(backpack, contents);
        openBackpacks.remove(player.getUniqueId());
    }

    public static UUID serializeItems(ItemStack backpack, ItemStack[] contents) {
        backpack.setData(DataComponentTypes.CONTAINER, ItemContainerContents.containerContents(
                Arrays.stream(contents)
                        .map(item -> item == null ? new ItemStack(Material.AIR) : item)
                        .toList()
        ));
        var backpackId = NBT.modify(backpack, nbt -> {
            var compound = Objects.requireNonNull(nbt.getCompound(BACKPACK_KEY));
            return compound.getUUID(BACKPACK_ID_KEY);
        });

        return backpackId;
    }
}
