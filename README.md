# 🎮 X-Bedrock

[![Minecraft Version](https://img.shields.io/badge/Minecraft-1.21.3-blue.svg)](https://www.minecraft.net)
[![License](https://img.shields.io/badge/License-MIT-green.svg)](LICENSE)

> 🌟 The Ultimate Cross-Platform Minecraft Experience

## 📋 Table of Contents
- [Features](#-features)
- [Installation](#-installation)
- [Configuration](#-configuration)
- [Commands](#-commands)
- [Permissions](#-permissions)
- [Roblox Integration](#-roblox-integration)
- [Browser Integration](#-browser-integration)
- [Webstore](#-webstore)
- [API Documentation](#-api-documentation)
- [Support](#-support)

## ✨ Features

> ✨🔧 Techy Sparkle: Now powered by X-Bedrock native Bedrock support and NettyIX anti-packet crasher! ⚡🛡️🌈

### 🎯 Core Features
- 🔄 Seamless Bedrock-Java crossplay (Native X-Bedrock implementation)
- 🎨 Native cosmetics support
- 💰 Integrated webstore system
- 🌐 Cross-platform player data
- 🔒 Secure authentication

### 🎮 Platform Support
- 📱 Minecraft: Bedrock Edition (via X-Bedrock, no GeyserMC required)
- 💻 Minecraft: Java Edition
- 🎲 Roblox
- 🌐 Browser-based games

### 🛠️ Technical Features
- 📊 Real-time data synchronization
- 🔄 Automatic resource pack management
- 🎨 RGB color support
- 📝 Customizable player prefixes
- 🔒 Secure API integration
- ⚡ Native Bedrock protocol handler (X-Bedrock)
- 🛡️ NettyIX anti-packet crasher & advanced security
- 🚦 Custom connection gate and rate limiting for anti-flood protection

## 📥 Installation

### Server Requirements
- Java 17 or higher
- Spigot/Paper 1.21.3
- 2GB+ RAM recommended
- No GeyserMC required! (Native Bedrock support via X-Bedrock)

### Installation Steps
1. 📥 Download the latest release
2. 📁 Place the jar in your `plugins` folder
3. 🔄 Restart your server
4. ⚙️ Configure `config.yml`
5. 🎮 Start playing!

## ⚙️ Configuration

### Basic Configuration
```yaml
features:
  cosmetics:
    enabled: true
  webstore:
    enabled: true
  roblox:
    enabled: true
  browser:
    enabled: false
```

### Advanced Settings
```yaml
api:
  roblox:
    endpoint: "https://api.roblox.com"
    key: "your-key"
  webstore:
    endpoint: "https://your-webstore.com/api"
    key: "your-key"
```

## 📝 Commands

### Admin Commands
- `/xbedrock toggle <feature>` - Toggle features
- `/xbedrock prefix <player> <prefix>` - Set player prefix
- `/xbedrock webstore <reload|status>` - Manage webstore
- `/xbedrock roblox <reload|status|sync>` - Manage Roblox integration

### Player Commands
- `/xbedrock cosmetics` - Open cosmetics menu
- `/xbedrock webstore` - Open webstore
- `/xbedrock link` - Link your accounts

## 🔑 Permissions

### Admin Permissions
- `xbedrock.admin` - Full access
- `xbedrock.reload` - Reload configuration
- `xbedrock.debug` - Debug mode

### Player Permissions
- `xbedrock.cosmetics` - Use cosmetics
- `xbedrock.webstore` - Use webstore
- `xbedrock.roblox` - Use Roblox features

## 🎲 Roblox Integration

### Roblox Addon
1. 📥 Download the Roblox addon
2. 📁 Place in your Roblox game
3. ⚙️ Configure the addon
4. 🔄 Start crossplay!

### Features
- 🎮 Real-time player synchronization
- 🎨 Skin conversion
- 💬 Cross-platform chat
- 🎯 Shared inventory
- 🌐 Seamless teleportation

### Addon Configuration
```lua
local XBedrock = require(game:GetService("ReplicatedStorage").XBedrock)

XBedrock:Initialize({
    ServerIP = "your-server-ip",
    ServerPort = 19132,
    SyncInterval = 1,
    EnableChat = true,
    EnableInventory = true
})
```

## 🌐 Browser Integration

### Features
- 🎮 Browser-based gameplay
- 💬 Cross-platform chat
- 🎨 Skin support
- 🔄 Real-time sync

### Implementation
```javascript
const xBedrock = new XBedrock({
    serverUrl: 'your-server-url',
    syncInterval: 1000,
    features: {
        chat: true,
        inventory: true,
        cosmetics: true
    }
});
```

## 💰 Webstore

### Features
- 💳 Native payment processing
- 🎁 Instant delivery
- 🔄 Cross-platform purchases
- 📊 Purchase history
- 💰 Virtual currency

### Integration
```yaml
webstore:
  currency: "coins"
  items:
    vip:
      price: 1000
      features:
        - "prefix"
        - "cosmetics"
    premium:
      price: 5000
      features:
        - "all_vip"
        - "special_effects"
```

## 📚 API Documentation

### Java API
```java
XBedrockPlugin plugin = (XBedrockPlugin) Bukkit.getPluginManager().getPlugin("X-Bedrock");

// Get managers
CosmeticsManager cosmetics = plugin.getCosmeticsManager();
WebstoreManager webstore = plugin.getWebstoreManager();
RobloxManager roblox = plugin.getRobloxManager();
```

### Roblox API
```lua
local XBedrock = require(game:GetService("ReplicatedStorage").XBedrock)

-- Connect to server
XBedrock:Connect()

-- Send position
XBedrock:SendPosition(Vector3.new(0, 0, 0))

-- Receive chat
XBedrock.OnChat:Connect(function(message)
    print(message)
end)
```

### Browser API
```javascript
const xBedrock = new XBedrock();

// Connect to server
xBedrock.connect();

// Send position
xBedrock.sendPosition({ x: 0, y: 0, z: 0 });

// Receive chat
xBedrock.on('chat', (message) => {
    console.log(message);
});
```

## 🆘 Support
i dont want any discord server but u can dm me anytime please dont be super timeconsuming..
discord: command19x.

### Contributing
1. 🔱 Fork the repository
2. 📝 Create your feature branch
3. 💾 Commit your changes
4. 🔄 Push to the branch
5. 📬 Create a Pull Request

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## 🎲 Acknowledgments

- 🎲 Roblox for cross-platform capabilities
- 🛡️ NettyIX for anti-packet crasher and security
- 🌐 All contributors and supporters

---

Made with ❤️ by [command17x] 
