package com.xbedrock;

import com.xbedrock.connection.BedrockConnectionManager;
import com.xbedrock.connection.ConnectionGate;
import com.xbedrock.cosmetics.CosmeticsManager;
import com.xbedrock.player.PlayerDataManager;
import com.xbedrock.pvp.PvPManager;
import com.xbedrock.resource.ResourcePackManager;
import com.xbedrock.roblox.RobloxManager;
import com.xbedrock.security.SecurityManager;
import com.xbedrock.webstore.WebstoreManager;
import org.bukkit.plugin.java.JavaPlugin;

public class XBedrockPlugin extends JavaPlugin {
    private BedrockConnectionManager bedrockManager;
    private ConnectionGate connectionGate;
    private CosmeticsManager cosmeticsManager;
    private PlayerDataManager playerDataManager;
    private PvPManager pvpManager;
    private ResourcePackManager resourcePackManager;
    private RobloxManager robloxManager;
    private SecurityManager securityManager;
    private WebstoreManager webstoreManager;

    @Override
    public void onEnable() {
        // Save default config
        saveDefaultConfig();

        // Initialize managers
        this.securityManager = new SecurityManager(this);
        this.connectionGate = new ConnectionGate(this);
        this.bedrockManager = new BedrockConnectionManager(this);
        this.playerDataManager = new PlayerDataManager(this);
        this.cosmeticsManager = new CosmeticsManager(this);
        this.pvpManager = new PvPManager(this);
        this.resourcePackManager = new ResourcePackManager(this);
        this.robloxManager = new RobloxManager(this);
        this.webstoreManager = new WebstoreManager(this);

        // Register commands
        getCommand("xbedrock").setExecutor(new XBedrockCommand(this));

        getLogger().info("X-Bedrock has been enabled!");
    }

    @Override
    public void onDisable() {
        // Shutdown managers
        if (bedrockManager != null)
            bedrockManager.shutdown();
        if (connectionGate != null)
            connectionGate.shutdown();
        if (robloxManager != null)
            robloxManager.shutdown();
        if (webstoreManager != null)
            webstoreManager.shutdown();

        getLogger().info("X-Bedrock has been disabled!");
    }

    public BedrockConnectionManager getBedrockManager() {
        return bedrockManager;
    }

    public ConnectionGate getConnectionGate() {
        return connectionGate;
    }

    public CosmeticsManager getCosmeticsManager() {
        return cosmeticsManager;
    }

    public PlayerDataManager getPlayerDataManager() {
        return playerDataManager;
    }

    public PvPManager getPvPManager() {
        return pvpManager;
    }

    public ResourcePackManager getResourcePackManager() {
        return resourcePackManager;
    }

    public RobloxManager getRobloxManager() {
        return robloxManager;
    }

    public SecurityManager getSecurityManager() {
        return securityManager;
    }

    public WebstoreManager getWebstoreManager() {
        return webstoreManager;
    }
}