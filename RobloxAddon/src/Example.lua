-- Example usage of X-Bedrock addon
local ReplicatedStorage = game:GetService("ReplicatedStorage")
local Players = game:GetService("Players")
local XBedrock = require(ReplicatedStorage.XBedrock)

-- Initialize X-Bedrock with custom configuration
local xBedrock = XBedrock.new({
    ServerIP = "your-server-ip", -- Replace with your server IP
    ServerPort = 19132,
    SyncInterval = 0.5, -- Update position every 0.5 seconds
    EnableChat = true,
    EnableInventory = true,
    EnableCosmetics = true
})

-- Connect to the server
xBedrock:Connect()

-- Handle incoming chat messages
xBedrock:OnChat(function(message)
    -- Create a chat bubble above the player's head
    local player = Players:GetPlayerByUserId(message.sender)
    if player and player.Character then
        local head = player.Character:FindFirstChild("Head")
        if head then
            local chatBubble = Instance.new("BillboardGui")
            chatBubble.Size = UDim2.new(0, 200, 0, 50)
            chatBubble.StudsOffset = Vector3.new(0, 3, 0)
            chatBubble.Adornee = head
            
            local textLabel = Instance.new("TextLabel")
            textLabel.Size = UDim2.new(1, 0, 1, 0)
            textLabel.BackgroundTransparency = 1
            textLabel.Text = message.content
            textLabel.TextColor3 = Color3.new(1, 1, 1)
            textLabel.TextScaled = true
            textLabel.Parent = chatBubble
            
            chatBubble.Parent = head
            
            -- Remove chat bubble after 5 seconds
            task.delay(5, function()
                chatBubble:Destroy()
            end)
        end
    end
end)

-- Handle position updates
xBedrock:OnPosition(function(position)
    -- Update player position if they're in a specific state
    local player = Players.LocalPlayer
    if player and player.Character then
        local humanoid = player.Character:FindFirstChild("Humanoid")
        if humanoid and humanoid.Sit then
            -- Only update position if player is sitting (teleporting)
            player.Character:SetPrimaryPartCFrame(CFrame.new(position.x, position.y, position.z))
        end
    end
end)

-- Handle inventory updates
xBedrock:OnInventory(function(items)
    -- Update player's inventory
    local player = Players.LocalPlayer
    if player then
        local backpack = player:FindFirstChild("Backpack")
        if backpack then
            -- Clear existing items
            for _, item in ipairs(backpack:GetChildren()) do
                if item:IsA("Tool") then
                    item:Destroy()
                end
            end
            
            -- Add new items
            for _, item in ipairs(items) do
                local tool = Instance.new("Tool")
                tool.Name = item.id
                -- Add item properties here
                tool.Parent = backpack
            end
        end
    end
end)

-- Handle cosmetics updates
xBedrock:OnCosmetics(function(cosmetics)
    -- Update player's cosmetics
    local player = Players.LocalPlayer
    if player and player.Character then
        -- Update hat
        if cosmetics.hat then
            local hat = player.Character:FindFirstChild("Hat")
            if hat then
                hat:Destroy()
            end
            -- Create new hat based on cosmetics.hat
            -- Implementation depends on your hat system
        end
        
        -- Update cape
        if cosmetics.cape then
            local cape = player.Character:FindFirstChild("Cape")
            if cape then
                cape:Destroy()
            end
            -- Create new cape based on cosmetics.cape
            -- Implementation depends on your cape system
        end
    end
end)

-- Example: Send chat message when player chats
local function onPlayerChatted(message)
    if message:sub(1, 1) == "/" then
        -- Handle commands
        local command = message:sub(2):lower()
        if command == "sync" then
            -- Force sync position
            local character = Players.LocalPlayer.Character
            if character then
                xBedrock:SendPosition(character:GetPivot().Position)
            end
        end
    else
        -- Send chat message to Minecraft
        xBedrock:SendChat({
            sender = Players.LocalPlayer.UserId,
            content = message
        })
    end
end

-- Connect chat event
Players.LocalPlayer.Chatted:Connect(onPlayerChatted)

-- Example: Sync inventory when player joins
Players.PlayerAdded:Connect(function(player)
    -- Wait for character to load
    player.CharacterAdded:Connect(function(character)
        -- Sync initial inventory
        local items = {}
        local backpack = player:FindFirstChild("Backpack")
        if backpack then
            for _, tool in ipairs(backpack:GetChildren()) do
                if tool:IsA("Tool") then
                    table.insert(items, {
                        id = tool.Name,
                        count = 1
                    })
                end
            end
        end
        xBedrock:SendInventory(items)
    end)
end)

-- Example: Sync cosmetics when player joins
Players.PlayerAdded:Connect(function(player)
    player.CharacterAdded:Connect(function(character)
        local cosmetics = {
            hat = character:FindFirstChild("Hat") and character.Hat.Name or nil,
            cape = character:FindFirstChild("Cape") and character.Cape.Name or nil
        }
        xBedrock:SendCosmetics(cosmetics)
    end)
end) 