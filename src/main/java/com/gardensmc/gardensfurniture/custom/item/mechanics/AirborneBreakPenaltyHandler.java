package com.gardensmc.gardensfurniture.custom.item.mechanics;

import com.gardensmc.gardensfurniture.GardensFurniture;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlotGroup;
import org.bukkit.inventory.ItemStack;

/**
 * Drives the {@code disable_airborn_break_penalty} mechanic for normal (client-predicted) blocks.
 *
 * <p>Vanilla divides a player's block-break speed by 5 while they're airborne. The client predicts
 * that slowdown locally from its own {@link Attribute#BLOCK_BREAK_SPEED} value, so to disable it we
 * grant a transient x5 modifier on that attribute (the exact inverse of the /5 penalty) whenever the
 * player is airborne and holding a main-hand item that carries the mechanic. The attribute change is
 * synced to the client, so its break prediction runs at full speed again.
 *
 * <p>The modifier is <b>transient</b> (never written to player data): if the server stops mid-air the
 * buff simply vanishes on next join, so it can't leak into a permanent mining boost. Custom furniture
 * blocks are server-driven instead and handle the same penalty in {@code CustomBreakingBlock}.
 */
public class AirborneBreakPenaltyHandler {

    public static final AirborneBreakPenaltyHandler INSTANCE = new AirborneBreakPenaltyHandler();

    private static final NamespacedKey MODIFIER_KEY =
            new NamespacedKey("gardensfurniture", "airborne_break_penalty");

    // vanilla airborne penalty divides break speed by this; the inverse cancels it exactly.
    private static final double AIRBORNE_PENALTY_DIVISOR = 5.0;
    // MULTIPLY_SCALAR_1 scales the attribute total by (1 + value), so value = divisor - 1 gives xdivisor.
    private static final double MODIFIER_VALUE = AIRBORNE_PENALTY_DIVISOR - 1.0;

    private AirborneBreakPenaltyHandler() {
    }

    public void tick() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            var attribute = player.getAttribute(Attribute.BLOCK_BREAK_SPEED);
            if (attribute == null) {
                continue;
            }
            boolean shouldApply = player.getGameMode() != GameMode.CREATIVE
                    && !player.isOnGround()
                    && hasMechanic(player.getInventory().getItemInMainHand());
            boolean isApplied = attribute.getModifier(MODIFIER_KEY) != null;
            if (shouldApply && !isApplied) {
                // ANY (not MAINHAND): this is added directly to the player's attribute, and we already
                // gate on the main-hand item ourselves - a slot-scoped modifier wouldn't count here.
                attribute.addTransientModifier(new AttributeModifier(
                        MODIFIER_KEY, MODIFIER_VALUE, AttributeModifier.Operation.MULTIPLY_SCALAR_1, EquipmentSlotGroup.ANY));
            } else if (!shouldApply && isApplied) {
                attribute.removeModifier(MODIFIER_KEY);
            }
        }
    }

    // True if the main-hand item is a custom item carrying the disable_airborn_break_penalty mechanic.
    public static boolean hasMechanic(ItemStack item) {
        var customItem = GardensFurniture.itemRegistry.getCustomItem(item);
        return customItem != null
                && customItem.getMechanics() != null
                && customItem.getMechanics().isDisableAirbornBreakPenalty();
    }
}
