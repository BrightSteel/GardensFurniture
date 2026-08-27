package com.gardensmc.gardensfurniture.listener;

import com.gardensmc.gardensfurniture.breaking.CustomBreakingBlock;
import com.gardensmc.gardensfurniture.breaking.SoundBreakingBlock;
import com.gardensmc.gardensfurniture.custom.CustomBlockManager;
import com.gardensmc.gardensfurniture.custom.item.CustomPlaceableItem;
import com.gardensmc.gardensfurniture.listener.sounds.VanillaBlockSoundListener;
import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.Tool;
import io.papermc.paper.registry.RegistryKey;
import io.papermc.paper.registry.set.RegistrySet;
import io.papermc.paper.registry.set.RegistryKeySet;
import net.kyori.adventure.util.TriState;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.SoundGroup;
import org.bukkit.block.Block;
import org.bukkit.block.BlockType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockDamageAbortEvent;
import org.bukkit.event.block.BlockDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.jspecify.annotations.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

public class BlockBreakingListener implements Listener {

    public static final Map<UUID, CustomBreakingBlock> customBreakingBlocks = new HashMap<>();
    public static final Map<Block, SoundBreakingBlock> soundBreakingBlocks = new HashMap<>();

    // True when the player currently has a barrier break in progress at this exact position that is
    // being shown as the invisible break block. Lets BlockPacketListener rewrite any barrier block
    // update the server sends for that position (e.g. its own dig-rejection re-send) back to the
    // break block, so the crack animation isn't clobbered mid-break.
    public static boolean isBreakingBarrierAt(UUID playerUUID, int x, int y, int z) {
        var breakingBlock = customBreakingBlocks.get(playerUUID);
        return breakingBlock != null && breakingBlock.isSwappedToBreakBlock() && breakingBlock.isAtBlock(x, y, z);
    }

    @EventHandler
    public void onBeginBreakCustomBlock(BlockDamageEvent e) {
        if (e.getPlayer().getGameMode() == GameMode.CREATIVE) {
            return;
        }
        var block = e.getBlock();
        // Runs for any custom block that declares material_traits (note blocks, barriers, ...).
        // Mining Fatigue holds back the client's own dig so our timer controls the real break
        // speed; the barrier-only leaf swap is handled separately inside CustomBreakingBlock.
        Material hardnessMaterial = null;
        if (CustomBlockManager.getCustomItemFromBlock(block) instanceof CustomPlaceableItem placeable) {
            hardnessMaterial = placeable.getCustomBlock().getMaterialTraits();
        }
        if (hardnessMaterial == null) {
            // We suppress client-side break prediction for SUPPRESSED_MATERIALS, so the client never
            // finishes the dig on its own and the server must drive every break of those types - even
            // real (non-furniture) ones, otherwise they'd crawl. Fall back to the block's own material
            // so it still breaks at its normal vanilla speed.
            if (SUPPRESSED_MATERIALS.contains(block.getType())) {
                hardnessMaterial = block.getType();
            } else {
                return;
            }
        }
        var player = e.getPlayer();
        var heldItem = e.getItemInHand();
        var playerUUID = player.getUniqueId();
        customBreakingBlocks.put(playerUUID, new CustomBreakingBlock(player, block, heldItem, hardnessMaterial));
    }

    // the hit-sound loop can play a little past the moment the block actually breaks
    // (the finish/abort event normally closes it first); this is only the fallback ceiling.
    private static final int SOUND_BUFFER_TICKS = 20;
    // hard ceiling so a runaway loop can never exceed this, regardless of computed break time.
    private static final int SOUND_MAX_TICKS = 200;

    @EventHandler
    public void onBeginBreakBlock(BlockDamageEvent e) {
        if (e.getInstaBreak()) {
            return;
        }
        var block = e.getBlock();
        var soundOverride = VanillaBlockSoundListener.getSoundToPlay(block, "hit");
        if (soundOverride == null || soundBreakingBlocks.containsKey(block)) {
            return;
        }
        var player = e.getPlayer();
        var breakTicks = CustomBreakingBlock.calculateBreakTimeInTicks(player, e.getItemInHand(), getHardnessMaterial(block));
        var maxTicks = Math.min(SOUND_MAX_TICKS, breakTicks + SOUND_BUFFER_TICKS);
        soundBreakingBlocks.put(block, new SoundBreakingBlock(block, soundOverride, player.getUniqueId(), maxTicks));
    }

    // the material whose hardness/tool rules drive the break time: the custom block's
    // material traits when present, otherwise the vanilla block's own type.
    private static Material getHardnessMaterial(Block block) {
        if (CustomBlockManager.getCustomItemFromBlock(block) instanceof CustomPlaceableItem placeable) {
            var traits = placeable.getCustomBlock().getMaterialTraits();
            if (traits != null) {
                return traits;
            }
        }
        return block.getType();
    }

    // The client predicts block breaking locally from the real block it sees and the tool in its hand.
    // For a low-hardness furniture block (e.g. a note block that's "really stone") with a high-efficiency
    // tool, the client finishes the break in ~1 tick - before any server-applied Mining Fatigue packet can
    // reach it - then the server rejects it and re-sends the block, producing a repeating break loop.
    // To stop that we patch the *client's* copy of the held item (via packet only - the real item is never
    // touched) with a tool rule that gives the simulated block types a near-zero mining speed, so the client
    // never predicts a fast break and the server stays the sole authority on when the block actually breaks.
    private static final float SUPPRESSED_MINING_SPEED = 0.00001f;
    private static RegistryKeySet<BlockType> suppressedBlocks;

    // Block types whose client-side break prediction we suppress (the client would otherwise mispredict and
    // insta-break them before the server-driven timer finishes). Real (non-furniture) blocks of these types
    // are force-broken by the server at their own vanilla speed (see onBeginBreakCustomBlock) so they don't
    // become unbreakable. Keep this list tight - a suppressed type is server-driven for everyone.
    //   NOTE_BLOCK   - the classic "really stone" furniture.
    //   BIRCH_LEAVES - the invisible block a barrier is swapped to during its break; must be suppressed too,
    //                  or leaf tools (shears) shred it out from under the crack animation. Other leaf types
    //                  stay excluded so normal tree-chopping is untouched.
    private static final List<Material> SUPPRESSED_MATERIALS = List.of(Material.NOTE_BLOCK, Material.BIRCH_LEAVES);

    // Built lazily because the block registry isn't ready at class-load time.
    private static RegistryKeySet<BlockType> getSuppressedBlocks() {
        if (suppressedBlocks == null) {
            var blockTypes = SUPPRESSED_MATERIALS.stream()
                    .map(Material::asBlockType)
                    .filter(Objects::nonNull)
                    .toList();
            suppressedBlocks = RegistrySet.keySetFromValues(RegistryKey.BLOCK, blockTypes);
        }
        return suppressedBlocks;
    }

    // A client-only clone of the held item with our near-zero-speed rule prepended. The rule must come first
    // so it wins over the tool's own mineable rules (an axe otherwise mines note blocks/wood at full speed).
    // Callers must always pass the real (unpatched) server item so the rule isn't duplicated on repeat patches.
    public static ItemStack withBreakSuppressionTool(ItemStack held) {
        if (held == null || held.getType() == Material.AIR) {
            return held;
        }
        var clone = held.clone();
        var builder = Tool.tool().addRule(Tool.rule(getSuppressedBlocks(), SUPPRESSED_MINING_SPEED, TriState.NOT_SET));
        var existing = clone.getData(DataComponentTypes.TOOL);
        if (existing != null) {
            builder.defaultMiningSpeed(existing.defaultMiningSpeed())
                    .damagePerBlock(existing.damagePerBlock())
                    .canDestroyBlocksInCreative(existing.canDestroyBlocksInCreative())
                    .addRules(existing.rules());
        }
        clone.setData(DataComponentTypes.TOOL, builder.build());
        return clone;
    }

    @EventHandler
    public void onCancelBreak(BlockDamageAbortEvent e) {
        removeBreaking(e.getPlayer(), e.getBlock());
    }

    @EventHandler
    public void onFinishBreak(BlockBreakEvent e) {
        if (isStillBreaking(e.getPlayer(), e.getBlock())) {
            e.setCancelled(true);
            return;
        }
        removeBreaking(e.getPlayer(), e.getBlock());
    }

//    @EventHandler
//    public void onPlayerQuit(PlayerQuitEvent e) {
//        removeBreaking(e.getPlayer());
//    }
//
//    @EventHandler
//    public void onPlayerTeleport(PlayerTeleportEvent e) {
//        removeBreaking(e.getPlayer());
//    }
//
//    @EventHandler
//    public void onPlayerChangeWorld(PlayerChangedWorldEvent e) {
//        removeBreaking(e.getPlayer());
//    }

    // A dig can end without a BlockDamageAbortEvent or BlockBreakEvent ever firing
    // (quit, teleport, world change, death). These close any lingering break state for that
    // player - both the hit-sound loop and the custom break loop (which would otherwise keep
    // ticking and leave Mining Fatigue applied after the dig has really ended).
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent e) {
        closeBreaking(e.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onPlayerTeleport(PlayerTeleportEvent e) {
        closeBreaking(e.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onPlayerChangeWorld(PlayerChangedWorldEvent e) {
        closeBreaking(e.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent e) {
        closeBreaking(e.getEntity().getUniqueId());
    }

    private void closeBreaking(UUID playerUUID) {
        var customBreakingBlock = customBreakingBlocks.get(playerUUID);
        if (customBreakingBlock != null) {
            customBreakingBlock.close();
        }
        // collect first: close() mutates soundBreakingBlocks
        soundBreakingBlocks.values().stream()
                .filter(soundBreakingBlock -> playerUUID.equals(soundBreakingBlock.getPlayerUUID()))
                .toList()
                .forEach(SoundBreakingBlock::close);
    }

    private void removeBreaking(Player player, Block block) {
        var breakingBlock = customBreakingBlocks.get(player.getUniqueId());
        if (breakingBlock != null) {
            breakingBlock.close();
        }
        var soundBreakingBlock = soundBreakingBlocks.get(block);
        if (soundBreakingBlock != null) {
            soundBreakingBlock.close();
        }
    }

    private boolean isStillBreaking(Player player, Block block) {
        var breakingBlock = customBreakingBlocks.get(player.getUniqueId());
        return breakingBlock != null && breakingBlock.isBreakingInProgress();
    }

}
