package com.gardensmc.gardensfurniture.custom.item.mechanics;

import lombok.Getter;
import org.bukkit.configuration.ConfigurationSection;

@Getter
public class Mechanics {

    private final String stripped_log;
    private final String sapling_structure;
    private final String fungus_structure;
    private final CaptureMobsMechanic capture_mobs;
    private final String pot_item;
    // ender_step: on double-shift, raytrace to the block being looked at and teleport there.
    // Only meaningful on armor; range scales with how many equipped armor pieces have it.
    private final boolean ender_step;
    // disable_airborn_break_penalty: while this item is held in the main hand and the player is airborne,
    // cancel the vanilla 5x mining slowdown by granting a temporary block-break-speed multiplier
    // (see AirborneBreakPenaltyHandler and CustomBreakingBlock).
    private final boolean disable_airborn_break_penalty;

    public Mechanics(ConfigurationSection mechanicsSection) {
        this.stripped_log = mechanicsSection.getString("stripped_log");
        this.sapling_structure = mechanicsSection.getString("sapling_structure");
        this.fungus_structure = mechanicsSection.getString("fungus_structure");
        var captureMobsSection = mechanicsSection.getConfigurationSection("capture_mobs");
        this.capture_mobs = captureMobsSection != null ? new CaptureMobsMechanic(captureMobsSection) : null;
        this.pot_item = mechanicsSection.getString("pot_item");
        this.ender_step = mechanicsSection.getBoolean("ender_step");
        this.disable_airborn_break_penalty = mechanicsSection.getBoolean("disable_airborn_break_penalty");
    }

    public boolean isCaptureMobs() {
        return capture_mobs != null && capture_mobs.isEnabled();
    }

    public boolean isEnderStep() {
        return ender_step;
    }

    public boolean isDisableAirbornBreakPenalty() {
        return disable_airborn_break_penalty;
    }
}
