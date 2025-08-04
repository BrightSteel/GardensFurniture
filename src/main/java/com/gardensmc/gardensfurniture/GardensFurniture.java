package com.gardensmc.gardensfurniture;

import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.fastasyncworldedit.core.event.extent.PasteEvent;
import com.gardensmc.gardensfurniture.command.CommandHandler;
import com.gardensmc.gardensfurniture.custom.item.CustomPlaceableItem;
import com.gardensmc.gardensfurniture.hook.GardensWorldEditExtent;
import com.gardensmc.gardensfurniture.hook.HookGardensFurniture;
import com.gardensmc.gardensfurniture.hook.ShopGuiPlusHook;
import com.gardensmc.gardensfurniture.listener.ListenerHandler;
import com.gardensmc.gardensfurniture.listener.sounds.DigSoundListener;
import com.gardensmc.gardensfurniture.loader.ChunkFurnitureLoader;
import com.gardensmc.gardensfurniture.mapletap.MapleTapHandler;
import com.gardensmc.gardensfurniture.register.ItemRegistry;
import com.gardensmc.gardensfurniture.structure.StructureHandler;
import com.gardensmc.gardensfurniture.util.ConfigUtil;
import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.settings.PacketEventsSettings;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.event.extent.EditSessionEvent;
import com.sk89q.worldedit.util.eventbus.Subscribe;
import io.github.retrooper.packetevents.factory.spigot.SpigotPacketEventsBuilder;
import me.lojosho.hibiscuscommons.hooks.Hooks;
import me.ryanhamshire.GriefPrevention.GriefPrevention;
import net.coreprotect.CoreProtect;
import net.coreprotect.CoreProtectAPI;
import org.bukkit.Bukkit;
import org.bukkit.block.data.type.NoteBlock;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

public class GardensFurniture extends JavaPlugin {

    public static GardensFurniture plugin;

    public static ItemRegistry itemRegistry;

    public static ProtocolManager protocolManager;
    public static boolean isGriefPreventionHooked;
    public static CoreProtectAPI coreProtectAPI;

    @Override
    public void onLoad() {
        loadPacketEvents();
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
        new DigSoundListener().registerListener();

        // load custom item configs
        ConfigUtil.loadConfigs();

        // load registry
        itemRegistry = new ItemRegistry();
        itemRegistry.assembleItems();

        // register handlers
        CommandHandler.registerCommands();
        ListenerHandler.registerListeners();

        // init packet events
        PacketEvents.getAPI().init();

        Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, MapleTapHandler.INSTANCE::tick, 0, 20);

        // hook into hmc cosmetics
        // this is kind of hacky...
        var hook = new HookGardensFurniture();
        hook.setDetected(true);
        hook.setActive(true);
        Hooks.addHook(hook);

        Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, ChunkFurnitureLoader.INSTANCE::tickChunkFurnitureLoader, 0, 1);
        Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, ChunkFurnitureLoader.INSTANCE::tickChunkStats, 0, 100);

        WorldEdit.getInstance().getEventBus().register(new Object() {
            @Subscribe
            public void onEditSession(EditSessionEvent event) {
                if (event.getWorld() == null) return;
                event.setExtent(new GardensWorldEditExtent(event.getExtent(), Bukkit.getWorld(event.getWorld().getName())));
            }
        });
        hookIntoShopGUIPlus();
        hookIntoGriefPrevention();
        hookIntoCoreProtect();
    }

    private void hookIntoShopGUIPlus() {
        if (Bukkit.getPluginManager().getPlugin("ShopGUIPlus") != null) {
            Bukkit.getPluginManager().registerEvents(new ShopGuiPlusHook(), this);
            this.getLogger().info("Hooked into ShopGUI+");
        }
    }

    private void hookIntoGriefPrevention() {
        if (Bukkit.getPluginManager().getPlugin("GriefPrevention") != null) {
            isGriefPreventionHooked = true;
        }
    }

    private void hookIntoCoreProtect() {
        var plugin = getServer().getPluginManager().getPlugin("CoreProtect");
        // Check that CoreProtect is loaded
        if (!(plugin instanceof CoreProtect)) {
            return;
        }

        // Check that the API is enabled
        var coreProtect = ((CoreProtect) plugin).getAPI();
        if (!coreProtect.isEnabled()) {
            return;
        }
        coreProtectAPI = coreProtect;
    }
}
