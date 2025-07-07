# MyShopPlugin

A Minecraft plugin that allows players to sell items with rarity values through a simple GUI interface.

## Description

MyShopPlugin is a shop system for Minecraft servers that enables players to sell items based on their rarity level. The plugin reads item lore to determine rarity and calculates value based on configurable rarity values and level multipliers.

### Features

- GUI-based shop interface for selling items
- Rarity-based item valuation system
- Support for item level multipliers (items with "+N" in their rarity)
- Integration with Vault for economy
- Optional integration with TrinketsPlugin for sell bonuses
- Command to check item lore

## Requirements

- Minecraft 1.20+
- Paper/Spigot server
- Vault and a compatible economy plugin
- Java 8 or higher

## Installation

1. Download the latest release of MyShopPlugin
2. Place the JAR file in your server's `plugins` folder
3. Restart your server or use a plugin manager to load the plugin
4. Configure the plugin settings in `config.yml` as needed

## Usage

### Commands

- `/shop` - Opens the shop GUI for selling items
- `/checklore` - Displays the lore of the item currently in your hand

### Permissions

- `shopplugin.use` - Allows access to the `/shop` command
- `shopplugin.checklore` - Allows access to the `/checklore` command

### How to Sell Items

1. Type `/shop` to open the shop interface
2. Place items you want to sell in the top section of the GUI
3. Click the emerald "Confirm Sale" button to sell all items
4. Items will be evaluated based on their rarity and level
5. Money will be deposited to your account

## Configuration

The plugin's configuration is stored in `config.yml`:

```yaml
rarity_values:
  Normal: 100
  Improved: 500
  Magic: 1000
  Extraordinary: 2500
  Legendary: 5000
  Unique: 10000
  MYTHIC: 20000

level_multiplier: 500
```

### Configuration Options

- `rarity_values` - Defines the base value for each rarity type
- `level_multiplier` - Multiplier applied for each level of an item (for items with "+N" in their rarity)

## Item Requirements

For an item to be sellable, it must have lore that includes a line starting with "Rarity:" followed by one of the configured rarity values. For example:
- "Rarity: Normal"
- "Rarity: Magic +2"

The plugin will extract the rarity name and level to calculate the item's value.

## Integration with Other Plugins

MyShopPlugin can integrate with TrinketsPlugin to apply sell bonuses from the Merchant Jewel. This integration is automatic if TrinketsPlugin is installed.

## Building from Source

1. Clone the repository
2. Build using Maven:
   ```
   mvn clean package
   ```
3. The compiled JAR will be in the `target` folder

## License

This project is available as open source under the terms of the [MIT License](https://opensource.org/licenses/MIT).