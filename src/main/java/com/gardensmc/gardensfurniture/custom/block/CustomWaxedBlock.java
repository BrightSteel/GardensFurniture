package com.gardensmc.gardensfurniture.custom.block;

import com.gardensmc.gardensfurniture.custom.item.BedrockBlockState;
import com.gardensmc.gardensfurniture.custom.item.CustomPlaceableItem;
import com.gardensmc.gardensfurniture.custom.item.PlaceOptions;
import com.gardensmc.gardensfurniture.store.FurnitureStore;
import com.gardensmc.gardensfurniture.store.FurnitureStoreHandler;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.Bisected;
import org.bukkit.block.data.Directional;
import org.bukkit.block.data.Waterlogged;
import org.bukkit.block.data.type.Door;
import org.bukkit.block.data.type.Slab;
import org.bukkit.block.data.type.Stairs;
import org.bukkit.block.data.type.TrapDoor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.geysermc.geyser.api.block.custom.CustomBlockData;
import org.geysermc.geyser.api.block.custom.CustomBlockPermutation;
import org.geysermc.geyser.api.block.custom.CustomBlockState;
import org.geysermc.geyser.api.block.custom.component.CustomBlockComponents;
import org.geysermc.geyser.api.block.custom.component.TransformationComponent;

import java.util.*;

@Getter
public class CustomWaxedBlock extends CustomBlock {

    private final WaxedType waxedType;

    public CustomWaxedBlock(ConfigurationSection blockSection, String identifier, String blockId) {
        super(blockSection, blockId);
        var waxedSection = blockSection.getConfigurationSection("waxed_data");
        if (waxedSection == null) {
            throw new IllegalStateException("Missing waxed_data for block " + identifier);
        }
        var waxedTypeStr = waxedSection.getString("type");
        if (waxedTypeStr == null) {
            throw new IllegalStateException("Missing waxed type for " + identifier);
        }
        var waxedVariation = waxedSection.getString("variation");
        if (waxedVariation == null) {
            throw new IllegalStateException("Missing waxed variation for " + identifier);
        }
        this.waxedType = WaxedType.valueOf((waxedTypeStr + "_" + waxedVariation).toUpperCase());
    }

    @Override
    public boolean isSolid() {
        return true;
    }

    @Override
    public boolean placeBlock(Player player, ItemStack heldItem, CustomPlaceableItem customItem, Location location, PlaceOptions placeOptions) {
        var furnitureStore = new FurnitureStore(customItem.getIdentifier());
        FurnitureStoreHandler.INSTANCE.addFurnitureStore(location.getBlock(), furnitureStore, heldItem != null);
        if (waxedType.toString().startsWith("DOOR")) {
            // top door block
            FurnitureStoreHandler.INSTANCE.addFurnitureStore(location.getBlock().getRelative(BlockFace.UP), furnitureStore, heldItem != null);
        }
        return false; // allow natural placement
    }

    @Getter
    @AllArgsConstructor
    public enum WaxedType {
        STAIRS_NORMAL(Material.WAXED_CUT_COPPER_STAIRS),
        SLAB_NORMAL(Material.WAXED_CUT_COPPER_SLAB),
        DOOR_NORMAL(Material.WAXED_COPPER_DOOR),
        TRAPDOOR_NORMAL(Material.WAXED_COPPER_TRAPDOOR),

        STAIRS_EXPOSED(Material.WAXED_EXPOSED_CUT_COPPER_STAIRS),
        SLAB_EXPOSED(Material.WAXED_EXPOSED_CUT_COPPER_SLAB),
        DOOR_EXPOSED(Material.WAXED_EXPOSED_COPPER_DOOR),
        TRAPDOOR_EXPOSED(Material.WAXED_EXPOSED_COPPER_TRAPDOOR),

        STAIRS_WEATHERED(Material.WAXED_WEATHERED_CUT_COPPER_STAIRS),
        SLAB_WEATHERED(Material.WAXED_WEATHERED_CUT_COPPER_SLAB),
        DOOR_WEATHERED(Material.WAXED_WEATHERED_COPPER_DOOR),
        TRAPDOOR_WEATHERED(Material.WAXED_WEATHERED_COPPER_TRAPDOOR),

        STAIRS_OXIDIZED(Material.WAXED_OXIDIZED_CUT_COPPER_STAIRS),
        SLAB_OXIDIZED(Material.WAXED_OXIDIZED_CUT_COPPER_SLAB),
        DOOR_OXIDIZED(Material.WAXED_OXIDIZED_COPPER_DOOR),
        TRAPDOOR_OXIDIZED(Material.WAXED_OXIDIZED_COPPER_TRAPDOOR);

        public static List<Material> materials() {
            return Arrays.stream(values()).map(WaxedType::getMaterial).toList();
        }

        private final Material material;
    }

//    @Override
//    public Map<String, String> getBlockStates(Block block) {
//        var states = new HashMap<String, String>();
//        var blockData = block.getBlockData();
//        switch (blockData) {
//            case Stairs stairs -> {
//                states.put("shape", stairs.getShape().toString().toLowerCase());
//                states.put("facing", stairs.getFacing().toString().toLowerCase());
//                states.put("half", stairs.getHalf().toString().toLowerCase());
//                states.put("waterlogged", String.valueOf(stairs.isWaterlogged()));
//            }
//            case Slab slab -> {
//                states.put("type", slab.getType().toString().toLowerCase());
//                states.put("waterlogged", String.valueOf(slab.isWaterlogged()));
//            }
//            case Door door -> {
//                states.put("hinge", door.getHinge().toString().toLowerCase());
//                states.put("facing", door.getFacing().toString().toLowerCase());
//                states.put("half", door.getHalf().toString().toLowerCase());
//                states.put("open", String.valueOf(door.isOpen()));
//                states.put("powered", String.valueOf(door.isPowered()));
//            }
//            case TrapDoor trapDoor -> {
//                states.put("facing", trapDoor.getFacing().toString().toLowerCase());
//                states.put("half", trapDoor.getHalf().toString().toLowerCase());
//                states.put("open", String.valueOf(trapDoor.isOpen()));
//            }
//            default -> {
//            }
//        }
//        return states;
//    }

    @Override
    public void onBuildGeyserBlockData(CustomBlockData.Builder builder) {
        var type = waxedType.toString().toLowerCase();
        if (type.startsWith("stairs_")) {
//            states.add(new BedrockBlockState("shape", Arrays.stream(Stairs.Shape.values()).map(e -> e.toString().toLowerCase()).toList()));
//            states.add(BedrockBlockState.of("facing", Arrays.stream(BlockFace.values()).map(e -> e.toString().toLowerCase()).toList()));
//            states.add(BedrockBlockState.of("half", Arrays.stream(Bisected.Half.values()).map(e -> e.toString().toLowerCase()).toList()));
//            states.add(BedrockBlockState.of("waterlogged", List.of("true", "false")));
        } else if (type.startsWith("slab")) {
//            states.add(BedrockBlockState.of("type", Arrays.stream(Slab.Type.values()).map(e -> e.toString().toLowerCase()).toList()));
//            states.add(BedrockBlockState.of("waterlogged", List.of("true", "false")));
        } else if (type.startsWith("door_")) {
//            states.add(BedrockBlockState.of("hinge", Arrays.stream(Door.Hinge.values()).map(e -> e.toString().toLowerCase()).toList()));
//            states.add(BedrockBlockState.of("facing", Arrays.stream(BlockFace.values()).map(e -> e.toString().toLowerCase()).toList()));
//            states.add(BedrockBlockState.of("half", Arrays.stream(Bisected.Half.values()).map(e -> e.toString().toLowerCase()).toList()));
//            states.add(BedrockBlockState.of("open", List.of("true", "false")));
//            states.add(BedrockBlockState.of("powered", List.of("true", "false")));
        } else if (type.startsWith("trapdoor_")) {
            builder.stringProperty("facing", Arrays.stream(BlockFace.values()).map(e -> e.toString().toLowerCase()).toList());
            builder.stringProperty("half", Arrays.stream(Bisected.Half.values()).map(e -> e.toString().toLowerCase()).toList());
            builder.booleanProperty("open");


//            states.add(BedrockBlockState.of("powered", List.of("true", "false")));
//            states.add(BedrockBlockState.of("facing", Arrays.stream(BlockFace.values()).map(e -> e.toString().toLowerCase()).toList()));
//            states.add(BedrockBlockState.of("half", Arrays.stream(Bisected.Half.values()).map(e -> e.toString().toLowerCase()).toList()));
//            states.add(BedrockBlockState.of("open", List.of("true", "false")));
//            states.add(BedrockBlockState.of("waterlogged", List.of("true", "false")));
        }
    }

    @Override
    public void onBuildGeyserBlockState(CustomBlockState.Builder builder, Block block) {
        var blockData = block.getBlockData();
        if (blockData instanceof TrapDoor trapDoor) {
            builder.stringProperty("facing", trapDoor.getFacing().toString().toLowerCase());
            builder.stringProperty("half", trapDoor.getHalf().toString().toLowerCase());
            builder.booleanProperty("open", trapDoor.isOpen());
        }
    }

    @Override
    public List<CustomBlockPermutation> getPermutations() {
        if (waxedType.toString().toLowerCase().startsWith("trapdoor_")) {
            return List.of(
                    // top closed
                    new CustomBlockPermutation(
                            CustomBlockComponents.builder().transformation(new TransformationComponent(0, 0, 180)).build(),
                            "q.block_state('half') == 'top' && q.block_state('facing') == 'south' && !q.block_state('open')"
                    ),
                    new CustomBlockPermutation(
                            CustomBlockComponents.builder().transformation(new TransformationComponent(180, 0, 0)).build(),
                            "q.block_state('half') == 'top' && q.block_state('facing') == 'north' && !q.block_state('open')"
                    ),
                    new CustomBlockPermutation(
                            CustomBlockComponents.builder().transformation(new TransformationComponent(180, 90, 0)).build(),
                            "q.block_state('half') == 'top' && q.block_state('facing') == 'west' && !q.block_state('open')"
                    ),
                    new CustomBlockPermutation(
                            CustomBlockComponents.builder().transformation(new TransformationComponent(180, -90, 0)).build(),
                            "q.block_state('half') == 'top' && q.block_state('facing') == 'east' && !q.block_state('open')"
                    ),
                    // top open
                    new CustomBlockPermutation(
                            CustomBlockComponents.builder().transformation(new TransformationComponent(90, 0, 0)).build(),
                            "q.block_state('half') == 'top' && q.block_state('facing') == 'south' && q.block_state('open')"
                    ),
                    new CustomBlockPermutation(
                            CustomBlockComponents.builder().transformation(new TransformationComponent(-90, 0, 180)).build(),
                            "q.block_state('half') == 'top' && q.block_state('facing') == 'north' && q.block_state('open')"
                    ),
                    new CustomBlockPermutation(
                            CustomBlockComponents.builder().transformation(new TransformationComponent(0, -90, 90)).build(),
                            "q.block_state('half') == 'top' && q.block_state('facing') == 'west' && q.block_state('open')"
                    ),
                    new CustomBlockPermutation(
                            CustomBlockComponents.builder().transformation(new TransformationComponent(180, 90, 90)).build(),
                            "q.block_state('half') == 'top' && q.block_state('facing') == 'east' && q.block_state('open')"
                    ),
                    // bottom closed
                    new CustomBlockPermutation(
                            CustomBlockComponents.builder().transformation(new TransformationComponent(0, 0, 0)).build(),
                            "q.block_state('half') == 'bottom' && q.block_state('facing') == 'south' && !q.block_state('open')"
                    ),
                    new CustomBlockPermutation(
                            CustomBlockComponents.builder().transformation(new TransformationComponent(0, 180, 0)).build(),
                            "q.block_state('half') == 'bottom' && q.block_state('facing') == 'north' && !q.block_state('open')"
                    ),
                    new CustomBlockPermutation(
                            CustomBlockComponents.builder().transformation(new TransformationComponent(0, -90, 0)).build(),
                            "q.block_state('half') == 'bottom' && q.block_state('facing') == 'west' && !q.block_state('open')"
                    ),
                    new CustomBlockPermutation(
                            CustomBlockComponents.builder().transformation(new TransformationComponent(0, 90, 0)).build(),
                            "q.block_state('half') == 'bottom' && q.block_state('facing') == 'east' && !q.block_state('open')"
                    ),
                    // bottom open
                    new CustomBlockPermutation(
                            CustomBlockComponents.builder().transformation(new TransformationComponent(90, 0, 180)).build(),
                            "q.block_state('half') == 'bottom' && q.block_state('facing') == 'south' && q.block_state('open')"
                    ),
                    new CustomBlockPermutation(
                            CustomBlockComponents.builder().transformation(new TransformationComponent(-90, 0, 0)).build(),
                            "q.block_state('half') == 'bottom' && q.block_state('facing') == 'north' && q.block_state('open')"
                    ),
                    new CustomBlockPermutation(
                            CustomBlockComponents.builder().transformation(new TransformationComponent(0, 90, 90)).build(),
                            "q.block_state('half') == 'bottom' && q.block_state('facing') == 'west' && q.block_state('open')"
                    ),
                    new CustomBlockPermutation(
                            CustomBlockComponents.builder().transformation(new TransformationComponent(180, -90, 90)).build(),
                            "q.block_state('half') == 'bottom' && q.block_state('facing') == 'east' && q.block_state('open')"
                    )
            );
        }
        return Collections.emptyList();
    }
}
