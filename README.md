# CubeClans

A complete clan system plugin for Minecraft 1.20.4 with PlaceholderAPI integration.

## Features

- **Clan Management**: Create, disband, and manage clans
- **Member System**: Invite players, accept invitations, kick members, and leave clans
- **Alliance System**: Form alliances with other clans for cooperation
- **Clan Bank**: Economy integration with Vault to deposit/withdraw clan money
- **GUI Menus**: Interactive inventory-based menus for all clan operations
- **PlaceholderAPI Integration**: Full support for placeholders in other plugins
- **Configurable**: Everything is customizable through config.yml
- **Persistent Storage**: All clan data is saved in an SQLite database for reliability and performance
- **Permission System**: Complete permission nodes for fine-grained control
- **Admin Commands**: Administrative tools for server management

## Commands

- `/clan` - Open the main clan menu
- `/clan create <name>` - Create a new clan
- `/clan disband` - Disband your clan (leader only)
- `/clan invite <player>` - Invite a player to your clan
- `/clan accept <clan>` - Accept a clan invitation
- `/clan leave` - Leave your current clan
- `/clan kick <player>` - Kick a member from your clan (leader only)
- `/clan info [clan]` - View clan information
- `/clan members` - View clan members
- `/clan ally <clan>` - Send an alliance request to another clan
- `/clan ally accept <clan>` - Accept an alliance request
- `/clan ally deny <clan>` - Deny an alliance request
- `/clan ally remove <clan>` - Remove an existing alliance
- `/clan ally list` - List all your clan's allies
- `/clan enemy <clan>` - Declare or remove an enemy clan (toggle)
- `/clan enemy remove <clan>` - Remove an enemy relationship
- `/clan enemy list` - List all your clan's enemies
- `/clan bank deposit <amount>` - Deposit money into clan bank (leader only)
- `/clan bank withdraw <amount>` - Withdraw money from clan bank (leader only)
- `/clan bank balance` - View clan bank balance
- `/clan help` - Show help menu
- `/clan admin disband <clan>` - Admin command to disband any clan
- `/clan admin reload` - Admin command to reload the plugin configuration

## Permissions

- `cubeclans.use` - Allows using the clan system (default: true)
- `cubeclans.admin` - Allows administrative control over clans (default: op)
- `cubeclans.create` - Allows creating a clan (default: true)
- `cubeclans.delete` - Allows deleting your own clan (default: true)
- `cubeclans.invite` - Allows inviting players to your clan (default: true)
- `cubeclans.kick` - Allows kicking members from your clan (default: true)

## PlaceholderAPI Placeholders

- `%cubeclans_name%` - Player's clan name (without color)
- `%cubeclans_name_color%` - Player's clan name (with color)
- `%cubeclans_leader%` - Clan leader name
- `%cubeclans_members%` - Number of clan members
- `%cubeclans_max_members%` - Maximum members allowed
- `%cubeclans_is_leader%` - True/false if player is leader
- `%cubeclans_has_clan%` - True/false if player has a clan
- `%cubeclans_created%` - Date the clan was created
- `%cubeclans_description%` - Clan description
- `%cubeclans_total_clans%` - Total number of clans on the server
- `%cubeclans_joined%` - Date the player joined the clan
- `%cubeclans_role%` - Player's role in the clan (Leader/Member)
- `%cubeclans_allies%` - Number of allied clans
- `%cubeclans_max_allies%` - Maximum allies allowed
- `%cubeclans_ally_names%` - Comma-separated list of ally clan names
- `%cubeclans_enemies%` - Number of enemy clans
- `%cubeclans_max_enemies%` - Maximum enemies allowed
- `%cubeclans_enemy_names%` - Comma-separated list of enemy clan names
- `%cubeclans_bank%` - Clan bank balance

## Configuration

All settings can be customized in `config.yml`:

- Minimum/maximum clan name length
- Maximum members per clan
- Maximum allies per clan
- Maximum enemies per clan (settings.max-clan-enemies)
- Color code support in names
- Custom messages
- Menu layouts and items
- Auto-save intervals

## Building

1. Make sure you have Maven installed
2. Clone this repository
3. Run `mvn clean package`
4. The compiled JAR will be in the `target` folder

## Installation

1. Download the latest release
2. Place the JAR file in your server's `plugins` folder
3. Install PlaceholderAPI from SpigotMC
4. Restart your server
5. Configure the plugin in `plugins/CubeClans/config.yml`

## Requirements

- Minecraft 1.20.4
- Java 17 or higher
- PlaceholderAPI plugin (optional, for placeholders)
- Vault + Economy plugin (optional, for clan bank feature)

## Support

For issues, feature requests, or questions, please open an issue on GitHub.

## License

This project is provided as-is for educational and server use purposes.
