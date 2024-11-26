# MyShopPlugin

MyShopPlugin is a Minecraft plugin that allows players to sell items with specific rarities and levels directly through a GUI shop interface. The plugin integrates with Vault and any compatible economy plugin to provide a seamless selling experience.

## Features

- **GUI Shop Interface**: Players can open a shop GUI to sell items.
- **Rarity-Based Pricing**: Items are valued based on their rarity defined in the configuration.
- **Level Multipliers**: Items with levels (e.g., `+1`, `+2`) increase in value exponentially per level.
- **Vault Integration**: Supports Vault for economy transactions.
- **Customizable Configuration**: Easily adjust rarities, base values, and multipliers via `config.yml`.
- **In-Game Commands**: Simple commands for players to interact with the shop and check item lore.

## Requirements

- **Minecraft Server**: Spigot, Paper, or any server supporting Bukkit plugins.
- **Vault**: [Vault plugin](https://www.spigotmc.org/resources/vault.34315/) must be installed.
- **Economy Plugin**: Any Vault-compatible economy plugin (e.g., EssentialsX Economy).

## Installation

1. **Download** the `MyShopPlugin.jar` file.

2. **Place** the `.jar` file into your server's `plugins` directory.

3. **Restart** your server to generate the default configuration files.

4. **Configure** the plugin settings in `config.yml` as needed.

5. **Restart** the server or use `/reload` to apply configuration changes.

## Configuration

The plugin's configuration file `config.yml` allows you to define item rarities, base values, and the level multiplier.

```yaml
rarity_values:
  Normal: 100
  Improved: 500
  Magic: 1000
  Extraordinary: 2500
  Legendary: 5000
  Unique: 10000
  MYTHIC: 20000

level_multiplier: 1.5
rarity_values: Defines the base value for each rarity type.
level_multiplier: Determines how much the item's value increases per level.
How the Value is Calculated
The total value of an item is calculated using the following formula:

totalValue = baseValue * (level_multiplier)^(rarityLevel - 1) * itemAmount
baseValue: The value associated with the item's rarity.
level_multiplier: The multiplier from the configuration.
rarityLevel: The level of the item (e.g., +2 would be level 2).
itemAmount: The quantity of the item being sold.
Usage
Selling Items
Open the Shop GUI: Use the /shop command to open the selling interface.

Place Items: Put the items you wish to sell into the slots (0-17) in the GUI.

Confirm Sale: Click on the Confirm Sale button (emerald) to sell the items.

Receive Payment: If the items are sellable, you'll receive money based on their value.

Commands
/shop

Description: Opens the shop GUI for selling items.
Usage: /shop
Permission: myshopplugin.use
/checklore

Description: Displays the lore of the item in your main hand.
Usage: /checklore
Permission: myshopplugin.checklore
Permissions
myshopplugin.use

Description: Allows the player to use the /shop command.
Default: All players (true)
myshopplugin.checklore

Description: Allows the player to use the /checklore command.
Default: All players (true)
Item Requirements
For an item to be sellable:

Lore: The item must have a lore line starting with Rarity:.

Rarity Name: The rarity name must match one defined in config.yml.

Optional Level: Items can have a level indicated by +N (e.g., +2).

Example Item Lore
Rarity: Magic +2
Rarity: Magic
Level: 2
Preventing Issues
Unsellable Items: If you place an unsellable item into the shop GUI, it will be returned to your inventory when you attempt to confirm the sale.

Full Inventory: If your inventory is full when unsellable items are returned, they will be dropped at your location.

GUI Restrictions: You cannot move or remove placeholder items or the confirm button from the GUI.

Contribution
Contributions are welcome! If you'd like to contribute to the plugin, please:

Fork the repository on GitHub.

Create a new branch for your feature or bug fix.

Commit your changes with clear messages.

Submit a pull request detailing your changes.

Issues
If you encounter any issues or have suggestions, please open an issue on the GitHub repository. Provide as much detail as possible, including:

Steps to reproduce the issue.

Any error messages or logs.

Version of Minecraft and other plugins you're using.
