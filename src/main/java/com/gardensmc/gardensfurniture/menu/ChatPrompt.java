package com.gardensmc.gardensfurniture.menu;

import com.gardensmc.gardensfurniture.GardensFurniture;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/** Asks a player to type something in chat (a recipe id, a group name) and hands it back on the main thread. */
public class ChatPrompt implements Listener {

    public static final ChatPrompt INSTANCE = new ChatPrompt();

    private final Map<UUID, Consumer<String>> waiting = new ConcurrentHashMap<>();

    private ChatPrompt() {
    }

    /** Closes the player's menu and waits for their next chat message; "cancel" gives up. */
    public void ask(Player player, String question, Consumer<String> answer) {
        player.closeInventory();
        waiting.put(player.getUniqueId(), answer);
        player.sendMessage(MiniMessage.miniMessage().deserialize("<gold>" + question + " <gray>(type <white>cancel</white> to stop)"));
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onChat(AsyncChatEvent e) {
        var answer = waiting.remove(e.getPlayer().getUniqueId());
        if (answer == null) return;
        e.setCancelled(true);
        var text = PlainTextComponentSerializer.plainText().serialize(e.message()).trim();
        if (text.equalsIgnoreCase("cancel")) {
            e.getPlayer().sendMessage(MiniMessage.miniMessage().deserialize("<gray>Cancelled."));
            return;
        }
        Bukkit.getScheduler().runTask(GardensFurniture.plugin, () -> answer.accept(text));
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        waiting.remove(e.getPlayer().getUniqueId());
    }
}
