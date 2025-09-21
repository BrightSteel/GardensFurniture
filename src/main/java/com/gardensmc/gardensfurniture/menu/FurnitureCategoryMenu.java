package com.gardensmc.gardensfurniture.menu;

import com.gardensmc.gardensfurniture.GardensFurniture;
import com.gardensmc.gardensfurniture.custom.item.CustomItem;
import de.themoep.inventorygui.*;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.Objects;

public class FurnitureCategoryMenu extends Menu {

    private static final String[] layout = new String[]{
            "aaaaaaaaa",
            "aaaaaaaaa",
            "aaaaaaaaa",
            " b     n "
    };

    private final String category;

    public FurnitureCategoryMenu(String category, Player viewer) {
        super(layout, category, viewer);
        this.category = category;
        initElements();
    }


    private void initElements() {
        var group = new GuiElementGroup('a');
        var customItems = GardensFurniture.itemRegistry.getCustomItemsByCategory(category);
        Objects.requireNonNull(customItems)
                .stream()
//                .sorted(Comparator.comparing(CustomItem::getIdentifier))
                .map(this::getGuiElement)
                .forEach(group::addElement);
        gui.addElement(group);

        gui.addElement(getPreviousButton());
        gui.addElement(new GuiPageElement(
                'n',
                new ItemStack(Material.STICK),
                GuiPageElement.PageAction.NEXT,
                "Next")
        );


    }

    private GuiElement getGuiElement(CustomItem customItem) {
        var itemStack = GardensFurniture.itemRegistry.buildItem(customItem);
        return new StaticGuiElement(
                'a',
                itemStack,
                click -> {
                    viewer.getInventory().addItem(itemStack);
                    return true;
                }
        );
    }

    private StaticGuiElement getPreviousButton() {
        return new StaticGuiElement(
                'b',
                new ItemStack(Material.STICK),
                click -> {
                    boolean first = gui.getPageNumber(viewer) == 0;
                    if (first) {
                        InventoryGui.clearHistory(viewer);
                        new FurnitureMenu(viewer).open();
                    } else {
                        gui.setPageNumber(gui.getPageNumber(viewer) - 1);
                    }
                    return true;
                },
                "Back"
        );
    }
}
