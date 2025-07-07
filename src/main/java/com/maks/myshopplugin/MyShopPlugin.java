package com.maks.myshopplugin;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.*;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.List;
import java.util.*;
import java.lang.reflect.Method;
import java.lang.reflect.InvocationTargetException;

public class MyShopPlugin extends JavaPlugin implements Listener {

    private static Economy econ = null;
    private Map<String, Integer> rarityValues = new HashMap<>();
    private double levelMultiplier = 1.5; // Default multiplier
    private final boolean DEBUG = false; // Set to true to enable debug messages

    @Override
    public void onEnable() {
        // Integrate with Vault
        if (!setupEconomy()) {
            getLogger().severe("Vault is required for this plugin to work!");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        // Load configuration
        saveDefaultConfig();
        loadRarityValues();

        // Load levelMultiplier from config.yml
        levelMultiplier = getConfig().getDouble("level_multiplier", 1.5);
        if (DEBUG) getLogger().info("Loaded levelMultiplier: " + levelMultiplier);

        // Register events
        Bukkit.getPluginManager().registerEvents(this, this);

        // Register commands
        this.getCommand("shop").setExecutor((sender, command, label, args) -> {
            if (sender instanceof Player) {
                Player player = (Player) sender;
                openShop(player);
                return true;
            }
            sender.sendMessage("Only players can use this command.");
            return false;
        });

        this.getCommand("checklore").setExecutor((sender, command, label, args) -> {
            if (sender instanceof Player) {
                Player player = (Player) sender;
                ItemStack itemInHand = player.getInventory().getItemInMainHand();
                if (itemInHand != null && itemInHand.getType() != Material.AIR) {
                    ItemMeta meta = itemInHand.getItemMeta();
                    if (meta != null && meta.hasLore()) {
                        player.sendMessage(ChatColor.YELLOW + "Item Lore:");
                        for (String loreLine : meta.getLore()) {
                            player.sendMessage(ChatColor.RESET + loreLine);
                        }
                    } else {
                        player.sendMessage(ChatColor.RED + "Item has no lore.");
                    }
                } else {
                    player.sendMessage(ChatColor.RED + "You are not holding any item.");
                }
                return true;
            }
            sender.sendMessage("Only players can use this command.");
            return false;
        });

        getLogger().info("Shop Plugin has been enabled.");
    }

    @Override
    public void onDisable() {
        getLogger().info("Shop Plugin has been disabled.");
    }

    private boolean setupEconomy() {
        if (getServer().getPluginManager().getPlugin("Vault") == null) {
            return false;
        }
        RegisteredServiceProvider<Economy> rsp = getServer()
                .getServicesManager()
                .getRegistration(Economy.class);
        if (rsp == null) {
            return false;
        }
        econ = rsp.getProvider();
        return econ != null;
    }

    public static Economy getEconomy() {
        return econ;
    }

    private void loadRarityValues() {
        FileConfiguration config = getConfig();
        if (config.getConfigurationSection("rarity_values") == null) {
            if (DEBUG) getLogger().severe("No 'rarity_values' section found in config.yml!");
            return;
        }

        for (String rarity : config.getConfigurationSection("rarity_values").getKeys(false)) {
            int value = config.getInt("rarity_values." + rarity, 0);
            rarityValues.put(rarity, value);
            if (DEBUG) getLogger().info("Loaded rarity '" + rarity + "' with value " + value);
        }
    }

    private void openShop(Player player) {
        Inventory shopGui = Bukkit.createInventory(null, 27, ChatColor.GOLD + "Sell Items");

        // Fill GUI with placeholders (slots 18-26)
        for (int i = 18; i < 27; i++) {
            shopGui.setItem(i, createPlaceholder());
        }

        shopGui.setItem(22, createConfirmButton());
        player.openInventory(shopGui);
    }

    private ItemStack createPlaceholder() {
        ItemStack item = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.BLACK + " ");
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack createConfirmButton() {
        ItemStack item = new ItemStack(Material.EMERALD);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.GREEN + "Confirm Sale");
            item.setItemMeta(meta);
        }
        return item;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!event.getView().getTitle().equals(ChatColor.GOLD + "Sell Items")) {
            return;
        }

        if (event.getClickedInventory() == null) {
            return;
        }

        if (event.getClickedInventory().equals(event.getView().getTopInventory())) {
            // Click in shop GUI
            ItemStack clickedItem = event.getCurrentItem();
            if (clickedItem == null) {
                return;
            }

            // Prevent moving placeholder items
            if (isPlaceholderItem(clickedItem)) {
                event.setCancelled(true);
                return;
            }

            // Handle confirm button click
            if (isConfirmButton(clickedItem)) {
                event.setCancelled(true);
                processSale((Player) event.getWhoClicked(), event.getInventory());
                return;
            }

            // Allow players to retrieve their items from the GUI
            event.setCancelled(false);
        } else {
            // Click in player's own inventory
            // Handle shift-click to move items to the shop GUI
            if (event.getAction() == InventoryAction.MOVE_TO_OTHER_INVENTORY) {
                ItemStack itemToMove = event.getCurrentItem();
                if (!isItemSellable(itemToMove)) {
                    event.getWhoClicked().sendMessage(ChatColor.RED + "You cannot sell this item.");
                    event.setCancelled(true);
                    return;
                }
            } else {
                // Prevent placing unsellable items into the shop GUI
                if (event.getCursor() != null && event.getRawSlot() < 27) {
                    if (!isItemSellable(event.getCursor())) {
                        event.getWhoClicked().sendMessage(ChatColor.RED + "You cannot sell this item.");
                        event.setCancelled(true);
                    }
                }
            }
        }
    }

    private boolean isPlaceholderItem(ItemStack item) {
        if (item.getType() == Material.BLACK_STAINED_GLASS_PANE) {
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                String displayName = meta.getDisplayName();
                String expectedName = ChatColor.BLACK + " ";
                return expectedName.equals(displayName);
            }
        }
        return false;
    }

    private boolean isConfirmButton(ItemStack item) {
        if (item.getType() == Material.EMERALD) {
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                String displayName = meta.getDisplayName();
                String expectedName = ChatColor.GREEN + "Confirm Sale";
                return expectedName.equals(displayName);
            }
        }
        return false;
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        if (!event.getView().getTitle().equals(ChatColor.GOLD + "Sell Items")) {
            return;
        }

        // Prevent dragging items into the shop GUI
        for (int slot : event.getRawSlots()) {
            if (slot < 27) {
                if (!isItemSellable(event.getOldCursor())) {
                    event.setCancelled(true);
                    event.getWhoClicked().sendMessage(ChatColor.RED + "You cannot sell this item.");
                    return;
                }
            }
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!event.getView().getTitle().equals(ChatColor.GOLD + "Sell Items")) {
            return;
        }

        Inventory inventory = event.getInventory();
        Player player = (Player) event.getPlayer();

        for (int i = 0; i < 18; i++) { // Slots 0-17 are for items to sell
            ItemStack item = inventory.getItem(i);
            if (item != null && item.getType() != Material.AIR) {
                player.getInventory().addItem(item);
                inventory.setItem(i, null);
                if (DEBUG)
                    getLogger().info("onInventoryClose: Returned unsold item: " + item.getType());
            }
        }
    }

    private void processSale(Player player, Inventory inventory) {
        int totalValue = 0;

        // Create a list to hold items that need to be removed
        List<Integer> slotsToClear = new ArrayList<>();

        for (int i = 0; i < 18; i++) { // Slots 0-17 are for items to sell
            ItemStack item = inventory.getItem(i);
            if (item != null && item.getType() != Material.AIR) {
                if (isItemSellable(item)) {
                    int itemValue = getItemValue(item);
                    totalValue += itemValue;
                    slotsToClear.add(i); // Mark slot for clearing
                    if (DEBUG)
                        getLogger().info("processSale: Sold item " + item.getType() + " for " + itemValue);
                } else {
                    // Return unsellable item to the player
                    HashMap<Integer, ItemStack> leftover = player.getInventory().addItem(item);
                    if (!leftover.isEmpty()) {
                        // If player's inventory is full, drop the items at their location
                        for (ItemStack dropItem : leftover.values()) {
                            player.getWorld().dropItemNaturally(player.getLocation(), dropItem);
                        }
                        player.sendMessage(ChatColor.RED + "Your inventory is full. Unsellable items have been dropped at your feet.");
                    }
                    inventory.setItem(i, null);
                    if (DEBUG)
                        getLogger().info("processSale: Returned unsellable item " + item.getType() + " to player");
                }
            }
        }

        // Clear sold items from the inventory
        for (int slot : slotsToClear) {
            inventory.clear(slot);
        }

        if (totalValue > 0) {
            // Sprawdź czy jest dostępne API TrinketsPlugin
            double sellMultiplier = 1.0;

            try {
                // Próba użycia JewelAPI
                Class<?> jewelAPIClass = Class.forName("com.maks.trinketsplugin.JewelAPI");
                Method getSellBonusMethod = jewelAPIClass.getMethod("getSellBonus", Player.class);
                Object result = getSellBonusMethod.invoke(null, player);
                if (result instanceof Double) {
                    sellMultiplier = (Double) result;
                    if (DEBUG) {
                        getLogger().info("Applied sell bonus from Steam Sale Jewel: " + sellMultiplier);
                    }
                }
            } catch (ClassNotFoundException | NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
                // API Niedostępne, używamy domyślnego mnożnika 1.0
                if (DEBUG) {
                    getLogger().warning("TrinketsPlugin JewelAPI not available: " + e.getMessage());
                }
            }

            // Zastosuj mnożnik do totalValue
            int finalValue = (int) (totalValue * sellMultiplier);

            if (sellMultiplier > 1.0 && DEBUG) {
                getLogger().info("Increased sale value from " + totalValue + " to " + finalValue + 
                        " due to Steam Sale Jewel");
            }

            econ.depositPlayer(player, finalValue);
            player.sendMessage(ChatColor.GREEN + "You received $" + finalValue + " for sold items.");

            if (sellMultiplier > 1.0) {
                player.sendMessage(ChatColor.GOLD + "Your Merchant Jewel gave you a " + 
                        (int)((sellMultiplier - 1.0) * 100) + "% bonus!");
            }

            if (DEBUG)
                getLogger().info("processSale: Total sale value: " + finalValue);
        } else {
            player.sendMessage(ChatColor.RED + "No sellable items were found.");
            if (DEBUG)
                getLogger().info("processSale: No sellable items found.");
        }

        player.closeInventory();
    }


    private boolean isItemSellable(ItemStack item) {
        if (item == null || item.getType() == Material.AIR) {
            if (DEBUG)
                getLogger().info("isItemSellable: Item is null or AIR.");
            return false;
        }
        ItemMeta meta = item.getItemMeta();
        if (meta == null || !meta.hasLore()) {
            if (DEBUG)
                getLogger().info("isItemSellable: Item has no meta or lore.");
            return false;
        }

        for (String line : meta.getLore()) {
            String strippedLine = ChatColor.stripColor(line).trim();
            if (DEBUG)
                getLogger().info("isItemSellable: Checking lore line: '" + strippedLine + "'");
            if (strippedLine.startsWith("Rarity:")) {
                String rarityName = extractRarityName(strippedLine);
                if (rarityValues.containsKey(rarityName)) {
                    if (DEBUG)
                        getLogger().info("isItemSellable: Rarity '" + rarityName + "' found.");
                    return true;
                }
            }
        }
        if (DEBUG)
            getLogger().info("isItemSellable: Item is not sellable.");
        return false;
    }

    private String extractRarityName(String strippedLine) {
        String[] parts = strippedLine.split(":");
        if (parts.length > 1) {
            String rarityPart = parts[1].trim();
            String[] rarityComponents = rarityPart.split(" ");
            return rarityComponents[0]; // Returns the rarity name without '+N'
        }
        return "";
    }

    private int extractRarityLevel(String strippedLine) {
        String[] parts = strippedLine.split("\\+");
        if (parts.length > 1) {
            try {
                return Integer.parseInt(parts[1].trim());
            } catch (NumberFormatException e) {
                if (DEBUG)
                    getLogger().info("extractRarityLevel: Could not parse rarity level from '" + parts[1] + "'");
            }
        }
        return 1; // Default level is 1 if '+N' is not present
    }

    private int getItemValue(ItemStack item) {
        if (!isItemSellable(item)) {
            if (DEBUG)
                getLogger().info("getItemValue: Item is not sellable, value is 0.");
            return 0;
        }

        ItemMeta meta = item.getItemMeta();
        for (String line : meta.getLore()) {
            String strippedLine = ChatColor.stripColor(line).trim();
            if (strippedLine.startsWith("Rarity:")) {
                String rarityName = extractRarityName(strippedLine);
                int rarityLevel = extractRarityLevel(strippedLine);
                Integer baseValue = rarityValues.get(rarityName);
                if (baseValue != null) {
                    // Multiply the base value by the level multiplier raised to the power of rarity level
                    double totalValue = baseValue * Math.pow(levelMultiplier, rarityLevel - 1) * item.getAmount();
                    int finalValue = (int) totalValue;
                    if (DEBUG)
                        getLogger().info("getItemValue: Rarity '" + rarityName + "', base value: " + baseValue +
                                ", level: " + rarityLevel + ", total value: " + finalValue);
                    return finalValue;
                }
            }
        }

        if (DEBUG)
            getLogger().info("getItemValue: No matching rarity found, value is 0.");
        return 0;
    }
}
