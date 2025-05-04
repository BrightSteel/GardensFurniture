package com.gardensmc.gardensfurniture.menu;

import com.gardensmc.gardensfurniture.GardensFurniture;
import de.themoep.inventorygui.*;
import org.apache.commons.lang3.StringUtils;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.Objects;

public class FurnitureMenu extends Menu {

    private static final String[] layout = new String[]{
            "aaaaaaaaa",
            "aaaaaaaaa",
            "aaaaaaaaa",
            " b     n "
    };

    public FurnitureMenu(Player viewer) {
        super(layout, "Furniture", viewer);
    }

    @Override
    protected void initializeElements() {
        var group = new GuiElementGroup('a');
        GardensFurniture.itemRegistry.getItemCategories()
                .stream()
                .sorted()
                .map(this::getCategoryElement)
                .forEach(group::addElement);
        this.gui.addElement(group);
        gui.addElement(new GuiPageElement(
                'n',
                new ItemStack(Material.STICK),
                GuiPageElement.PageAction.NEXT,
                "Next")
        );
        gui.addElement(new GuiPageElement(
                'b',
                new ItemStack(Material.STICK),
                GuiPageElement.PageAction.PREVIOUS,
                "Back")
        );
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
                StringUtils.capitalize(category)
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
