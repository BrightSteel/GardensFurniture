package com.gardensmc.gardensfurniture;

import com.gardensmc.gardensfurniture.store.FurnitureCache;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.gardensmc.gardensfurniture.command.CommandHandler;
import com.gardensmc.gardensfurniture.custom.item.mechanics.AirborneBreakPenaltyHandler;
import com.gardensmc.gardensfurniture.dye.DyeColorGroups;
import com.gardensmc.gardensfurniture.hook.HookGardensFurniture;
import com.gardensmc.gardensfurniture.hook.PrismHook;
import com.gardensmc.gardensfurniture.listener.ListenerHandler;
import com.gardensmc.gardensfurniture.loader.ChunkFurnitureLoader;
import com.gardensmc.gardensfurniture.mapletap.MapleTapHandler;
import com.gardensmc.gardensfurniture.recipe.CustomFuelHandler;
import com.gardensmc.gardensfurniture.recipe.RecipeManager;
import com.gardensmc.gardensfurniture.register.ItemRegistry;
import com.gardensmc.gardensfurniture.util.ConfigUtil;
import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.settings.PacketEventsSettings;
import com.hibiscusmc.hmccosmetics.HMCCosmeticsPlugin;
import io.github.retrooper.packetevents.factory.spigot.SpigotPacketEventsBuilder;
import me.lojosho.hibiscuscommons.hooks.Hooks;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.plugin.java.JavaPlugin;
import org.geysermc.event.subscribe.Subscribe;
import org.geysermc.geyser.api.GeyserApi;
import org.geysermc.geyser.api.event.EventRegistrar;
import org.geysermc.geyser.api.event.lifecycle.GeyserDefineCustomItemsEvent;
import org.geysermc.geyser.api.item.custom.v2.CustomItemBedrockOptions;
import org.geysermc.geyser.api.item.custom.v2.CustomItemDefinition;
import org.geysermc.geyser.api.item.custom.v2.component.java.JavaConsumable;
import org.geysermc.geyser.api.item.custom.v2.component.java.JavaEquippable;
import org.geysermc.geyser.api.item.custom.v2.component.java.JavaFoodProperties;
import org.geysermc.geyser.api.item.custom.v2.component.java.JavaItemDataComponents;
import org.geysermc.geyser.api.util.Identifier;

import java.util.logging.Level;

public class GardensFurniture extends JavaPlugin implements EventRegistrar {

    public static GardensFurniture plugin;

    public static ItemRegistry itemRegistry;

    public static DyeColorGroups dyeColorGroups;

    public static ProtocolManager protocolManager;
    public static boolean isGriefPreventionHooked;
    public static boolean isCosmeticsHooked;

    @Override
    public void onLoad() {
        loadPacketEvents();
        // Register the Geyser listener in onLoad, before Geyser fires its lifecycle define
        // events in GeyserImpl.start(). Registering in onEnable is too late on /reload (and some
        // startup paths) because we depend on Geyser, so Geyser enables/starts before we do.
        // Geyser registers its API during its own onLoad, which runs first thanks to the depend.
        GeyserApi.api().eventBus().register(this, this);
    }

    private void loadPacketEvents() {
        PacketEvents.setAPI(SpigotPacketEventsBuilder
                .build(this, new PacketEventsSettings().checkForUpdates(false)));
        PacketEvents.getAPI().load();
    }

    @Override
    public void onEnable() {
        plugin = this;
        protocolManager = ProtocolLibrary.getProtocolManager();

        // load custom item configs
        ConfigUtil.loadConfigs();
        dyeColorGroups = new DyeColorGroups(ConfigUtil.dyeColors);

        // load registry
        itemRegistry = new ItemRegistry();
        itemRegistry.assembleItems();
        ChunkFurnitureLoader.INSTANCE.validateMappings();
        // after the items: recipes are checked against them
        RecipeManager.INSTANCE.reload();

        // register handlers
        CommandHandler.registerCommands();
        ListenerHandler.registerListeners();
        FurnitureCache.INSTANCE.loadAllLoadedChunks();

        // init packet events
        PacketEvents.getAPI().init();

        MapleTapHandler.INSTANCE.trackLoadedTaps();
        Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, MapleTapHandler.INSTANCE::tick, 0, 20);

        // hook into hmc cosmetics
        // this is kind of hacky...
        var hook = new HookGardensFurniture();
        hook.setDetected(true);
        hook.setActive(true);
        Hooks.addHook(hook);

        // toggles the airborne block-break-speed buff for items with the disable_airborn_break_penalty mechanic
        Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, AirborneBreakPenaltyHandler.INSTANCE::tick, 0, 1);

        Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, ChunkFurnitureLoader.INSTANCE::tickChunkFurnitureLoader, 0, 1);
        Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, ChunkFurnitureLoader.INSTANCE::tickChunkStats, 0, 100);
        Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, CustomFuelHandler.INSTANCE::tick, 0, 1);

        hookIntoGriefPrevention();
        hookIntoCosmetics();
        hookIntoPrism();
    }

    private void hookIntoPrism() {
        // Guard here (before touching PrismHook) so the Prism API classes it references are never
        // loaded when Prism is absent, avoiding a NoClassDefFoundError.
        if (getServer().getPluginManager().getPlugin("Prism") == null) {
            getLogger().info("Prism not found — furniture rollback/restore integration disabled.");
            return;
        }
        var prismHook = new PrismHook();
        if (prismHook.init()) {
            Bukkit.getPluginManager().registerEvents(prismHook, this);
        }
    }

    @Subscribe
    public void onGeyserDefineCustomItems(GeyserDefineCustomItemsEvent event) {
        System.out.println("Geyser defining custom events subscription ran");
        System.out.println(itemRegistry.getCustomItems().size());
        itemRegistry.getCustomItems().entrySet()
                .stream()
                .filter(entry -> entry.getValue().isBedrockItem())
                .forEach(entry -> {
                    var customItem = entry.getValue();
                    var components = customItem.getComponents();
                    if (components == null) {
                        GardensFurniture.plugin.getLogger().log(Level.SEVERE, "No component defined for bedrock item, ignoring: " + entry.getKey());
                        return;
                    }
                    var itemModel = components.getItem_model();
                    if (itemModel == null) {
                        GardensFurniture.plugin.getLogger().log(Level.SEVERE, "No item model defined for bedrock item, ignoring: " + entry.getKey());
                        return;
                    }
                    var definition = CustomItemDefinition.builder(
                            Identifier.of("gardens", entry.getKey()),
                            Identifier.of(itemModel)
                    );
                    var bedrockOptions = CustomItemBedrockOptions.builder();
                    if (isSwordOrTool(customItem.getMaterial())) {
                        bedrockOptions.displayHandheld(true);
                    }
                    definition.bedrockOptions(bedrockOptions);
                    var maxStackSize = components.getMax_stack_size();
                    if (maxStackSize != null) {
                        definition.component(JavaItemDataComponents.MAX_STACK_SIZE, maxStackSize);
                    }
                    var enchantmentGlintOverride = components.getEnchantment_glint_override();
                    if (enchantmentGlintOverride != null) {
                        definition.component(JavaItemDataComponents.ENCHANTMENT_GLINT_OVERRIDE, enchantmentGlintOverride);
                    }
                    var food = components.getFood();
                    if (food != null) {
                        definition.component(JavaItemDataComponents.FOOD, JavaFoodProperties.builder()
                                .nutrition(food.getNutrition())
                                .saturation((float) food.getSaturation())
                                .canAlwaysEat(food.isCan_always_eat())
                                .build());
                    }
                    var consumable = components.getConsumable();
                    if (consumable != null) {
                        JavaConsumable.Animation animation;
                        try {
                            animation = JavaConsumable.Animation.valueOf(consumable.getAnimation());
                        } catch (IllegalArgumentException e) {
                            GardensFurniture.plugin.getLogger().log(Level.WARNING, "Consumable animation '" + consumable.getAnimation() + "' is not supported by Geyser, skipping consumable component for bedrock item: " + entry.getKey());
                            animation = null;
                        }
                        if (animation != null) {
                            definition.component(JavaItemDataComponents.CONSUMABLE, JavaConsumable.builder()
                                    .consumeSeconds((float) consumable.getConsume_seconds())
                                    .animation(animation)
                                    .build());
                        }
                    }
                    var equippable = components.getEquippable();
                    if (equippable != null && equippable.getSlot() != null) {
                        JavaEquippable.EquipmentSlot slot;
                        try {
                            slot = JavaEquippable.EquipmentSlot.valueOf(equippable.getSlot().name());
                        } catch (IllegalArgumentException e) {
                            GardensFurniture.plugin.getLogger().log(Level.WARNING, "Equippable slot '" + equippable.getSlot().name() + "' is not supported by Geyser, skipping equippable component for bedrock item: " + entry.getKey());
                            slot = null;
                        }
                        if (slot != null) {
                            definition.component(JavaItemDataComponents.EQUIPPABLE, JavaEquippable.of(slot));
                        }
                    }
                    System.out.println("Registered geyser item: " + entry.getKey());
                    event.register(Identifier.of(customItem.getMaterialName()), definition.build());
                });
    }

    // swords/tools are rendered in third person in the bedrock hand, so mark them handheld
    private static boolean isSwordOrTool(Material material) {
        var name = material.name();
        return name.endsWith("_SWORD")
                || name.endsWith("_PICKAXE")
                || name.endsWith("_AXE")
                || name.endsWith("_SHOVEL")
                || name.endsWith("_HOE");
    }

    private void hookIntoGriefPrevention() {
        if (Bukkit.getPluginManager().getPlugin("GriefPrevention") != null) {
            isGriefPreventionHooked = true;
        }
    }

    private void hookIntoCosmetics() {
        if (Bukkit.getPluginManager().getPlugin("HMCCosmetics") != null) {
            isCosmeticsHooked = true;
            // reload cosmetics now that our items are registered
            HMCCosmeticsPlugin.setup();
        }
    }
}
