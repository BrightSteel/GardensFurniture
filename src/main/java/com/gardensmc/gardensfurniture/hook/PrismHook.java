package com.gardensmc.gardensfurniture.hook;

import com.gardensmc.gardensfurniture.GardensFurniture;
import com.gardensmc.gardensfurniture.custom.CustomBlockManager;
import com.gardensmc.gardensfurniture.custom.block.CustomBlock;
import com.gardensmc.gardensfurniture.custom.block.CustomSaplingBlock;
import com.gardensmc.gardensfurniture.custom.item.CustomItem;
import com.gardensmc.gardensfurniture.custom.item.CustomPlaceableItem;
import com.gardensmc.gardensfurniture.custom.item.PlaceOptions;
import com.gardensmc.gardensfurniture.events.FurnitureBreakEvent;
import com.gardensmc.gardensfurniture.events.FurniturePlaceEvent;
import com.gardensmc.gardensfurniture.store.FurnitureStore;
import com.gardensmc.gardensfurniture.store.FurnitureStoreHandler;
import de.tr7zw.nbtapi.NBT;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Interaction;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.prism_mc.prism.api.actions.Action;
import org.prism_mc.prism.api.actions.ItemAction;
import org.prism_mc.prism.api.actions.types.ActionResultType;
import org.prism_mc.prism.api.actions.types.ActionType;
import org.prism_mc.prism.api.activities.Activity;
import org.prism_mc.prism.api.services.modifications.ModificationHandler;
import org.prism_mc.prism.api.services.modifications.ModificationQueueMode;
import org.prism_mc.prism.api.services.modifications.ModificationResult;
import org.prism_mc.prism.api.services.modifications.ModificationRuleset;
import org.prism_mc.prism.paper.api.PrismPaperApi;
import org.prism_mc.prism.paper.api.activities.PaperActivity;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.logging.Level;

/**
 * Prism (prism-mc.org) rollback/restore integration for furniture.
 *
 * <p>Furniture comes in three physical shapes and this hook records and reconstructs all of them
 * through a <b>single</b> Prism mechanism — a reversible <b>item action</b> located at the
 * furniture's block:</p>
 * <ul>
 *   <li><b>BLOCK</b> — note block / tripwire / leaves / cave vine / waxed copper. A real vanilla
 *       block plus a {@link FurnitureStore} in chunk NBT; rendered client-side via packets. No entity.</li>
 *   <li><b>DISPLAY</b> — chairs, tables, etc. An {@code ItemDisplay} plus an {@code Interaction}
 *       or {@code BARRIER} hitbox (and optional light). No {@link FurnitureStore}.</li>
 *   <li><b>HYBRID</b> — saplings. Both a block+store and a display.</li>
 * </ul>
 *
 * <p>Why item actions: Prism's public factory can't attach custom metadata to a block action, and
 * block furniture has no entity for an entity action. An item action carries both the full display
 * {@link ItemStack} NBT (dye/model) <i>and</i> an arbitrary descriptor string, which is enough to
 * reconstruct every kind — so no separate side-store (e.g. SQLite) is needed; Prism is the store.</p>
 *
 * <p>Recording sources (complete coverage, no per-type event plumbing):</p>
 * <ul>
 *   <li>BLOCK + HYBRID: {@link FurnitureStoreHandler#lifecycleListener} — fired whenever a
 *       {@link FurnitureStore} is written/removed, the canonical lifecycle for stored furniture.</li>
 *   <li>DISPLAY: {@link FurniturePlaceEvent}/{@link FurnitureBreakEvent} (skipped when a store is
 *       present, so the hybrid sapling is recorded once via the store path only).</li>
 * </ul>
 *
 * <p>When an admin runs {@code /pr rollback|restore}, Prism invokes our handler synchronously for
 * the affected furniture activities and we own the world mutation: rebuild or tear down.</p>
 *
 * <p>DISPLAY rebuilds replay {@code placeBlock} with no player, using the display's yaw recorded in
 * the descriptor's facing slot so rotation and barrier vectors match the original.</p>
 *
 * <p><b>Needs live-server validation:</b> multi-block waxed doors and barrier-vector furniture, and
 * that Prism's item-action rollback defers entirely to our handler.</p>
 */
public class PrismHook implements Listener {

    public static final String PLACE_ACTION_KEY = "gardens-furniture-place";
    public static final String BREAK_ACTION_KEY = "gardens-furniture-break";

    private static final String DESCRIPTOR_VERSION = "gf1";

    private enum Kind { BLOCK, DISPLAY, HYBRID }

    private static PrismPaperApi prism;
    private ActionType placeAction;
    private ActionType breakAction;

    public static boolean isActive() {
        return prism != null;
    }

    /**
     * Registers our custom actions with Prism. Callers must confirm the Prism plugin is installed
     * <em>before</em> constructing/invoking this hook, so its Prism API references never load when
     * Prism is absent. Returns false (with a warning) if Prism is present but its API service isn't.
     */
    public boolean init() {
        RegisteredServiceProvider<PrismPaperApi> provider =
                Bukkit.getServicesManager().getRegistration(PrismPaperApi.class);
        if (provider == null) {
            GardensFurniture.plugin.getLogger().warning("Prism plugin present but its API service is unavailable — furniture rollback/restore integration disabled.");
            return false;
        }
        prism = provider.getProvider();

        placeAction = prism.actionTypeRegistry().registerItemAction(
                PLACE_ACTION_KEY, ActionResultType.CREATES, true,
                new FurnitureModificationHandler(false), "placed");
        breakAction = prism.actionTypeRegistry().registerItemAction(
                BREAK_ACTION_KEY, ActionResultType.REMOVES, true,
                new FurnitureModificationHandler(true), "broke");

        // Stored (block/hybrid) furniture: record whenever a FurnitureStore is added/removed.
        FurnitureStoreHandler.lifecycleListener = this::onStoreLifecycle;

        GardensFurniture.plugin.getLogger().info("Hooked into Prism — furniture rollback/restore enabled.");
        return true;
    }

    // ------------------------------------------------------------------
    // Recording
    // ------------------------------------------------------------------

    private void onStoreLifecycle(Block block, boolean added) {
        // Suppression + null-listener already handled by FurnitureStoreHandler before calling us.
        var store = FurnitureStoreHandler.INSTANCE.getFurnitureStore(block);
        String identifier = store != null ? store.getItemIdentifier() : null;
        if (identifier == null) return;
        var customItem = GardensFurniture.itemRegistry.getItem(identifier);
        Kind kind = (customItem instanceof CustomPlaceableItem pi && pi.getCustomBlock() instanceof CustomSaplingBlock)
                ? Kind.HYBRID : Kind.BLOCK;

        ItemStack item = displayItemAt(block.getLocation()); // present for hybrid; null for pure block
        if (item == null) {
            item = GardensFurniture.itemRegistry.buildItem(identifier);
        }
        // a delayed removal runs after the block is gone - use the state captured before the break
        String removedBlockData = added ? null : FurnitureStoreHandler.removedBlockData.get();
        String blockData = removedBlockData != null ? removedBlockData : block.getBlockData().getAsString();
        String descriptor = encode(kind, identifier,
                store.getFacing(), store.getMultiplier(), blockData);

        record(added ? placeAction : breakAction, block.getLocation(), item, descriptor,
                FurnitureStoreHandler.currentActor.get());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onFurniturePlace(FurniturePlaceEvent e) {
        if (!isActive() || suppressed()) return;
        recordDisplay(placeAction, e.getFurniture().itemDisplay(),
                CustomBlockManager.getInteractionEntity(displayLoc(e.getFurniture().itemDisplay())),
                e.getFurniture().customItem(), e.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onFurnitureBreak(FurnitureBreakEvent e) {
        if (!isActive() || suppressed()) return;
        Entity entity = e.getItemEntity().getEntity();
        if (!(entity instanceof ItemDisplay display)) return;
        recordDisplay(breakAction, display, e.getHitbox(), e.getCustomItem(), e.getPlayer());
    }

    private void recordDisplay(ActionType type, ItemDisplay display, Interaction interaction,
                               CustomItem customItem, Object cause) {
        if (display == null) return;
        Block block = baseBlock(display, interaction);
        // Hybrid furniture (sapling) is recorded via the store path; don't double-record here.
        if (FurnitureStoreHandler.INSTANCE.getFurnitureStore(block) != null) return;
        // display furniture has no facing: store its yaw in that slot so a rebuild faces the same way
        String descriptor = encode(Kind.DISPLAY, customItem.getIdentifier(), Float.toString(display.getLocation().getYaw()), 0, "");
        record(type, block.getLocation(), display.getItemStack(), descriptor, cause);
    }

    private void record(ActionType type, Location loc, ItemStack item, String descriptor, Object cause) {
        if (item == null) item = new ItemStack(Material.PAPER); // placeholder; block rebuild ignores it
        Action action = prism.actionFactory().createItemAction(type, item, 1, descriptor);
        Activity activity = PaperActivity.builder()
                .action(action)
                .location(loc.toBlockLocation())
                .cause(cause != null ? cause : GardensFurniture.plugin.getName())
                .build();
        prism.recordingService().addToQueue(activity);
    }

    // ------------------------------------------------------------------
    // Modification handler
    // ------------------------------------------------------------------

    /**
     * @param presentAfterRollback whether furniture should exist after a rollback of this action.
     *                             place → false (rollback removes), break → true (rollback re-adds).
     */
    private record FurnitureModificationHandler(boolean presentAfterRollback) implements ModificationHandler {

        @Override
        public ModificationResult applyRollback(ModificationRuleset r, Object owner, Activity activity, ModificationQueueMode mode) {
            if (mode == ModificationQueueMode.COMPLETING) {
                if (presentAfterRollback) rebuild(activity);
                else teardown(activity);
            }
            return ModificationResult.builder().activity(activity).statusFromMode(mode).build();
        }

        @Override
        public ModificationResult applyRestore(ModificationRuleset r, Object owner, Activity activity, ModificationQueueMode mode) {
            if (mode == ModificationQueueMode.COMPLETING) {
                // restore is the inverse of rollback
                if (presentAfterRollback) teardown(activity);
                else rebuild(activity);
            }
            return ModificationResult.builder().activity(activity).statusFromMode(mode).build();
        }
    }

    // ------------------------------------------------------------------
    // World mutation (guarded so our own changes are never re-recorded)
    // ------------------------------------------------------------------

    private static void teardown(Activity activity) {
        Block block = blockOf(activity);
        if (block == null) return;
        Location loc = block.getLocation();
        FurnitureStoreHandler.suppressLifecycle.set(true);
        try {
            // Removes display + interaction + barrier blocks + base block, and fires FurnitureBreakEvent
            // (which strips light for display furniture). player == null → no drops.
            CustomBlockManager.breakFurniture(null, loc, false);
            // Block/hybrid furniture: clear the store, the base block, and any block-furniture light.
            var store = FurnitureStoreHandler.INSTANCE.getFurnitureStore(block);
            if (store != null) {
                var customItem = GardensFurniture.itemRegistry.getItem(store.getItemIdentifier());
                FurnitureStoreHandler.INSTANCE.removeFurnitureStore(block);
                if (block.getType() != Material.AIR) {
                    block.setType(Material.AIR, false);
                }
                removeLight(loc, customItem);
            }
        } catch (Exception ex) {
            GardensFurniture.plugin.getLogger().log(Level.WARNING, "Prism furniture teardown failed at " + loc, ex);
        } finally {
            FurnitureStoreHandler.suppressLifecycle.set(false);
        }
    }

    private static void rebuild(Activity activity) {
        Block block = blockOf(activity);
        if (block == null) return;
        String descriptor = activity.action().descriptor();
        Spec spec = decode(descriptor);
        if (spec == null) return;

        var customItem = GardensFurniture.itemRegistry.getItem(spec.identifier);
        if (!(customItem instanceof CustomPlaceableItem placeable)) {
            GardensFurniture.plugin.getLogger().warning("Prism rebuild: '" + spec.identifier + "' is not placeable furniture.");
            return;
        }
        Location loc = block.getLocation();
        ItemStack item = itemFromActivity(activity);

        FurnitureStoreHandler.suppressLifecycle.set(true);
        try {
            switch (spec.kind) {
                case BLOCK -> rebuildBlock(block, loc, placeable, spec, item);
                case DISPLAY -> placeable.placeBlock(null, item, loc, displayOptions(spec), null);
                case HYBRID -> {
                    // Set the sapling's base block, then let the sapling re-add its store + display model.
                    if (spec.blockData != null && !spec.blockData.isEmpty()) {
                        block.setBlockData(Bukkit.createBlockData(spec.blockData), false);
                    }
                    if (placeable.getCustomBlock() instanceof CustomSaplingBlock sapling) {
                        sapling.onSaplingPlace(null, item, placeable, loc);
                    }
                }
            }
        } catch (Exception ex) {
            GardensFurniture.plugin.getLogger().log(Level.WARNING, "Prism furniture rebuild failed at " + loc, ex);
        } finally {
            FurnitureStoreHandler.suppressLifecycle.set(false);
        }
    }

    private static void rebuildBlock(Block block, Location loc, CustomPlaceableItem placeable, Spec spec, ItemStack item) {
        // Prefer the item's own placement: it derives the correct block state (note pitch, leaf type,
        // etc.) from the CustomBlock definition and handles the store + light. This avoids depending on
        // a stored blockData that, for break records, may have been captured after the block became air.
        boolean placed = placeable.placeBlock(null, item, loc, options(spec), null);
        if (placed) {
            return;
        }
        // "Natural placement" types (e.g. waxed copper) don't set the block themselves — restore the
        // exact captured block state and re-add the store. Orientation only survives if the capture
        // happened while the block still existed (i.e. from a place record).
        if (spec.blockData != null && !spec.blockData.isEmpty()) {
            block.setBlockData(Bukkit.createBlockData(spec.blockData), false);
        }
        FurnitureStoreHandler.INSTANCE.addFurnitureStore(
                block, new FurnitureStore(spec.identifier, spec.multiplier, spec.facing), true);
        CustomBlock customBlock = placeable.getCustomBlock();
        customBlock.placeLight(loc);
    }

    private static PlaceOptions options(Spec spec) {
        var options = new PlaceOptions().setForce(true).setMultiplier(spec.multiplier);
        if (spec.facing != null) {
            options.setFacing(spec.facing);
        }
        return options;
    }

    private static PlaceOptions displayOptions(Spec spec) {
        var options = new PlaceOptions().setForce(true);
        if (spec.facing != null) {
            try {
                options.setYaw(Float.parseFloat(spec.facing));
            } catch (NumberFormatException ignored) {
                // recorded before yaw was stored - keep the default rotation
            }
        }
        return options;
    }

    private static void removeLight(Location loc, CustomItem customItem) {
        if (customItem instanceof CustomPlaceableItem placeable) {
            placeable.getCustomBlock().removeLight(loc);
        }
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    private static boolean suppressed() {
        return Boolean.TRUE.equals(FurnitureStoreHandler.suppressLifecycle.get());
    }

    private static Block blockOf(Activity activity) {
        World world = Bukkit.getWorld(activity.worldUuid());
        if (world == null) return null;
        var c = activity.coordinate();
        return world.getBlockAt(c.intX(), c.intY(), c.intZ());
    }

    private ItemStack displayItemAt(Location loc) {
        var display = CustomBlockManager.getDisplayEntity(loc, true);
        return display != null ? display.getItemStack() : null;
    }

    private static Location displayLoc(ItemDisplay display) {
        return display == null ? null : display.getLocation();
    }

    /** Base block a display sits on. The interaction hitbox is spawned at block-centre; the display is offset up. */
    private static Block baseBlock(ItemDisplay display, Interaction interaction) {
        if (interaction != null) {
            return interaction.getLocation().getBlock();
        }
        return display.getLocation().clone().subtract(0, 0.5, 0).getBlock();
    }

    private static ItemStack itemFromActivity(Activity activity) {
        Action action = activity.action();
        if (action instanceof ItemAction itemAction) {
            try {
                String nbt = itemAction.serializeItemData();
                if (nbt != null && !nbt.isEmpty()) {
                    return NBT.itemStackFromNBT(NBT.parseNBT(nbt));
                }
            } catch (Exception ex) {
                GardensFurniture.plugin.getLogger().log(Level.WARNING, "Prism: failed to read stored furniture item", ex);
            }
        }
        return null;
    }

    // Descriptor codec: "gf1|<kind>|b64(id)|b64(facing)|<multiplier>|b64(blockdata)".
    private static String encode(Kind kind, String identifier, String facing, int multiplier, String blockData) {
        return String.join("|",
                DESCRIPTOR_VERSION,
                kind.name(),
                b64(identifier),
                b64(facing),
                Integer.toString(multiplier),
                b64(blockData));
    }

    private static Spec decode(String descriptor) {
        if (descriptor == null) return null;
        String[] p = descriptor.split("\\|", -1);
        if (p.length != 6 || !DESCRIPTOR_VERSION.equals(p[0])) return null;
        try {
            var spec = new Spec();
            spec.kind = Kind.valueOf(p[1]);
            spec.identifier = unb64(p[2]);
            spec.facing = unb64(p[3]);
            spec.multiplier = Integer.parseInt(p[4]);
            spec.blockData = unb64(p[5]);
            return spec;
        } catch (Exception ex) {
            return null;
        }
    }

    private static String b64(String s) {
        return s == null ? "" : Base64.getEncoder().encodeToString(s.getBytes(StandardCharsets.UTF_8));
    }

    private static String unb64(String s) {
        return s == null || s.isEmpty() ? null : new String(Base64.getDecoder().decode(s), StandardCharsets.UTF_8);
    }

    private static final class Spec {
        Kind kind;
        String identifier;
        String facing;
        int multiplier;
        String blockData;
    }
}
