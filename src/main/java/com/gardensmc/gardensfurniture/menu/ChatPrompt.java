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

    private record Waiting(Consumer<String> answer, Runnable onCancel, Runnable onQuit) {
    }

    private final Map<UUID, Waiting> waiting = new ConcurrentHashMap<>();

    private ChatPrompt() {
    }

    /** Closes the player's menu and waits for their next chat message; "cancel" gives up. */
    public void ask(Player player, String question, Consumer<String> answer) {
        ask(player, question, answer, () -> {
        }, () -> {
        });
    }

    /** As {@link #ask(Player, String, Consumer)}, told when they type cancel or leave without answering. */
    public void ask(Player player, String question, Consumer<String> answer, Runnable onCancel, Runnable onQuit) {
        waiting.put(player.getUniqueId(), new Waiting(answer, onCancel, onQuit));
        player.closeInventory();
        player.sendMessage(MiniMessage.miniMessage().deserialize("<gold>" + question + " <gray>(type <white>cancel</white> to stop)"));
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onChat(AsyncChatEvent e) {
        var prompt = waiting.remove(e.getPlayer().getUniqueId());
        if (prompt == null) return;
        e.setCancelled(true);
        var text = PlainTextComponentSerializer.plainText().serialize(e.message()).trim();
        if (text.equalsIgnoreCase("cancel")) {
            e.getPlayer().sendMessage(MiniMessage.miniMessage().deserialize("<gray>Cancelled."));
            Bukkit.getScheduler().runTask(GardensFurniture.plugin, prompt.onCancel());
            return;
        }
        Bukkit.getScheduler().runTask(GardensFurniture.plugin, () -> prompt.answer().accept(text));
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        var prompt = waiting.remove(e.getPlayer().getUniqueId());
        if (prompt != null) prompt.onQuit().run();
    }
}
