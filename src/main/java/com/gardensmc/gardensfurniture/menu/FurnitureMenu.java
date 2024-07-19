package com.gardensmc.gardensfurniture.menu;

import com.gardensmc.gardensfurniture.GardensFurniture;
import de.themoep.inventorygui.GuiElement;
import de.themoep.inventorygui.GuiElementGroup;
import de.themoep.inventorygui.StaticGuiElement;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

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
                new ItemStack(Material.STONE),
                click -> {
                    new FurnitureCategoryMenu(category, viewer).open();
                    return true;
                },
                category
        );
    }
}
