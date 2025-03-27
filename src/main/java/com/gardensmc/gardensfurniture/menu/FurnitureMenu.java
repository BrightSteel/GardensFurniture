package com.gardensmc.gardensfurniture.menu;

import com.gardensmc.gardensfurniture.GardensFurniture;
import com.gardensmc.gardensfurniture.custom.item.CustomItem;
import de.themoep.inventorygui.GuiElement;
import de.themoep.inventorygui.GuiElementGroup;
import de.themoep.inventorygui.InventoryGui;
import de.themoep.inventorygui.StaticGuiElement;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.Comparator;
import java.util.Objects;

public class FurnitureMenu extends Menu {

    private static final String[] layout = new String[]{
            "aaaaaaaaa",
            "aaaaaaaaa",
            "aaaaaaaaa"
    };

    public FurnitureMenu(Player viewer) {
        super(layout, "Furniture", viewer);
    }

    @Override
    protected void initializeElements() {
        var group = new GuiElementGroup('a');
        GardensFurniture.itemRegistry.getItemCategories()
                .stream()
                .map(this::getCategoryElement)
                .forEach(group::addElement);
        this.gui.addElement(group);
    }

    private GuiElement getCategoryElement(String category) {
        return new StaticGuiElement(
                'a',
                getFirstItemInCategory(category),
                click -> {
                    InventoryGui.clearHistory(viewer);
                    new FurnitureCategoryMenu(category, viewer).open();
                    return true;
                },
                category
        );
    }

    private ItemStack getFirstItemInCategory(String category) {
        var customItem = Objects.requireNonNull(GardensFurniture.itemRegistry.getCustomItemsByCategory(category))
                .stream()
                .iterator()
                .next();
        return GardensFurniture.itemRegistry.buildItem(customItem);
    }
}
