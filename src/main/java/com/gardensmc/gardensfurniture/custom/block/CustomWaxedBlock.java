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
import org.bukkit.block.data.Openable;
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
import org.geysermc.geyser.api.block.custom.component.BoxComponent;
import org.geysermc.geyser.api.block.custom.component.CustomBlockComponents;
import org.geysermc.geyser.api.block.custom.component.GeometryComponent;
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
    public boolean placeBlock(Player player, ItemStack heldItem, CustomPlaceableItem customItem, Location location, PlaceOptions placeOptions, Block clickedBlock) {
        if (clickedBlock != null && clickedBlock.getBlockData() instanceof Slab slab && slab.getType() != Slab.Type.DOUBLE) {
            var clickedFurniture = FurnitureStoreHandler.INSTANCE.getFurnitureStore(clickedBlock);
            if (clickedFurniture != null && clickedFurniture.getItemIdentifier().equals(customItem.getIdentifier())) {
                return false; // furniture store already exists - simply turning into double slab at current location, handled by vanilla
            }
        }
        var furnitureStore = new FurnitureStore(customItem.getIdentifier());
        var block = location.getBlock();
        FurnitureStoreHandler.INSTANCE.addFurnitureStore(block, furnitureStore, heldItem != null);
        FurnitureStoreHandler.INSTANCE.removeIfNotPlaced(block, type -> type == waxedType.getMaterial());
        if (waxedType.toString().startsWith("DOOR")) {
            // top door block
            var top = block.getRelative(BlockFace.UP);
            FurnitureStoreHandler.INSTANCE.addFurnitureStore(top, furnitureStore, heldItem != null);
            FurnitureStoreHandler.INSTANCE.removeIfNotPlaced(top, type -> type == waxedType.getMaterial());
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
            builder.stringProperty("facing", Arrays.stream(BlockFace.values()).map(e -> e.toString().toLowerCase()).toList());
            builder.stringProperty("half", Arrays.stream(Bisected.Half.values()).map(e -> e.toString().toLowerCase()).toList());
            builder.stringProperty("shape", Arrays.stream(Stairs.Shape.values()).map(e -> e.toString().toLowerCase()).toList());
        } else if (type.startsWith("slab")) {
            builder.stringProperty("type", Arrays.stream(Slab.Type.values()).map(e -> e.toString().toLowerCase()).toList());
        } else if (type.startsWith("door_")) {
            builder.stringProperty("facing", Arrays.stream(BlockFace.values()).map(e -> e.toString().toLowerCase()).toList());
            builder.stringProperty("half", Arrays.stream(Bisected.Half.values()).map(e -> e.toString().toLowerCase()).toList());
            builder.stringProperty("hinge", Arrays.stream(Door.Hinge.values()).map(e -> e.toString().toLowerCase()).toList());
            builder.booleanProperty("open");
        } else if (type.startsWith("trapdoor_")) {
            builder.stringProperty("facing", Arrays.stream(BlockFace.values()).map(e -> e.toString().toLowerCase()).toList());
            builder.stringProperty("half", Arrays.stream(Bisected.Half.values()).map(e -> e.toString().toLowerCase()).toList());
            builder.booleanProperty("open");
        }
    }

    @Override
    public void onBuildGeyserBlockState(CustomBlockState.Builder builder, Block block) {
        // Branch on the (always-known) waxed type and set EVERY declared property, defaulting when
        // the live block data doesn't match (e.g. a block update where the block is mid-change) so
        // the state builder never fails with "Missing property".
        var blockData = block.getBlockData();
        var type = waxedType.toString().toLowerCase();
        if (type.startsWith("trapdoor_")) {
            builder.stringProperty("facing", blockData instanceof Directional d ? d.getFacing().toString().toLowerCase() : "north");
            builder.stringProperty("half", blockData instanceof Bisected b ? b.getHalf().toString().toLowerCase() : "bottom");
            builder.booleanProperty("open", blockData instanceof Openable o && o.isOpen());
        } else if (type.startsWith("stairs_")) {
            builder.stringProperty("facing", blockData instanceof Directional d ? d.getFacing().toString().toLowerCase() : "north");
            builder.stringProperty("half", blockData instanceof Bisected b ? b.getHalf().toString().toLowerCase() : "bottom");
            builder.stringProperty("shape", blockData instanceof Stairs s ? s.getShape().toString().toLowerCase() : "straight");
        } else if (type.startsWith("slab")) {
            builder.stringProperty("type", blockData instanceof Slab s ? s.getType().toString().toLowerCase() : "bottom");
        } else if (type.startsWith("door_")) {
            builder.stringProperty("facing", blockData instanceof Directional d ? d.getFacing().toString().toLowerCase() : "north");
            builder.stringProperty("half", blockData instanceof Bisected b ? b.getHalf().toString().toLowerCase() : "bottom");
            builder.stringProperty("hinge", blockData instanceof Door dr ? dr.getHinge().toString().toLowerCase() : "left");
            builder.booleanProperty("open", blockData instanceof Openable o && o.isOpen());
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
        // Slab: geometry.slab is a bottom slab. bottom = base, top = flip. double swaps to a full
        // block geometry (scaling 2x pivots on the block centre and grows into the ground) and
        // overrides the collision/selection box to a full block.
        if (waxedType.toString().toLowerCase().startsWith("slab")) {
            return List.of(
                    new CustomBlockPermutation(
                            CustomBlockComponents.builder().transformation(new TransformationComponent(180, 0, 0)).build(),
                            "q.block_state('type') == 'top'"),
                    new CustomBlockPermutation(
                            CustomBlockComponents.builder()
                                    .geometry(GeometryComponent.builder().identifier("geometry.slab_double").build())
                                    .collisionBox(new BoxComponent(-8, 0, -8, 16, 16, 16))
                                    .selectionBox(new BoxComponent(-8, 0, -8, 16, 16, 16))
                                    .build(),
                            "q.block_state('type') == 'double'")
            );
        }
        // Stairs: geometry.stairs shares geometry.trapdoor's default orientation (bottom, facing
        // south, tall step on the north half), so facing/half reuse the trapdoor's proven rotations.
        // Shape (corner stairs) is not a declared property — corners render as straight.
        if (waxedType.toString().toLowerCase().startsWith("stairs_")) {
            return List.of(
                    // bottom half (facing is flipped vs the trapdoor: the stair's step has a front)
                    new CustomBlockPermutation(
                            CustomBlockComponents.builder().transformation(new TransformationComponent(0, 0, 0)).build(),
                            "q.block_state('half') == 'bottom' && q.block_state('facing') == 'north'"),
                    new CustomBlockPermutation(
                            CustomBlockComponents.builder().transformation(new TransformationComponent(0, 180, 0)).build(),
                            "q.block_state('half') == 'bottom' && q.block_state('facing') == 'south'"),
                    new CustomBlockPermutation(
                            CustomBlockComponents.builder().transformation(new TransformationComponent(0, -90, 0)).build(),
                            "q.block_state('half') == 'bottom' && q.block_state('facing') == 'east'"),
                    new CustomBlockPermutation(
                            CustomBlockComponents.builder().transformation(new TransformationComponent(0, 90, 0)).build(),
                            "q.block_state('half') == 'bottom' && q.block_state('facing') == 'west'"),
                    // top half (upside-down) — same flips as the trapdoor top permutations
                    new CustomBlockPermutation(
                            CustomBlockComponents.builder().transformation(new TransformationComponent(0, 0, 180)).build(),
                            "q.block_state('half') == 'top' && q.block_state('facing') == 'north'"),
                    new CustomBlockPermutation(
                            CustomBlockComponents.builder().transformation(new TransformationComponent(180, 0, 0)).build(),
                            "q.block_state('half') == 'top' && q.block_state('facing') == 'south'"),
                    new CustomBlockPermutation(
                            CustomBlockComponents.builder().transformation(new TransformationComponent(180, 90, 0)).build(),
                            "q.block_state('half') == 'top' && q.block_state('facing') == 'east'"),
                    new CustomBlockPermutation(
                            CustomBlockComponents.builder().transformation(new TransformationComponent(180, -90, 0)).build(),
                            "q.block_state('half') == 'top' && q.block_state('facing') == 'west'")
            );
        }
        // Door: geometry.door has a 'lower' bone (door_bottom via '*') and an 'upper' bone (door_top
        // via 'top'), both a 16px panel; bone_visibility (set in the yml on 'half') shows the right
        // one per block, so each of the two door blocks renders its own half within the block bounds.
        // Both halves share the same facing/open transform. Closed doors rotate by facing; open doors
        // swing 90 about the hinge edge = rotation + a 1-block translation (block units). Closed
        // rendering ignores hinge (handle-side mirror only); open uses it.
        if (waxedType.toString().toLowerCase().startsWith("door_")) {
            return List.of(
                    // closed, by facing (base is 180 from the geometry so the panel sits on the front
                    // face and shares the hinge corner with the open permutations below)
                    new CustomBlockPermutation(
                            CustomBlockComponents.builder().transformation(new TransformationComponent(0, 180, 0)).build(),
                            "q.block_state('facing') == 'north' && !q.block_state('open')"),
                    new CustomBlockPermutation(
                            CustomBlockComponents.builder().transformation(new TransformationComponent(0, 90, 0)).build(),
                            "q.block_state('facing') == 'east' && !q.block_state('open')"),
                    new CustomBlockPermutation(
                            CustomBlockComponents.builder().transformation(new TransformationComponent(0, 0, 0)).build(),
                            "q.block_state('facing') == 'south' && !q.block_state('open')"),
                    new CustomBlockPermutation(
                            CustomBlockComponents.builder().transformation(new TransformationComponent(0, -90, 0)).build(),
                            "q.block_state('facing') == 'west' && !q.block_state('open')"),
                    // open — the panel sits on an edge, so a pure 90 rotation about the block centre
                    // swings it onto the perpendicular edge (stays inside the unit cube, no translation).
                    // hinge left = +90, hinge right = -90, composed with the closed facing rotation.
                    new CustomBlockPermutation(
                            CustomBlockComponents.builder().transformation(new TransformationComponent(0, 90, 0)).build(),
                            "q.block_state('facing') == 'north' && q.block_state('hinge') == 'left' && q.block_state('open')"),
                    new CustomBlockPermutation(
                            CustomBlockComponents.builder().transformation(new TransformationComponent(0, -90, 0)).build(),
                            "q.block_state('facing') == 'north' && q.block_state('hinge') == 'right' && q.block_state('open')"),
                    new CustomBlockPermutation(
                            CustomBlockComponents.builder().transformation(new TransformationComponent(0, 0, 0)).build(),
                            "q.block_state('facing') == 'east' && q.block_state('hinge') == 'left' && q.block_state('open')"),
                    new CustomBlockPermutation(
                            CustomBlockComponents.builder().transformation(new TransformationComponent(0, 180, 0)).build(),
                            "q.block_state('facing') == 'east' && q.block_state('hinge') == 'right' && q.block_state('open')"),
                    new CustomBlockPermutation(
                            CustomBlockComponents.builder().transformation(new TransformationComponent(0, -90, 0)).build(),
                            "q.block_state('facing') == 'south' && q.block_state('hinge') == 'left' && q.block_state('open')"),
                    new CustomBlockPermutation(
                            CustomBlockComponents.builder().transformation(new TransformationComponent(0, 90, 0)).build(),
                            "q.block_state('facing') == 'south' && q.block_state('hinge') == 'right' && q.block_state('open')"),
                    new CustomBlockPermutation(
                            CustomBlockComponents.builder().transformation(new TransformationComponent(0, 180, 0)).build(),
                            "q.block_state('facing') == 'west' && q.block_state('hinge') == 'left' && q.block_state('open')"),
                    new CustomBlockPermutation(
                            CustomBlockComponents.builder().transformation(new TransformationComponent(0, 0, 0)).build(),
                            "q.block_state('facing') == 'west' && q.block_state('hinge') == 'right' && q.block_state('open')")
            );
        }
        return Collections.emptyList();
    }
}
