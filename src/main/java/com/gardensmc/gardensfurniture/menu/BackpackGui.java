package com.gardensmc.gardensfurniture.menu;

import com.gardensmc.gardensfurniture.GardensFurniture;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.stream.IntStream;

public class BackpackGui implements InventoryHolder {

    private final Player viewer;
    @Getter
    private final ItemStack backpackItem;
    private final Inventory inventory;
    private final ItemStack blankHiddenItem = GardensFurniture.itemRegistry.buildItem("blank_hidden");
    @Getter
    private final int fillerSlots;

    public BackpackGui(Player viewer, BackpackGui.RowConfig rowConfig, ItemStack backpackItem, ItemStack[] items) {
        this.viewer = viewer;
        this.fillerSlots = rowConfig.getFillerRows() * 9;
        this.backpackItem = backpackItem;
        this.inventory = Bukkit.createInventory(this, rowConfig.getTotalRows() * 9, getTitle(rowConfig));
        addEmptySlots();
        if (items != null) {
            for (int i = 0; i < items.length; i++) {
                inventory.setItem(fillerSlots + i, items[i]);
            }
        }
    }

    private static Component getTitle(BackpackGui.RowConfig rowConfig) {
        return MiniMessage.miniMessage().deserialize("<white><font:gardens:default>\u0105\u010B" + rowConfig.getSymbol() + "</font>");
    }

    private void addEmptySlots() {
        IntStream.range(0, fillerSlots).forEach(i -> inventory.setItem(i, blankHiddenItem));
    }

    public void open() {
        viewer.openInventory(getInventory());
    }

    public ItemStack[] getItemContents() {
        // remove filler content
        var originalContents = inventory.getContents();
        if (fillerSlots == 0) {
            return originalContents;
        }
        return Arrays.copyOfRange(originalContents, fillerSlots, originalContents.length);
    }

    @Override
    public @NotNull Inventory getInventory() {
        return inventory;
    }

    @RequiredArgsConstructor
    @AllArgsConstructor
    @Getter
    public enum RowConfig {
        TIER1(1, '\u0130', 5),
        TIER2(2, '\u0131', 4),
        TIER3(3, '\u0132', 3),
        TIER4(4, '\u0133', 2),
        TIER5(5, '\u0134', 1),
        TIER6(6, '\u0135');

        private final int rows;
        private final char symbol;
        private int fillerRows;

        public int getTotalRows() {
            return rows + fillerRows;
        }

        public static BackpackGui.RowConfig fromRows(int rows) {
            return Arrays.stream(BackpackGui.RowConfig.values()).
                    filter(rowConfig -> rowConfig.rows == rows)
                    .findFirst()
                    .orElseThrow();
        }
    }
}
