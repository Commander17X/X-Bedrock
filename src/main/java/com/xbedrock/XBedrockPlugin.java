package com.xbedrock;

import org.bukkit.plugin.java.JavaPlugin;
import org.geysermc.geyser.api.GeyserApi;
import org.geysermc.geyser.api.connection.GeyserConnection;
import org.geysermc.geyser.api.event.EventRegistrar;
import org.geysermc.geyser.api.event.lifecycle.GeyserDefineCommandsEvent;
import org.geysermc.geyser.api.event.lifecycle.GeyserPostInitializeEvent;
import org.geysermc.geyser.api.event.lifecycle.GeyserPreInitializeEvent;
import org.geysermc.geyser.api.event.lifecycle.GeyserShutdownEvent;
import org.geysermc.geyser.api.event.connection.ConnectionEvent;
import org.geysermc.geyser.api.event.connection.ConnectionRequestEvent;
import org.geysermc.geyser.api.event.connection.ConnectionSuccessEvent;
import org.geysermc.geyser.api.event.connection.ConnectionCloseEvent;
import com.xbedrock.resource.ResourcePackManager;
import com.xbedrock.pvp.PvPManager;
import com.xbedrock.pvp.PvPManager.PvPMode;
import com.xbedrock.connection.ConnectionManager;
import com.xbedrock.message.MessageManager;
import com.xbedrock.compatibility.CompatibilityManager;
import com.xbedrock.resource.ResourcePackHandler;
import com.xbedrock.cosmetics.CosmeticsManager;
import com.xbedrock.cosmetics.CosmeticPacketHandler;
import com.xbedrock.commands.AdminCommandManager;
import com.xbedrock.roblox.RobloxManager;
import com.xbedrock.webstore.WebstoreManager;
import com.xbedrock.config.ConfigManager;
import com.xbedrock.browser.BrowserManager;
import com.xbedrock.player.PlayerDataManager;

public class XBedrockPlugin extends JavaPlugin {
    private static XBedrockPlugin instance;
    private GeyserApi geyserApi;
    private ResourcePackManager resourcePackManager;
    private PvPManager pvpManager;
    private ConnectionManager connectionManager;
    private MessageManager messageManager;
    private CompatibilityManager compatibilityManager;
    private ResourcePackHandler resourcePackHandler;
    private CosmeticsManager cosmeticsManager;
    private CosmeticPacketHandler cosmeticPacketHandler;
    private AdminCommandManager adminCommandManager;
    private RobloxManager robloxManager;
    private WebstoreManager webstoreManager;
    private BrowserManager browserManager;
    private ConfigManager configManager;
    private PlayerDataManager playerDataManager;

    @Override
    public void onEnable() {
        instance = this;

        // Save default config
        saveDefaultConfig();

        // Initialize Geyser API
        geyserApi = GeyserApi.api();

        // Initialize config first
        this.configManager = new ConfigManager(this);

        // Initialize player data manager
        this.playerDataManager = new PlayerDataManager(this);

        // Initialize managers
        resourcePackManager = new ResourcePackManager(this);
        pvpManager = new PvPManager(this);
        connectionManager = new ConnectionManager(this);
        messageManager = new MessageManager(this);
        compatibilityManager = new CompatibilityManager(this);
        resourcePackHandler = new ResourcePackHandler(this);
        this.cosmeticsManager = new CosmeticsManager(this);
        this.cosmeticPacketHandler = new CosmeticPacketHandler(this);
        this.adminCommandManager = new AdminCommandManager(this);
        this.robloxManager = new RobloxManager(this);
        this.webstoreManager = new WebstoreManager(this);
        this.browserManager = new BrowserManager(this);
        getServer().getPluginManager().registerEvents(cosmeticsManager, this);

        // Load resource packs
        resourcePackHandler.loadResourcePacks();
        resourcePackHandler.registerResourcePacks();

        // Register events
        registerEvents();

        // Register commands
        getCommand("xbedrock").setExecutor(adminCommandManager);
        getCommand("xbedrock").setTabCompleter(adminCommandManager);

        // Set enabled states from config
        cosmeticsManager.setEnabled(configManager.isFeatureEnabled("cosmetics"));
        robloxManager.setEnabled(configManager.isFeatureEnabled("roblox"));
        webstoreManager.setEnabled(configManager.isFeatureEnabled("webstore"));
        browserManager.setEnabled(configManager.isFeatureEnabled("browser"));

        getLogger().info("X-Bedrock has been enabled!");
    }

    @Override
    public void onDisable() {
        if (geyserApi != null) {
            geyserApi.shutdown();
        }

        // Clean up cosmetics
        if (cosmeticPacketHandler != null) {
            cosmeticPacketHandler.clearCache();
        }

        // Clean up managers
        if (robloxManager != null) {
            robloxManager.setEnabled(false);
        }

        if (webstoreManager != null) {
            webstoreManager.setEnabled(false);
        }

        if (browserManager != null) {
            browserManager.setEnabled(false);
        }

        getLogger().info("X-Bedrock has been disabled!");
    }

    private void registerEvents() {
        EventRegistrar events = geyserApi.eventBus();

        // Lifecycle events
        events.subscribe(GeyserPreInitializeEvent.class, this::onPreInitialize);
        events.subscribe(GeyserPostInitializeEvent.class, this::onPostInitialize);
        events.subscribe(GeyserShutdownEvent.class, this::onShutdown);

        // Connection events
        events.subscribe(ConnectionRequestEvent.class, connectionManager::handleConnectionRequest);
        events.subscribe(ConnectionSuccessEvent.class, connectionManager::handleConnectionSuccess);
        events.subscribe(ConnectionCloseEvent.class, connectionManager::handleConnectionClose);

        // Register managers as listeners
        getServer().getPluginManager().registerEvents(pvpManager, this);
        getServer().getPluginManager().registerEvents(compatibilityManager, this);
    }

    private void onPreInitialize(GeyserPreInitializeEvent event) {
        getLogger().info("Initializing X-Bedrock...");
    }

    private void onPostInitialize(GeyserPostInitializeEvent event) {
        getLogger().info("X-Bedrock initialization complete!");
    }

    private void onShutdown(GeyserShutdownEvent event) {
        getLogger().info("X-Bedrock is shutting down...");
    }

    public static XBedrockPlugin getInstance() {
        return instance;
    }

    public GeyserApi getGeyserApi() {
        return geyserApi;
    }

    public ResourcePackManager getResourcePackManager() {
        return resourcePackManager;
    }

    public PvPManager getPvPManager() {
        return pvpManager;
    }

    public ConnectionManager getConnectionManager() {
        return connectionManager;
    }

    public MessageManager getMessageManager() {
        return messageManager;
    }

    public CompatibilityManager getCompatibilityManager() {
        return compatibilityManager;
    }

    public ResourcePackHandler getResourcePackHandler() {
        return resourcePackHandler;
    }

    public CosmeticsManager getCosmeticsManager() {
        return cosmeticsManager;
    }

    public CosmeticPacketHandler getCosmeticPacketHandler() {
        return cosmeticPacketHandler;
    }

    public RobloxManager getRobloxManager() {
        return robloxManager;
    }

    public WebstoreManager getWebstoreManager() {
        return webstoreManager;
    }

    public BrowserManager getBrowserManager() {
        return browserManager;
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public PlayerDataManager getPlayerDataManager() {
        return playerDataManager;
    }
}