package com.gardensmc.gardensfurniture.custom.block;

import com.gardensmc.gardensfurniture.custom.block.bedrock.BedrockBlock;
import com.gardensmc.gardensfurniture.custom.item.BedrockBlockState;
import com.gardensmc.gardensfurniture.custom.item.CustomPlaceableItem;
import com.gardensmc.gardensfurniture.custom.item.PlaceOptions;
import lombok.Getter;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.geysermc.geyser.api.block.custom.CustomBlockData;
import org.geysermc.geyser.api.block.custom.CustomBlockPermutation;
import org.geysermc.geyser.api.block.custom.CustomBlockState;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public abstract class CustomBlock {

    private final boolean barrier;
    @Getter
    private final boolean seated;
    private int[] seats;
    private final boolean cancel_drop;
    private final double seat_offset;
    @Getter
    private final int light;
    @Getter
    private final int lightOffset;
    private final String drop_override;
    @Getter
    private final Material materialTraits;
    @Getter
    private final BedrockBlock bedrock;
    @Getter
    private final String blockId;

    public CustomBlock(ConfigurationSection blockSection, String identifier) {
        this.barrier = blockSection.getBoolean("barrier");
        this.seated = blockSection.getBoolean("seated");
        this.cancel_drop = blockSection.getBoolean("cancel_drop");
        this.seat_offset = blockSection.getDouble("seat_offset");
        this.light = blockSection.getInt("light");
        this.lightOffset = blockSection.getInt("light_offset");
        this.drop_override = blockSection.getString("drop_override");
        var materialTraitsStr = blockSection.getString("material_traits");
        this.materialTraits = materialTraitsStr == null ? null : Material.valueOf(materialTraitsStr.toUpperCase());
        var bedrockSection = blockSection.getConfigurationSection("bedrock");
        this.bedrock = bedrockSection == null ? null : new BedrockBlock(bedrockSection, identifier);
        this.blockId = identifier;
    }

    public boolean hasBarrier() {
        return barrier;
    }

    public boolean cancelDrop() {
        return cancel_drop;
    }

    public double getSeatOffset() {
        return seat_offset;
    }

    public String getDropOverride() {
        return drop_override;
    }

    public abstract boolean isSolid();

    protected ConfigurationSection getConfigSectionOrThrow(ConfigurationSection config, String sectionName, String customBlockName) {
        if (!config.isConfigurationSection(sectionName)) {
            throw new RuntimeException(String.format("No %s section exists for custom block %s", sectionName, customBlockName));
        }
        return config.getConfigurationSection(sectionName);
    }

    public boolean placeBlock(Player player, ItemStack heldItem, CustomPlaceableItem placeableItem, Location location) {
        return placeBlock(player, heldItem, placeableItem, location, new PlaceOptions());
    }

    public abstract boolean placeBlock(Player player, ItemStack heldItem, CustomPlaceableItem placeableItem, Location location, PlaceOptions placeOptions);

    public void onBreak(Location location) {
    }

    public void onBuildGeyserBlockData(CustomBlockData.Builder builder) {
    }

    public void onBuildGeyserBlockState(CustomBlockState.Builder builder, Block block) {
    }

    public List<CustomBlockPermutation> getPermutations() {
        return Collections.emptyList();
    }
}
