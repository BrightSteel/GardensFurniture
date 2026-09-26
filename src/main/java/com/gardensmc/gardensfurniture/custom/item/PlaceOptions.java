package com.gardensmc.gardensfurniture.custom.item;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.bukkit.block.BlockFace;

@Setter
@Getter
@Accessors(chain = true)
public class PlaceOptions {

    private BlockFace blockFace = BlockFace.UP;
    private boolean persistentLeaves = true;
    private int multiplier;
    private String facing;
    private boolean force;
    // rotation to use when nobody is placing (world gen, structures, rollbacks); ignored when a player places
    private float yaw;

}
