//package com.gardensmc.gardensfurniture.menu;
//
//import com.gardensmc.gardensfurniture.GardensFurniture;
//import com.gardensmc.gardensfurniture.backpack.BackpackHandler;
//import com.gardensmc.gardensfurniture.custom.item.CustomBackpackItem;
//import de.themoep.inventorygui.GuiElementGroup;
//import de.themoep.inventorygui.GuiStorageElement;
//import lombok.AllArgsConstructor;
//import lombok.Getter;
//import lombok.RequiredArgsConstructor;
//import org.bukkit.Bukkit;
//import org.bukkit.Material;
//import org.bukkit.entity.Player;
//import org.bukkit.inventory.Inventory;
//import org.bukkit.inventory.ItemStack;
//
//import java.util.Arrays;
//import java.util.stream.Collectors;
//import java.util.stream.IntStream;
//
//public class BackpackMenu extends Menu {
//
//    @Getter
//    private final ItemStack backpackItem;
//    private final Inventory inventory;
//
//    public BackpackMenu(Player viewer, RowConfig rowConfig, ItemStack backpackItem, ItemStack[] items) {
//        super(getLayout(rowConfig), getTitle(rowConfig), viewer);
//        this.backpackItem = backpackItem;
//        this.inventory = Bukkit.createInventory(viewer, rowConfig.getTotalRows() * 9);
//        if (items != null) {
//            inventory.setContents(items);
//        }
//        initElements();
//    }
//
//    private static String[] getLayout(RowConfig rowConfig) {
//        var layout = new String[rowConfig.getTotalRows()];
//        IntStream.range(0, rowConfig.fillerRows).forEach(i -> layout[i] = "bbbbbbbbb");
//        IntStream.range(rowConfig.fillerRows, layout.length).forEach(i -> layout[i] = "aaaaaaaaa");
//        return layout;
//    }
//
//    private static String getTitle(RowConfig rowConfig) {
//        return "<white><font:gardens:default>\u0105\u010B" + rowConfig.symbol + "</font>";
//    }
//
//    public void initElements() {
//        var grp = new GuiElementGroup('a');
//        grp.addElements(IntStream.range(0, inventory.getSize())
//                .mapToObj(i -> new GuiStorageElement('a', inventory, i, () -> System.out.println("Test"), this::isValidItem, (t) -> true))
//                .collect(Collectors.toList()));
//        gui.addElement(grp);
//        gui.addElement('b', blankHiddenItem);
//
//        gui.setCloseAction(close -> {
//            onCloseInventory();
//            return true;
//        });
//    }
//
//    private boolean isValidItem(GuiStorageElement.ValidatorInfo validatorInfo) {
//        var item = validatorInfo.getItem();
//        System.out.println("Validating: " + item);
//        if (item != null) {
//            if (item.getType() == Material.SHULKER_BOX || item.getType() == Material.BUNDLE) {
//                return false;
//            }
//            return !(GardensFurniture.itemRegistry.getCustomItem(item) instanceof CustomBackpackItem);
//        }
//        return true;
//    }
//
//    private void onCloseInventory() {
//        BackpackHandler.onClose(viewer, backpackItem, inventory);
//    }
//
//    @RequiredArgsConstructor
//    @AllArgsConstructor
//    @Getter
//    public enum RowConfig {
//        TIER1(1, '\u0130', 5),
//        TIER2(2, '\u0131', 4),
//        TIER3(3, '\u0132', 3),
//        TIER4(4, '\u0133', 2),
//        TIER5(5, '\u0134', 1),
//        TIER6(6, '\u0135');
//
//        private final int rows;
//        private final char symbol;
//        private int fillerRows;
//
//        public int getTotalRows() {
//            return rows + fillerRows;
//        }
//
//        public static RowConfig fromRows(int rows) {
//            return Arrays.stream(RowConfig.values()).
//                    filter(rowConfig -> rowConfig.rows == rows)
//                    .findFirst()
//                    .orElseThrow();
//        }
//    }
//}
