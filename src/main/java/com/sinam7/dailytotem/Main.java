package com.sinam7.dailytotem;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitScheduler;
import org.jetbrains.annotations.NotNull;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class Main extends JavaPlugin implements Listener, CommandExecutor {

    private List<UUID> dailyTotemUsed = new ArrayList<>();
    private LocalDate today;

    private TextComponent notUsedFree;
    private TextComponent usedFreeNow;
    private TextComponent usedInventory;
    private TextComponent alreadyUsed;


    @Override
    public void onEnable() {
        Bukkit.getPluginManager().registerEvents(this, this);
        today = LocalDate.now();
        saveDefaultConfig();
        loadConfig();
        LanguageLoader languageLoader = new LanguageLoader(this);
        textInit(languageLoader);
    }

    private void textInit(LanguageLoader languageLoader) {
        notUsedFree = Component.text(languageLoader.get("notRevivedYet"), Style.style(TextColor.fromHexString("#00ff00")));
        usedFreeNow = Component.text(languageLoader.get("useFreeRevive"), Style.style(TextColor.fromHexString("#ff8800")));
        usedInventory = Component.text(languageLoader.get("useInventoryTotem"), Style.style(TextColor.fromHexString("#ff8800")));
        alreadyUsed = Component.text(languageLoader.get("alreadyRevivedToday"), Style.style(TextColor.fromHexString("#ff0000")));
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (command.getName().equalsIgnoreCase("freetotem")) {
            checkDayChanged();
            sender.sendMessage(dailyTotemUsed.contains(((Player) sender).getUniqueId()) ? alreadyUsed : notUsedFree);
            return true;
        } else if (sender.isOp()) {
            sender.sendMessage("config reloaded");
            reloadConfig();
            loadConfig();
            return true;
        }
        return false;
    }


    @SuppressWarnings("unused")
    @EventHandler
    public void onPlayerDeath(EntityDamageEvent event) {
        if (event.getEntity() instanceof Player player) {
            if (event.getFinalDamage() >= player.getHealth()) { // if player died
                reloadConfig();
                PlayerInventory inv = player.getInventory();
                if (inv.getItemInOffHand().getType().equals(Material.TOTEM_OF_UNDYING) || inv.getItemInMainHand().getType().equals(Material.TOTEM_OF_UNDYING)) {
                    // general totem used
                    justBuffScheduler(player);
                } else {
                    int totemSlot = inv.first(Material.TOTEM_OF_UNDYING);
                    if (totemSlot != -1) { // if totem is not in players hand
                        ItemStack totem = inv.getItem(totemSlot);
                        if (totem != null) {
                            useTotem(player, inv, totem);
                            player.sendMessage(usedInventory);
                        }
                    } else { // if player don't have totem
                        checkDayChanged();
                        if (!dailyTotemUsed.contains(player.getUniqueId())) { // if player didn't die today yet
                            useFreeTotem(player);
                            player.sendMessage(usedFreeNow);
                        } else {
                            player.sendMessage(alreadyUsed);
                        }
                    }
                }
            }
        }
    }

    private void useTotem(Player player, PlayerInventory inv, ItemStack totem) {
        inv.removeItem(totem);
        ItemStack handItem = inv.getItemInOffHand().clone();
        inv.setItemInOffHand(totem);
        afterTotemScheduler(player, inv, handItem);
    }

    private void checkDayChanged() {
        if (!today.equals(LocalDate.now())) { // if the day changed, clear used data
            dailyTotemUsed.clear();
            today = LocalDate.now();
        }
    }

    private void useFreeTotem(Player player) {
        PlayerInventory inv = player.getInventory();
        ItemStack handItem = inv.getItemInOffHand().clone();
        inv.setItemInOffHand(new ItemStack(Material.TOTEM_OF_UNDYING));
        dailyTotemUsed.add(player.getUniqueId());
        addToConfig(player);

        afterTotemScheduler(player, inv, handItem);
    }

    public void loadConfig() {
        FileConfiguration config = getConfig();
        today = LocalDate.now();
        List<String> stringList = config.getStringList(today.toString());
        List<UUID> configList = new ArrayList<>();
        for (String s : stringList) {
            UUID uuid = UUID.fromString(s);
            configList.add(uuid);
        }
        dailyTotemUsed = configList;
    }

    private void addToConfig(Player player) {
        FileConfiguration config = getConfig();
        String todayString = LocalDate.now().toString();
        List<String> data = config.getStringList(todayString);
        data.add(player.getUniqueId().toString());
        config.set(todayString, data);
        saveConfig();
    }

    private void afterTotemScheduler(Player player, PlayerInventory inv, ItemStack handItem) {
        BukkitScheduler scheduler = Bukkit.getScheduler();
        scheduler.runTaskLater(this, () -> {
            inv.setItemInOffHand(handItem);
            powerfulBuffs(player);
        }, 1);
    }

    private void justBuffScheduler(Player player) {
        BukkitScheduler scheduler = Bukkit.getScheduler();
        scheduler.runTaskLater(this, () -> powerfulBuffs(player), 1);
    }

    private static void powerfulBuffs(Player player) {
        player.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 20, 15, true, true));
        player.addPotionEffect(new PotionEffect(PotionEffectType.SATURATION, 20, 15, true, true));
    }


}