local XBedrock = {}
XBedrock.__index = XBedrock

-- Services
local ReplicatedStorage = game:GetService("ReplicatedStorage")
local Players = game:GetService("Players")
local RunService = game:GetService("RunService")
local HttpService = game:GetService("HttpService")

-- Constants
local DEFAULT_CONFIG = {
    ServerIP = "localhost",
    ServerPort = 19132,
    SyncInterval = 1,
    EnableChat = true,
    EnableInventory = true,
    EnableCosmetics = true
}

-- Private variables
local _config = {}
local _connected = false
local _lastSync = 0
local _webSocket = nil
local _eventHandlers = {}

-- Event handlers
local Events = {
    Chat = Instance.new("BindableEvent"),
    Position = Instance.new("BindableEvent"),
    Inventory = Instance.new("BindableEvent"),
    Cosmetics = Instance.new("BindableEvent")
}

-- Utility functions
local function createWebSocket()
    local success, result = pcall(function()
        return WebSocket.connect("ws://" .. _config.ServerIP .. ":" .. _config.ServerPort)
    end)
    
    if success then
        return result
    else
        warn("Failed to create WebSocket connection:", result)
        return nil
    end
end

local function handleMessage(message)
    local data = HttpService:JSONDecode(message)
    
    if data.type == "chat" and _config.EnableChat then
        Events.Chat:Fire(data.message)
    elseif data.type == "position" then
        Events.Position:Fire(data.position)
    elseif data.type == "inventory" and _config.EnableInventory then
        Events.Inventory:Fire(data.items)
    elseif data.type == "cosmetics" and _config.EnableCosmetics then
        Events.Cosmetics:Fire(data.cosmetics)
    end
end

-- Public methods
function XBedrock.new(config)
    local self = setmetatable({}, XBedrock)
    _config = table.clone(DEFAULT_CONFIG)
    
    -- Merge custom config
    for key, value in pairs(config or {}) do
        _config[key] = value
    end
    
    return self
end

function XBedrock:Initialize(config)
    if config then
        for key, value in pairs(config) do
            _config[key] = value
        end
    end
    
    -- Set up WebSocket connection
    _webSocket = createWebSocket()
    if _webSocket then
        _connected = true
        
        -- Set up message handler
        _webSocket.OnMessage:Connect(handleMessage)
        
        -- Set up connection closed handler
        _webSocket.OnClose:Connect(function()
            _connected = false
            -- Attempt to reconnect after 5 seconds
            task.delay(5, function()
                self:Initialize()
            end)
        end)
    end
    
    -- Set up position sync
    if _config.SyncInterval > 0 then
        RunService.Heartbeat:Connect(function()
            local now = tick()
            if now - _lastSync >= _config.SyncInterval then
                _lastSync = now
                self:SendPosition(Players.LocalPlayer.Character and 
                    Players.LocalPlayer.Character:GetPivot().Position or Vector3.new(0, 0, 0))
            end
        end)
    end
end

function XBedrock:Connect()
    if not _connected then
        self:Initialize()
    end
end

function XBedrock:Disconnect()
    if _webSocket then
        _webSocket:Close()
        _webSocket = nil
        _connected = false
    end
end

function XBedrock:SendPosition(position)
    if _connected and _webSocket then
        local data = {
            type = "position",
            position = {
                x = position.X,
                y = position.Y,
                z = position.Z
            }
        }
        _webSocket:Send(HttpService:JSONEncode(data))
    end
end

function XBedrock:SendChat(message)
    if _connected and _webSocket and _config.EnableChat then
        local data = {
            type = "chat",
            message = message
        }
        _webSocket:Send(HttpService:JSONEncode(data))
    end
end

function XBedrock:SendInventory(items)
    if _connected and _webSocket and _config.EnableInventory then
        local data = {
            type = "inventory",
            items = items
        }
        _webSocket:Send(HttpService:JSONEncode(data))
    end
end

function XBedrock:SendCosmetics(cosmetics)
    if _connected and _webSocket and _config.EnableCosmetics then
        local data = {
            type = "cosmetics",
            cosmetics = cosmetics
        }
        _webSocket:Send(HttpService:JSONEncode(data))
    end
end

-- Event handlers
function XBedrock:OnChat(callback)
    Events.Chat.Event:Connect(callback)
end

function XBedrock:OnPosition(callback)
    Events.Position.Event:Connect(callback)
end

function XBedrock:OnInventory(callback)
    Events.Inventory.Event:Connect(callback)
end

function XBedrock:OnCosmetics(callback)
    Events.Cosmetics.Event:Connect(callback)
end

return XBedrock 