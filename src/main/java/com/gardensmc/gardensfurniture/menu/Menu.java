package com.gardensmc.gardensfurniture.menu;

import com.gardensmc.gardensfurniture.GardensFurniture;
import de.themoep.inventorygui.InventoryGui;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.entity.Player;

public class Menu {

    protected Player viewer;

    protected InventoryGui gui;

    public Menu(String[] layout, String title, Player viewer) {
        this(layout, MiniMessage.miniMessage().deserialize(title), viewer);
    }

    public Menu(String[] layout, Component title, Player viewer) {
        this.viewer = viewer;
        this.gui = new InventoryGui(
                GardensFurniture.plugin,
                new InventoryGui.InventoryCreator(
                        (gui, who, type) -> GardensFurniture.plugin.getServer().createInventory(new InventoryGui.Holder(gui), type, title),
                        (gui, who, size) -> GardensFurniture.plugin.getServer().createInventory(new InventoryGui.Holder(gui), size, title)
                ),
                (meta, s) -> meta.displayName(toComponent(s)),
                (meta, strings) -> meta.lore(strings.stream().map(this::toComponent).toList()),
                viewer,
                "",
                layout
        );
        initializeElements();
    }

    private Component toComponent(String s) {
        return MiniMessage.miniMessage().deserialize("<!italic><white>" + s + "</white>");
    }

    protected void initializeElements() {
    }

    public void open() { gui.show(viewer); }
}
