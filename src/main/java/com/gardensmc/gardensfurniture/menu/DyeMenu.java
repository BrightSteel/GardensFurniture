package com.gardensmc.gardensfurniture.menu;

import com.gardensmc.gardensfurniture.GardensFurniture;
import com.gardensmc.gardensfurniture.dye.DyeColorGroup;
import com.gardensmc.gardensfurniture.util.ItemUtil;
import de.themoep.inventorygui.*;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.LeatherArmorMeta;

import java.util.stream.IntStream;

public class DyeMenu extends Menu {

    private static final ItemStack DYE_TILE = GardensFurniture.itemRegistry.buildItem("dye_tile");

    private static final String[] LAYOUT = new String[] {
            "xxxxxxxxx",
            "xxxaaaxxx",
            "xd aaa h ",
            "   aaa   ",
            "         ",
            " lllllll "
    };

    private final Inventory storage;
    private DyeColorGroup currDyeGroup;
    private GuiStorageElement input;

    public DyeMenu(Player viewer) {
        super(LAYOUT, "ꐤꐤ<white>\uE12A", viewer);
        this.storage = Bukkit.createInventory(viewer, 9);
        initElements();
    }

    private void initElements() {
        gui.addElement(input = new GuiStorageElement('d', storage, 0, this::onApplyItem, validatorInfo -> {
            var item = validatorInfo.getItem();
            return ItemUtil.isEmpty(item) || item.getItemMeta() instanceof LeatherArmorMeta;
        }));
        gui.addElement(new GuiStorageElement('h', storage, 1, this::onClickResult, validatorInfo -> ItemUtil.isEmpty(validatorInfo.getItem())));

        var baseColorGrp = new GuiElementGroup('a');
        GardensFurniture.dyeColorGroups.getDyeColorGroups().forEach(dyeColorGroup -> baseColorGrp.addElement(createColorElement(dyeColorGroup)));
        gui.addElement(baseColorGrp);

        var subColorGrp = new GuiElementGroup('l');
        IntStream.range(0, 7).forEach(i -> subColorGrp.addElement(createSubColorElement(i)));
        gui.addElement(subColorGrp);
        gui.addElement(new StaticGuiElement('x', blankHiddenItem, ""));
        gui.setCloseAction(close -> {
            onClose();
            return true;
        });
    }

    private void onClose() {
        var input = storage.getItem(0);
        if (input != null) {
            viewer.getInventory().addItem(input);
        }
    }

    private void onClickResult() {
        // run task later so it doesn't trigger removal of held item
        Bukkit.getScheduler().runTaskLater(GardensFurniture.plugin, () -> {
            storage.setItem(0, null);
            gui.draw();
        }, 1);
    }

    private DynamicGuiElement createSubColorElement(int i) {
        return new DynamicGuiElement('l', () -> {
            var subColor = currDyeGroup == null ? null : currDyeGroup.getSubColors().get(i);
            if (subColor == null) {
                return null;
            }
            return new StaticGuiElement(
                'l',
                getDyeTile(subColor.getBukkitColor()),
                onClick -> {
                    setDyedResult(subColor.getBukkitColor());
                    return true;
                },
                subColor.getName()
            );
        });
    }

    private void setDyedResult(Color color) {
        var dyedItem = storage.getItem(0);
        if (dyedItem == null) {
            return;
        }
        dyedItem = dyedItem.clone();
        if (dyedItem.getItemMeta() instanceof LeatherArmorMeta leatherMeta) {
            leatherMeta.setColor(color);
            dyedItem.setItemMeta(leatherMeta);
        }
        storage.setItem(1, dyedItem);
        gui.draw();
    }

    private GuiElement createColorElement(DyeColorGroup dyeColorGroup) {
        var baseColor = dyeColorGroup.getBaseColor();
        return new StaticGuiElement('a', getDyeTile(baseColor.getBukkitColor()), onClick -> {
            currDyeGroup = dyeColorGroup;
            setDyedResult(dyeColorGroup.getBaseColor().getBukkitColor());
            return true;
        }, baseColor.getName());
    }

    private void onApplyItem() {
        Bukkit.getScheduler().runTaskLater(GardensFurniture.plugin, () -> {
            var item = storage.getItem(0);
            if (ItemUtil.isEmpty(item)) {
                storage.setItem(1, null);
            }
            gui.draw();
        }, 1);
    }

    private ItemStack getDyeTile(Color color) {
        var tile = DYE_TILE.clone();
        if (tile.getItemMeta() instanceof LeatherArmorMeta leatherMeta) {
            leatherMeta.setColor(color);
            tile.setItemMeta(leatherMeta);
        }
        return tile;
    }

}
