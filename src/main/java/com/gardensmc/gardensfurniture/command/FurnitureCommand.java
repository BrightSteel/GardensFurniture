package com.gardensmc.gardensfurniture.command;

import com.gardensmc.gardensfurniture.custom.CustomItem;
import com.gardensmc.gardensfurniture.register.CustomRegister;
import com.gardensmc.gardensfurniture.structures.MapleTree;
import com.gardensmc.gardensfurniture.util.ChatUtil;
import com.gardensmc.gardensfurniture.util.ConfigUtil;
import com.gardensmc.gardensfurniture.util.CosmeticUtil;
import com.gardensmc.gardensfurniture.util.PlayerUtil;
import de.tr7zw.nbtapi.NBTItem;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public class FurnitureCommand implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender commandSender, Command command, String s, String[] args) {

        if (!(commandSender.hasPermission("gardens.furniture.all"))) return false;

        if (args.length > 0) {
            switch (args[0]) {
                case "structure" -> {
                    if (args.length > 1 && commandSender instanceof Player player) {
                        switch (args[1].toLowerCase()) {
                            // todo make this coherent and support all structures as you add them
                            case "maple_tree" -> {
                                new MapleTree(player.getLocation()).generate();
                                player.sendMessage(ChatColor.GREEN + "Generated maple tree");
                            }
                        }
                    }
                }
                case "reload" -> {
                    ConfigUtil.loadConfigs();
                    CustomRegister.INSTANCE.reload();
                    commandSender.sendMessage(ChatColor.GREEN + "Reloaded Gardens Furniture!");
                }
                case "get" -> {
                    if (!(commandSender instanceof Player player)) {
                        return false;
                    }

                    if (!(commandSender.hasPermission("gardens.furniture.all"))) return false;
                    if (args.length < 2) {
                        player.sendMessage(ChatColor.RED + "Please enter the item identifier");
                        return true;
                    }

                    String identifier = args[1];
                    ItemStack itemStack = CustomRegister.INSTANCE.itemRegistry.buildItem(identifier);
                    if (itemStack == null) {
                        player.sendMessage(ChatColor.RED + "No item exists by that identifier!");
                        return true;
                    }

                    player.getInventory().addItem(itemStack);
                    player.sendMessage(ChatColor.GREEN + "Received " + identifier);
                }
                case "give" -> {
                   // no feedback, used by console only right now
                    if (args.length < 3) {
                        return false;
                    }
                    String target = args[1];
                    String identifier = args[2];
                    Player player = Bukkit.getServer().getPlayer(target);
                    if (player != null) {
                        ItemStack itemStack = CustomRegister.INSTANCE.itemRegistry.buildItem(identifier);
                        if (itemStack != null) {
                            if (args.length > 3) {
                                try {
                                    int amount = Integer.parseInt(args[3]);
                                    itemStack.setAmount(amount);
                                } catch (Exception ex) {
                                    ex.printStackTrace();
                                }
                            }
                            player.getInventory().addItem(itemStack);
                        }
                    }
                }
                case "info" -> {
                    if (!(commandSender instanceof Player player)) {
                        return false;
                    }

                    ItemStack heldItem = player.getInventory().getItemInMainHand();
                    NBTItem nbtItem = new NBTItem(heldItem);
                    player.sendMessage(nbtItem.toString());
                }
                case "givetoken" -> {
                    // no feedback, used by console only right now
                    if (args.length < 5) {
                        return false;
                    }
                    String target = args[1];
                    // ie) backpack common cosmetic_name
                    String type = args[2];
                    String rarity = args[3];
                    String cosmetic = args[4];

                    Player player = Bukkit.getServer().getPlayer(target);
                    if (player != null) {
                        ItemStack itemStack = CustomRegister.INSTANCE.itemRegistry.buildItem("token_" + type + "_" + rarity);
                        if (itemStack == null) return false;

                        ItemMeta meta = itemStack.getItemMeta();
                        if (meta != null) {
                            meta.setDisplayName(CosmeticUtil.formatName(cosmetic));
                            List<String> lore = new ArrayList<>();
                            lore.add(ChatUtil.translateAll("&7Visit the Tailor Shop near"));
                            lore.add(ChatUtil.translateAll("&7spawn to redeem me!"));
                            meta.setLore(lore);
                            itemStack.setItemMeta(meta);
                        }

                        NBTItem nbtItem = new NBTItem(itemStack);
                        nbtItem.setString("cosmetic", cosmetic);
                        player.getInventory().addItem(nbtItem.getItem());

                    }
                }
                case "redeemtoken" -> {
                    if (args.length < 2) return false;
                    String target = args[1];
                    Player player = Bukkit.getServer().getPlayer(target);
                    if (player != null) {
                        ItemStack token = player.getInventory().getItemInMainHand();
                        NBTItem nbtItem = new NBTItem(token);
                        if (nbtItem.hasKey("cosmetic")) {
                            String cosmetic = nbtItem.getString("cosmetic");
                            PlayerUtil.addPermission(player, "cosmetics." + cosmetic);
                            token.setAmount(token.getAmount() - 1);
                            player.getInventory().setItemInMainHand(token);

                            player.sendMessage(ChatColor.LIGHT_PURPLE + "Redeemed the " + CosmeticUtil.formatName(cosmetic) + "!");
                        }

                    }
                }
            }
        }

        return true;
    }
}
