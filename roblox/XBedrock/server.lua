local XBedrockServer = {}
XBedrockServer.__index = XBedrockServer

-- Services
local ReplicatedStorage = game:GetService("ReplicatedStorage")
local Players = game:GetService("Players")
local HttpService = game:GetService("HttpService")
local RunService = game:GetService("RunService")

-- Constants
local DEFAULT_CONFIG = {
    ServerIP = "localhost",
    ServerPort = 19132,
    SyncInterval = 1,
    EnableChat = true,
    EnableInventory = true,
    EnableCosmetics = true,
    EnableWebstore = true,
    APIKey = "",
    APIEndpoint = "http://localhost:8080/api"
}

-- Private variables
local config = {}
local connected = false
local playerData = {}
local remoteEvents = {}
local remoteFunctions = {}

-- Create RemoteEvents and RemoteFunctions
local function createRemotes()
    local remotes = Instance.new("Folder")
    remotes.Name = "XBedrockRemotes"
    remotes.Parent = ReplicatedStorage

    -- Events
    remoteEvents = {
        Chat = Instance.new("RemoteEvent"),
        Position = Instance.new("RemoteEvent"),
        Inventory = Instance.new("RemoteEvent"),
        Cosmetics = Instance.new("RemoteEvent"),
        Webstore = Instance.new("RemoteEvent")
    }

    -- Functions
    remoteFunctions = {
        GetPlayerData = Instance.new("RemoteFunction"),
        SyncInventory = Instance.new("RemoteFunction"),
        GetCosmetics = Instance.new("RemoteFunction")
    }

    -- Parent events
    for name, event in pairs(remoteEvents) do
        event.Name = name
        event.Parent = remotes
    end

    -- Parent functions
    for name, func in pairs(remoteFunctions) do
        func.Name = name
        func.Parent = remotes
    end
end

-- Initialize the server
function XBedrockServer:Initialize(customConfig)
    config = table.clone(DEFAULT_CONFIG)
    for key, value in pairs(customConfig or {}) do
        config[key] = value
    end

    createRemotes()
    self:SetupEventHandlers()
    self:StartSyncLoop()
end

-- Setup event handlers
function XBedrockServer:SetupEventHandlers()
    -- Player joining
    Players.PlayerAdded:Connect(function(player)
        self:HandlePlayerJoin(player)
    end)

    -- Player leaving
    Players.PlayerRemoving:Connect(function(player)
        self:HandlePlayerLeave(player)
    end)

    -- Chat handler
    if config.EnableChat then
        remoteEvents.Chat.OnServerEvent:Connect(function(player, message)
            self:HandleChat(player, message)
        end)
    end

    -- Position handler
    remoteEvents.Position.OnServerEvent:Connect(function(player, position)
        self:HandlePosition(player, position)
    end)

    -- Inventory handler
    if config.EnableInventory then
        remoteEvents.Inventory.OnServerEvent:Connect(function(player, inventory)
            self:HandleInventory(player, inventory)
        end)
    end

    -- Cosmetics handler
    if config.EnableCosmetics then
        remoteEvents.Cosmetics.OnServerEvent:Connect(function(player, cosmetics)
            self:HandleCosmetics(player, cosmetics)
        end)
    end

    -- Webstore handler
    if config.EnableWebstore then
        remoteEvents.Webstore.OnServerEvent:Connect(function(player, webstore)
            self:HandleWebstore(player, webstore)
        end)
    end

    -- Remote function handlers
    remoteFunctions.GetPlayerData.OnServerInvoke = function(player)
        return self:GetPlayerData(player)
    end

    remoteFunctions.SyncInventory.OnServerInvoke = function(player)
        return self:GetInventory(player)
    end

    remoteFunctions.GetCosmetics.OnServerInvoke = function(player)
        return self:GetCosmetics(player)
    end
end

-- Start sync loop
function XBedrockServer:StartSyncLoop()
    RunService.Heartbeat:Connect(function()
        if not connected then return end

        -- Sync all players
        for _, player in ipairs(Players:GetPlayers()) do
            self:SyncPlayer(player)
        end
    end)
end

-- Handle player join
function XBedrockServer:HandlePlayerJoin(player)
    -- Initialize player data
    playerData[player.UserId] = {
        position = Vector3.new(0, 0, 0),
        inventory = {},
        cosmetics = {},
        webstore = {}
    }

    -- Load player data from API
    self:LoadPlayerData(player)
end

-- Handle player leave
function XBedrockServer:HandlePlayerLeave(player)
    -- Save player data to API
    self:SavePlayerData(player)

    -- Clean up
    playerData[player.UserId] = nil
end

-- Handle chat
function XBedrockServer:HandleChat(player, message)
    -- Broadcast to all players
    for _, otherPlayer in ipairs(Players:GetPlayers()) do
        remoteEvents.Chat:FireClient(otherPlayer, {
            player = player.Name,
            message = message
        })
    end

    -- Send to API
    self:SendToAPI("chat", {
        player = player.UserId,
        message = message
    })
end

-- Handle position
function XBedrockServer:HandlePosition(player, position)
    -- Update player data
    if playerData[player.UserId] then
        playerData[player.UserId].position = position
    end

    -- Send to API
    self:SendToAPI("position", {
        player = player.UserId,
        position = position
    })
end

-- Handle inventory
function XBedrockServer:HandleInventory(player, inventory)
    -- Update player data
    if playerData[player.UserId] then
        playerData[player.UserId].inventory = inventory
    end

    -- Send to API
    self:SendToAPI("inventory", {
        player = player.UserId,
        inventory = inventory
    })
end

-- Handle cosmetics
function XBedrockServer:HandleCosmetics(player, cosmetics)
    -- Update player data
    if playerData[player.UserId] then
        playerData[player.UserId].cosmetics = cosmetics
    end

    -- Send to API
    self:SendToAPI("cosmetics", {
        player = player.UserId,
        cosmetics = cosmetics
    })
end

-- Handle webstore
function XBedrockServer:HandleWebstore(player, webstore)
    -- Update player data
    if playerData[player.UserId] then
        playerData[player.UserId].webstore = webstore
    end

    -- Send to API
    self:SendToAPI("webstore", {
        player = player.UserId,
        webstore = webstore
    })
end

-- Get player data
function XBedrockServer:GetPlayerData(player)
    return playerData[player.UserId] or {}
end

-- Get inventory
function XBedrockServer:GetInventory(player)
    return playerData[player.UserId] and playerData[player.UserId].inventory or {}
end

-- Get cosmetics
function XBedrockServer:GetCosmetics(player)
    return playerData[player.UserId] and playerData[player.UserId].cosmetics or {}
end

-- Load player data
function XBedrockServer:LoadPlayerData(player)
    local success, data = pcall(function()
        return HttpService:GetAsync(config.APIEndpoint .. "/player/" .. player.UserId)
    end)

    if success and data then
        playerData[player.UserId] = HttpService:JSONDecode(data)
    end
end

-- Save player data
function XBedrockServer:SavePlayerData(player)
    if not playerData[player.UserId] then return end

    local success, _ = pcall(function()
        HttpService:PostAsync(config.APIEndpoint .. "/player/" .. player.UserId, 
            HttpService:JSONEncode(playerData[player.UserId]))
    end)

    if not success then
        warn("Failed to save player data for " .. player.Name)
    end
end

-- Send to API
function XBedrockServer:SendToAPI(endpoint, data)
    local success, _ = pcall(function()
        HttpService:PostAsync(config.APIEndpoint .. "/" .. endpoint,
            HttpService:JSONEncode(data))
    end)

    if not success then
        warn("Failed to send data to API: " .. endpoint)
    end
end

-- Sync player
function XBedrockServer:SyncPlayer(player)
    if not playerData[player.UserId] then return end

    -- Sync position
    remoteEvents.Position:FireClient(player, playerData[player.UserId].position)

    -- Sync inventory
    if config.EnableInventory then
        remoteEvents.Inventory:FireClient(player, playerData[player.UserId].inventory)
    end

    -- Sync cosmetics
    if config.EnableCosmetics then
        remoteEvents.Cosmetics:FireClient(player, playerData[player.UserId].cosmetics)
    end

    -- Sync webstore
    if config.EnableWebstore then
        remoteEvents.Webstore:FireClient(player, playerData[player.UserId].webstore)
    end
end

-- Create new instance
function XBedrockServer.new()
    local self = setmetatable({}, XBedrockServer)
    return self
end

return XBedrockServer 