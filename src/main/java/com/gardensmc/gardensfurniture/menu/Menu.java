package com.gardensmc.gardensfurniture.menu;

import com.gardensmc.gardensfurniture.GardensFurniture;
import de.themoep.inventorygui.InventoryGui;
import org.bukkit.entity.Player;

public class Menu {

    protected String[] layout;
    protected Player viewer;

    protected InventoryGui gui;

    public Menu(String[] layout, String title, Player viewer) {
        this.viewer = viewer;
        this.gui = new InventoryGui(GardensFurniture.plugin, viewer, title, layout);
        initializeElements();
    }

    protected void initializeElements() {

    }

    public void open() { gui.show(viewer); }
}
