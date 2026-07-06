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
import java.util.Arrays;
import java.util.Random;
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
                openMainShopGUI(player);
                return true;
            }
            sender.sendMessage("Only players can use this command.");
            return false;
        });

        this.getCommand("smelter").setExecutor((sender, command, label, args) -> {
            if (sender instanceof Player) {
                Player player = (Player) sender;
                openSmelterGUI(player);
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
                    
                    player.sendMessage(ChatColor.YELLOW + "=== ITEM DEBUG ===");
                    player.sendMessage(ChatColor.YELLOW + "Display Name: " + ChatColor.RESET + meta.getDisplayName());
                    player.sendMessage(ChatColor.YELLOW + "Tier: " + ChatColor.RESET + getItemTier(itemInHand));
                    player.sendMessage(ChatColor.YELLOW + "Rarity: " + ChatColor.RESET + getItemRarity(itemInHand));
                    player.sendMessage(ChatColor.YELLOW + "Enhancement: " + ChatColor.RESET + getEnhancementLevel(itemInHand));
                    player.sendMessage(ChatColor.YELLOW + "Is Smeltable: " + ChatColor.RESET + isSmeltableItem(itemInHand));
                    
                    if (meta != null && meta.hasLore()) {
                        player.sendMessage(ChatColor.YELLOW + "Item Lore:");
                        for (int i = 0; i < meta.getLore().size(); i++) {
                            String loreLine = meta.getLore().get(i);
                            String stripped = ChatColor.stripColor(loreLine).trim();
                            player.sendMessage(ChatColor.GRAY + "[" + i + "] " + ChatColor.RESET + loreLine);
                            player.sendMessage(ChatColor.DARK_GRAY + "    Stripped: " + stripped);
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

    private void openMainShopGUI(Player player) {
        Inventory mainGui = Bukkit.createInventory(null, 27, ChatColor.DARK_BLUE + "Shop Services");
        
        for (int i = 0; i < 27; i++) {
            mainGui.setItem(i, createGrayPlaceholder());
        }
        
        ItemStack quickSellButton = new ItemStack(Material.EMERALD);
        ItemMeta quickSellMeta = quickSellButton.getItemMeta();
        quickSellMeta.setDisplayName(ChatColor.GREEN + "Quick Sell");
        quickSellMeta.setLore(Arrays.asList(
            ChatColor.GRAY + "Sell your items quickly",
            ChatColor.GRAY + "for coins based on rarity",
            "",
            ChatColor.YELLOW + "Click to open!"
        ));
        quickSellButton.setItemMeta(quickSellMeta);
        mainGui.setItem(11, quickSellButton);
        
        ItemStack smelterButton = new ItemStack(Material.FURNACE);
        ItemMeta smelterMeta = smelterButton.getItemMeta();
        smelterMeta.setDisplayName(ChatColor.RED + "Item Smelter");
        smelterMeta.setLore(Arrays.asList(
            ChatColor.GRAY + "Smelt tiered items into",
            ChatColor.GRAY + "crafting materials",
            "",
            ChatColor.YELLOW + "Click to open!"
        ));
        smelterButton.setItemMeta(smelterMeta);
        mainGui.setItem(15, smelterButton);
        
        player.openInventory(mainGui);
    }

    private void openShop(Player player) {
        Inventory shopGui = Bukkit.createInventory(null, 27, ChatColor.GOLD + "Sell Items");

        for (int i = 18; i < 27; i++) {
            shopGui.setItem(i, createPlaceholder());
        }

        shopGui.setItem(22, createConfirmButton());
        player.openInventory(shopGui);
    }

    private void openSmelterGUI(Player player) {
        Inventory smelterGui = Bukkit.createInventory(null, 27, ChatColor.DARK_RED + "Item Smelter");
        
        for (int i = 18; i < 27; i++) {
            smelterGui.setItem(i, createPlaceholder());
        }
        
        smelterGui.setItem(22, createSmelterConfirmButton());
        player.openInventory(smelterGui);
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

    private ItemStack createGrayPlaceholder() {
        ItemStack item = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.GRAY + " ");
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

    private ItemStack createSmelterConfirmButton() {
        ItemStack item = new ItemStack(Material.BLAZE_POWDER);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.GOLD + "Smelt Items");
            meta.setLore(Arrays.asList(
                ChatColor.GRAY + "Convert tiered items into",
                ChatColor.GRAY + "crafting materials",
                "",
                ChatColor.RED + "Warning: Items will be consumed!"
            ));
            item.setItemMeta(meta);
        }
        return item;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        String title = event.getView().getTitle();
        
        if (title.equals(ChatColor.DARK_BLUE + "Shop Services")) {
            handleMainShopClick(event);
            return;
        }
        
        if (title.equals(ChatColor.GOLD + "Sell Items")) {
            handleQuickSellClick(event);
            return;
        }
        
        if (title.equals(ChatColor.DARK_RED + "Item Smelter")) {
            handleSmelterClick(event);
            return;
        }
    }
    
    private void handleMainShopClick(InventoryClickEvent event) {
        event.setCancelled(true);
        
        if (event.getClickedInventory() == null || event.getCurrentItem() == null) {
            return;
        }
        
        ItemStack clicked = event.getCurrentItem();
        Player player = (Player) event.getWhoClicked();
        
        if (clicked.getType() == Material.EMERALD) {
            giveTemporaryPermission(player, "shopplugin.use");
            openShop(player);
        } else if (clicked.getType() == Material.FURNACE) {
            giveTemporaryPermission(player, "shopplugin.smelter");
            openSmelterGUI(player);
        }
    }
    
    private void handleQuickSellClick(InventoryClickEvent event) {

        if (event.getClickedInventory() == null) {
            return;
        }

        if (event.getClickedInventory().equals(event.getView().getTopInventory())) {
            ItemStack clickedItem = event.getCurrentItem();
            if (clickedItem == null) {
                return;
            }

            if (isPlaceholderItem(clickedItem)) {
                event.setCancelled(true);
                return;
            }

            if (isConfirmButton(clickedItem)) {
                event.setCancelled(true);
                processSale((Player) event.getWhoClicked(), event.getInventory());
                return;
            }

            event.setCancelled(false);
        } else {
            if (event.getAction() == InventoryAction.MOVE_TO_OTHER_INVENTORY) {
                ItemStack itemToMove = event.getCurrentItem();
                if (!isItemSellable(itemToMove)) {
                    event.getWhoClicked().sendMessage(ChatColor.RED + "You cannot sell this item.");
                    event.setCancelled(true);
                    return;
                }
            } else {
                if (event.getCursor() != null && event.getRawSlot() < 27) {
                    if (!isItemSellable(event.getCursor())) {
                        event.getWhoClicked().sendMessage(ChatColor.RED + "You cannot sell this item.");
                        event.setCancelled(true);
                    }
                }
            }
        }
    }
    
    private void handleSmelterClick(InventoryClickEvent event) {
        if (event.getClickedInventory() == null) {
            return;
        }

        if (event.getClickedInventory().equals(event.getView().getTopInventory())) {
            ItemStack clickedItem = event.getCurrentItem();
            if (clickedItem == null) {
                return;
            }

            if (isPlaceholderItem(clickedItem)) {
                event.setCancelled(true);
                return;
            }

            if (isSmelterConfirmButton(clickedItem)) {
                event.setCancelled(true);
                processSmelt((Player) event.getWhoClicked(), event.getInventory());
                return;
            }

            event.setCancelled(false);
        } else {
            if (event.getAction() == InventoryAction.MOVE_TO_OTHER_INVENTORY) {
                ItemStack itemToMove = event.getCurrentItem();
                if (!isSmeltableItem(itemToMove)) {
                    event.getWhoClicked().sendMessage(ChatColor.RED + "You cannot smelt this item.");
                    event.setCancelled(true);
                    return;
                }
            } else {
                if (event.getCursor() != null && event.getRawSlot() < 27) {
                    if (!isSmeltableItem(event.getCursor())) {
                        event.getWhoClicked().sendMessage(ChatColor.RED + "You cannot smelt this item.");
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
        String title = event.getView().getTitle();
        
        if (title.equals(ChatColor.DARK_BLUE + "Shop Services")) {
            event.setCancelled(true);
            return;
        }
        
        if (title.equals(ChatColor.GOLD + "Sell Items")) {
            for (int slot : event.getRawSlots()) {
                if (slot < 27) {
                    if (!isItemSellable(event.getOldCursor())) {
                        event.setCancelled(true);
                        event.getWhoClicked().sendMessage(ChatColor.RED + "You cannot sell this item.");
                        return;
                    }
                }
            }
            return;
        }
        
        if (title.equals(ChatColor.DARK_RED + "Item Smelter")) {
            for (int slot : event.getRawSlots()) {
                if (slot < 27) {
                    if (!isSmeltableItem(event.getOldCursor())) {
                        event.setCancelled(true);
                        event.getWhoClicked().sendMessage(ChatColor.RED + "You cannot smelt this item.");
                        return;
                    }
                }
            }
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        String title = event.getView().getTitle();
        
        if (title.equals(ChatColor.GOLD + "Sell Items") || title.equals(ChatColor.DARK_RED + "Item Smelter")) {
            Inventory inventory = event.getInventory();
            Player player = (Player) event.getPlayer();

            for (int i = 0; i < 18; i++) {
                ItemStack item = inventory.getItem(i);
                if (item != null && item.getType() != Material.AIR) {
                    player.getInventory().addItem(item);
                    inventory.setItem(i, null);
                    if (DEBUG)
                        getLogger().info("onInventoryClose: Returned unsold item: " + item.getType());
                }
            }
        }
        
        removeTemporaryPermission((Player) event.getPlayer());
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

    private void giveTemporaryPermission(Player player, String permission) {
        if (player.hasPermission(permission)) {
            return;
        }
        
        player.addAttachment(this, permission, true);
        if (DEBUG)
            getLogger().info("Gave temporary permission " + permission + " to " + player.getName());
    }

    private void removeTemporaryPermission(Player player) {
        // Nie robimy nic - uprawnienia tymczasowe same się usuwają gdy gracz się rozłączy
        // lub gdy attachment zostanie usunięty przez serwer
    }

    private boolean isSmelterConfirmButton(ItemStack item) {
        if (item.getType() == Material.BLAZE_POWDER) {
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                String displayName = meta.getDisplayName();
                String expectedName = ChatColor.GOLD + "Smelt Items";
                return expectedName.equals(displayName);
            }
        }
        return false;
    }

    private boolean isSmeltableItem(ItemStack item) {
        if (item == null || item.getType() == Material.AIR) {
            return false;
        }
        
        ItemMeta meta = item.getItemMeta();
        if (meta == null || !meta.hasDisplayName()) {
            return false;
        }
        
        String displayName = meta.getDisplayName();
        String strippedName = ChatColor.stripColor(displayName);
        
        return (strippedName.contains("[ I ]") || strippedName.contains("[ II ]") ||
                strippedName.contains("[ III ]") || strippedName.contains("[ IV ]")) &&
               meta.hasLore() && hasValidRarity(meta.getLore());
    }

    private boolean hasValidRarity(List<String> lore) {
        for (String line : lore) {
            String strippedLine = ChatColor.stripColor(line).trim();
            if (strippedLine.startsWith("Rarity:")) {
                String rarityName = extractRarityName(strippedLine);
                return rarityName.equals("Magic") || rarityName.equals("Extraordinary") ||
                       rarityName.equals("Legendary") || rarityName.equals("Unique") || rarityName.equals("MYTHIC");
            }
        }
        return false;
    }

    private void processSmelt(Player player, Inventory inventory) {
        List<String> materialsObtained = new ArrayList<>();
        List<Integer> slotsToClear = new ArrayList<>();
        
        for (int i = 0; i < 18; i++) {
            ItemStack item = inventory.getItem(i);
            if (item != null && isSmeltableItem(item)) {
                List<String> materials = smeltItem(item);
                materialsObtained.addAll(materials);
                slotsToClear.add(i);
            }
        }
        
        for (int slot : slotsToClear) {
            inventory.clear(slot);
        }
        
        if (!materialsObtained.isEmpty()) {
            addMaterialsToPouch(player, materialsObtained);
            player.sendMessage(ChatColor.GREEN + "Successfully smelted items! Materials added to your pouch:");
            for (String material : materialsObtained) {
                player.sendMessage(ChatColor.GRAY + "- " + material);
            }
        } else {
            player.sendMessage(ChatColor.RED + "No valid items to smelt!");
        }
        
        player.closeInventory();
    }

    private List<String> smeltItem(ItemStack item) {
        List<String> materials = new ArrayList<>();
        
        int tier = getItemTier(item);
        String rarity = getItemRarity(item);
        int enhancement = getEnhancementLevel(item);
        
        if (DEBUG) {
            getLogger().info("Smelting item: " + item.getItemMeta().getDisplayName());
            getLogger().info("Tier: " + tier + ", Rarity: " + rarity + ", Enhancement: " + enhancement);
        }
        
        Map<String, Integer> materialAmounts = calculateMaterials(tier, rarity, enhancement);
        
        for (Map.Entry<String, Integer> entry : materialAmounts.entrySet()) {
            String materialType = entry.getKey();
            int amount = entry.getValue();
            String tieredMaterial = materialType + "_" + getRomanNumeral(tier);
            materials.add(amount + "x " + tieredMaterial);
            
            if (DEBUG) {
                getLogger().info("Material: " + tieredMaterial + " x" + amount);
            }
        }
        
        return materials;
    }

    private int getItemTier(ItemStack item) {
        String displayName = ChatColor.stripColor(item.getItemMeta().getDisplayName());
        if (displayName.contains("[ IV ]")) return 4;
        if (displayName.contains("[ III ]")) return 3;
        if (displayName.contains("[ II ]")) return 2;
        if (displayName.contains("[ I ]")) return 1;
        return 1;
    }

    private String getItemRarity(ItemStack item) {
        for (String line : item.getItemMeta().getLore()) {
            String strippedLine = ChatColor.stripColor(line).trim();
            if (strippedLine.startsWith("Rarity:")) {
                return extractRarityName(strippedLine);
            }
        }
        return "Magic";
    }

    private int getEnhancementLevel(ItemStack item) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null || !meta.hasLore()) {
            if (DEBUG) getLogger().info("Enhancement check: No meta or lore");
            return 0;
        }
        
        // Sprawdź w lore linii z "Rarity:" czy ma enhancement
        for (String line : meta.getLore()) {
            String strippedLine = ChatColor.stripColor(line).trim();
            if (DEBUG) getLogger().info("Enhancement check lore line: '" + strippedLine + "'");
            if (strippedLine.startsWith("Rarity:")) {
                if (strippedLine.contains("+4")) {
                    if (DEBUG) getLogger().info("Found +4 enhancement in rarity line");
                    return 4;
                }
                if (strippedLine.contains("+3")) {
                    if (DEBUG) getLogger().info("Found +3 enhancement in rarity line");
                    return 3;
                }
                if (strippedLine.contains("+2")) {
                    if (DEBUG) getLogger().info("Found +2 enhancement in rarity line");
                    return 2;
                }
                if (strippedLine.contains("+1")) {
                    if (DEBUG) getLogger().info("Found +1 enhancement in rarity line");
                    return 1;
                }
                break;
            }
        }
        
        // Jeśli nie znaleziono w Rarity, sprawdź w nazwie jako fallback
        String displayName = ChatColor.stripColor(meta.getDisplayName());
        if (DEBUG) getLogger().info("Enhancement check display name: '" + displayName + "'");
        if (displayName.contains("+4")) {
            if (DEBUG) getLogger().info("Found +4 enhancement in display name");
            return 4;
        }
        if (displayName.contains("+3")) {
            if (DEBUG) getLogger().info("Found +3 enhancement in display name");
            return 3;
        }
        if (displayName.contains("+2")) {
            if (DEBUG) getLogger().info("Found +2 enhancement in display name");
            return 2;
        }
        if (displayName.contains("+1")) {
            if (DEBUG) getLogger().info("Found +1 enhancement in display name");
            return 1;
        }
        
        if (DEBUG) getLogger().info("No enhancement found, returning 0");
        return 0;
    }

    private Map<String, Integer> calculateMaterials(int tier, String rarity, int enhancement) {
        Map<String, Integer> materials = new HashMap<>();
        Random random = new Random();

        // Oryginalne materiały
        List<String> basicMaterials = Arrays.asList("mob_soul", "ore", "leaf", "alga");
        List<String> rareMaterials = Arrays.asList("elite_heart", "blood", "bone", "pearl");
        List<String> legendaryMaterials = Arrays.asList(
            "grimmag_frag", "arachna_frag", "heredur_frag", "bearach_frag",
            "khalys_frag", "heralds_frag", "sigrismar_frag", "medusa_frag",
            "gorga_frag", "mortis_frag", "ocean_heart"
        );

        // Demon Tower - Basic (wszystkie tiery)
        List<String> demonBasicMaterials = Arrays.asList("demon_ash", "cursed_cloth");

        // Demon Tower - Rare (tier-specific)
        List<String> demonRareMaterials;
        switch (tier) {
            case 1: demonRareMaterials = Arrays.asList("demon_head", "raven_feather", "blacksmith_scrap"); break;
            case 2: demonRareMaterials = Arrays.asList("demon_head", "raven_feather", "smelter_coal"); break;
            case 3: demonRareMaterials = Arrays.asList("demon_head", "raven_feather", "divine_droplet"); break;
            case 4: demonRareMaterials = Arrays.asList("demon_head", "raven_feather", "crucible_flux"); break;
            default: demonRareMaterials = Arrays.asList("demon_head", "raven_feather"); break;
        }

        // Demon Tower - Legendary (tier-specific boss materials z 30% szansą)
        List<String> demonLegendaryMaterials;
        boolean isBossDrop = random.nextDouble() < 0.30; // 30% szans na boss material
        switch (tier) {
            case 1:
                demonLegendaryMaterials = isBossDrop ?
                    Arrays.asList("valgroth_sigil") : Arrays.asList("chaos_artifact");
                break;
            case 2:
                demonLegendaryMaterials = isBossDrop ?
                    Arrays.asList("azhramorth_scepter") : Arrays.asList("chaos_artifact");
                break;
            case 3:
                demonLegendaryMaterials = isBossDrop ?
                    Arrays.asList("korthamoth_eye") : Arrays.asList("chaos_artifact");
                break;
            case 4:
                demonLegendaryMaterials = isBossDrop ?
                    Arrays.asList("vharzhul_blade", "acheron_core") : Arrays.asList("chaos_artifact");
                break;
            default:
                demonLegendaryMaterials = Arrays.asList("chaos_artifact");
                break;
        }

        // 50% szans na materiały z Demon Tower vs oryginalne
        boolean useDemonBasic = random.nextBoolean();
        boolean useDemonRare = random.nextBoolean();
        boolean useDemonLegendary = random.nextBoolean();

        List<String> chosenBasic = useDemonBasic ? demonBasicMaterials : basicMaterials;
        List<String> chosenRare = useDemonRare ? demonRareMaterials : rareMaterials;
        List<String> chosenLegendary = useDemonLegendary ? demonLegendaryMaterials : legendaryMaterials;

        switch (rarity.toLowerCase()) {
            case "magic":
                String basicMat = chosenBasic.get(random.nextInt(chosenBasic.size()));
                materials.put(basicMat, 2 + random.nextInt(2));
                break;
            case "extraordinary":
                basicMat = chosenBasic.get(random.nextInt(chosenBasic.size()));
                String rareMat = chosenRare.get(random.nextInt(chosenRare.size()));
                materials.put(basicMat, 2 + random.nextInt(4));
                materials.put(rareMat, 1);
                break;
            case "legendary":
                basicMat = chosenBasic.get(random.nextInt(chosenBasic.size()));
                rareMat = chosenRare.get(random.nextInt(chosenRare.size()));
                materials.put(basicMat, 2 + random.nextInt(9));
                materials.put(rareMat, 2 + random.nextInt(3));
                break;
            case "unique":
                basicMat = chosenBasic.get(random.nextInt(chosenBasic.size()));
                rareMat = chosenRare.get(random.nextInt(chosenRare.size()));
                String legendaryMat = chosenLegendary.get(random.nextInt(chosenLegendary.size()));
                materials.put(basicMat, 20 + random.nextInt(81));
                materials.put(rareMat, 10 + random.nextInt(6));
                materials.put(legendaryMat, 1);
                break;
            case "mythic":
                basicMat = chosenBasic.get(random.nextInt(chosenBasic.size()));
                rareMat = chosenRare.get(random.nextInt(chosenRare.size()));
                legendaryMat = chosenLegendary.get(random.nextInt(chosenLegendary.size()));
                materials.put(basicMat, 1000);
                materials.put(rareMat, 150 + random.nextInt(101));
                materials.put(legendaryMat, 1 + random.nextInt(3));
                break;
        }

        int multiplier = enhancement + 1;
        for (Map.Entry<String, Integer> entry : materials.entrySet()) {
            entry.setValue(entry.getValue() * multiplier);
        }

        return materials;
    }

    private String getRomanNumeral(int tier) {
        switch (tier) {
            case 1: return "I";
            case 2: return "II";
            case 3: return "III";
            case 4: return "IV";
            default: return "I";
        }
    }

    private void addMaterialsToPouch(Player player, List<String> materials) {
        try {
            Class<?> pouchAPIClass = Class.forName("com.maks.ingredientpouchplugin.api.PouchAPI");
            org.bukkit.plugin.Plugin pouchPlugin = Bukkit.getPluginManager().getPlugin("IngredientPouchPlugin");
            
            if (pouchPlugin == null) {
                player.sendMessage(ChatColor.RED + "IngredientPouchPlugin not found! Materials will be dropped.");
                return;
            }
            
            Class<?> pluginClass = Class.forName("com.maks.ingredientpouchplugin.IngredientPouchPlugin");
            Method getAPIMethod = pluginClass.getMethod("getAPI");
            Object apiInstance = getAPIMethod.invoke(pouchPlugin);
            
            Method updateMethod = pouchAPIClass.getMethod("updateItemQuantity", String.class, String.class, int.class);
            
            for (String materialStr : materials) {
                String[] parts = materialStr.split("x ", 2);
                if (parts.length == 2) {
                    int amount = Integer.parseInt(parts[0]);
                    String materialId = parts[1];
                    
                    updateMethod.invoke(apiInstance, player.getUniqueId().toString(), materialId, amount);
                    
                    if (DEBUG) {
                        getLogger().info("Added " + amount + " " + materialId + " to " + player.getName() + "'s pouch");
                    }
                }
            }
        } catch (Exception e) {
            player.sendMessage(ChatColor.RED + "Error adding materials to pouch: " + e.getMessage());
            if (DEBUG) {
                e.printStackTrace();
            }
        }
    }
}
