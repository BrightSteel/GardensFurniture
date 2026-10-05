package com.gardensmc.gardensfurniture.recipe;

import com.gardensmc.gardensfurniture.custom.item.CustomItem;
import net.minecraft.server.MinecraftServer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Tag;
import org.bukkit.craftbukkit.inventory.CraftItemStack;
import org.bukkit.inventory.ItemStack;

import java.util.*;
import java.util.logging.Logger;

/**
 * Which vanilla item tags GardensFurniture items are in, from their {@code tags:} in items/*.yml.
 *
 * <p>An item in a tag is also in every tag that holds all of that tag's items, the way vanilla's
 * tags nest: one in {@code minecraft:logs_that_burn} is in {@code minecraft:logs} too, and one in
 * {@code minecraft:planks} is in {@code minecraft:wooden_tool_materials}.
 */
final class CustomItemTags {

    static final CustomItemTags EMPTY = new CustomItemTags(Map.of(), Map.of(), Map.of());

    // GardensFurniture id -> every vanilla tag it's in
    private final Map<String, Set<NamespacedKey>> tagsById;
    // vanilla tag -> the GardensFurniture ids in it
    private final Map<NamespacedKey, Set<String>> idsByTag;
    // ticks a furnace burns the item for, as long as the longest-burning vanilla item in its tags
    private final Map<String, Integer> burnTicks;

    private CustomItemTags(Map<String, Set<NamespacedKey>> tagsById, Map<NamespacedKey, Set<String>> idsByTag, Map<String, Integer> burnTicks) {
        this.tagsById = tagsById;
        this.idsByTag = idsByTag;
        this.burnTicks = burnTicks;
    }

    static CustomItemTags build(Collection<CustomItem> items, Logger log) {
        var all = new HashMap<NamespacedKey, Set<Material>>();
        for (var tag : Bukkit.getTags(Tag.REGISTRY_ITEMS, Material.class)) {
            all.put(tag.getKey(), tag.getValues());
        }
        var fuel = MinecraftServer.getServer().fuelValues();
        var tagsById = new HashMap<String, Set<NamespacedKey>>();
        var idsByTag = new HashMap<NamespacedKey, Set<String>>();
        var burnTicks = new HashMap<String, Integer>();
        for (var item : items) {
            if (item.getTags().isEmpty()) continue;
            var tags = new LinkedHashSet<NamespacedKey>();
            int burn = 0;
            for (var name : item.getTags()) {
                var key = NamespacedKey.fromString(name);
                var values = key == null ? null : all.get(key);
                if (values == null || values.isEmpty()) {
                    log.warning("Item '%s' lists tag '%s', which isn't a vanilla item tag".formatted(item.getIdentifier(), name));
                    continue;
                }
                tags.add(key);
                all.forEach((other, otherValues) -> {
                    if (otherValues.containsAll(values)) tags.add(other);
                });
                for (var material : values) {
                    if (material.isItem()) burn = Math.max(burn, fuel.burnDuration(CraftItemStack.asNMSCopy(new ItemStack(material))));
                }
            }
            if (tags.isEmpty()) continue;
            tagsById.put(item.getIdentifier(), tags);
            tags.forEach(tag -> idsByTag.computeIfAbsent(tag, t -> new LinkedHashSet<>()).add(item.getIdentifier()));
            if (burn > 0) burnTicks.put(item.getIdentifier(), burn);
        }
        return new CustomItemTags(tagsById, idsByTag, burnTicks);
    }

    Set<String> idsIn(NamespacedKey tag) {
        return idsByTag.getOrDefault(tag, Set.of());
    }

    Set<NamespacedKey> tagsOf(String id) {
        return tagsById.getOrDefault(id, Set.of());
    }

    /** Tags some GardensFurniture item is in: the ones vanilla recipes are worth copying for. */
    Set<NamespacedKey> usedTags() {
        return idsByTag.keySet();
    }

    int burnTicks(String id) {
        return burnTicks.getOrDefault(id, 0);
    }
}
