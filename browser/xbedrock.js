class XBedrock {
    constructor(config = {}) {
        this.config = {
            serverUrl: config.serverUrl || 'ws://localhost:8080',
            syncInterval: config.syncInterval || 1000,
            features: {
                chat: config.features?.chat ?? true,
                inventory: config.features?.inventory ?? true,
                cosmetics: config.features?.cosmetics ?? true,
                webstore: config.features?.webstore ?? true
            }
        };

        this.connected = false;
        this.ws = null;
        this.playerData = {
            position: { x: 0, y: 0, z: 0 },
            inventory: [],
            cosmetics: [],
            webstore: {}
        };

        this.eventHandlers = {
            chat: [],
            position: [],
            inventory: [],
            cosmetics: [],
            webstore: [],
            connect: [],
            disconnect: [],
            error: []
        };
    }

    // Connect to server
    connect() {
        if (this.connected) return;

        this.ws = new WebSocket(this.config.serverUrl);

        this.ws.onopen = () => {
            this.connected = true;
            this._triggerEvent('connect');
            this._startSyncLoop();
        };

        this.ws.onclose = () => {
            this.connected = false;
            this._triggerEvent('disconnect');
        };

        this.ws.onerror = (error) => {
            this._triggerEvent('error', error);
        };

        this.ws.onmessage = (event) => {
            try {
                const data = JSON.parse(event.data);
                this._handleMessage(data);
            } catch (error) {
                this._triggerEvent('error', error);
            }
        };
    }

    // Disconnect from server
    disconnect() {
        if (!this.connected) return;

        this.ws.close();
        this.connected = false;
    }

    // Send chat message
    sendChat(message) {
        if (!this.config.features.chat) return;
        this._send('chat', { message });
    }

    // Send position
    sendPosition(position) {
        this._send('position', { position });
    }

    // Update inventory
    updateInventory(inventory) {
        if (!this.config.features.inventory) return;
        this._send('inventory', { inventory });
    }

    // Update cosmetics
    updateCosmetics(cosmetics) {
        if (!this.config.features.cosmetics) return;
        this._send('cosmetics', { cosmetics });
    }

    // Update webstore
    updateWebstore(webstore) {
        if (!this.config.features.webstore) return;
        this._send('webstore', { webstore });
    }

    // Get player data
    getPlayerData() {
        return { ...this.playerData };
    }

    // Event handling
    on(event, handler) {
        if (this.eventHandlers[event]) {
            this.eventHandlers[event].push(handler);
        }
    }

    off(event, handler) {
        if (this.eventHandlers[event]) {
            this.eventHandlers[event] = this.eventHandlers[event].filter(h => h !== handler);
        }
    }

    // Private methods
    _send(type, data) {
        if (!this.connected) return;

        this.ws.send(JSON.stringify({
            type,
            data
        }));
    }

    _handleMessage(message) {
        const { type, data } = message;

        switch (type) {
            case 'chat':
                if (this.config.features.chat) {
                    this._triggerEvent('chat', data);
                }
                break;

            case 'position':
                this.playerData.position = data.position;
                this._triggerEvent('position', data);
                break;

            case 'inventory':
                if (this.config.features.inventory) {
                    this.playerData.inventory = data.inventory;
                    this._triggerEvent('inventory', data);
                }
                break;

            case 'cosmetics':
                if (this.config.features.cosmetics) {
                    this.playerData.cosmetics = data.cosmetics;
                    this._triggerEvent('cosmetics', data);
                }
                break;

            case 'webstore':
                if (this.config.features.webstore) {
                    this.playerData.webstore = data.webstore;
                    this._triggerEvent('webstore', data);
                }
                break;
        }
    }

    _triggerEvent(event, data) {
        if (this.eventHandlers[event]) {
            this.eventHandlers[event].forEach(handler => handler(data));
        }
    }

    _startSyncLoop() {
        setInterval(() => {
            if (!this.connected) return;

            // Sync position
            this.sendPosition(this.playerData.position);

            // Sync inventory
            if (this.config.features.inventory) {
                this.updateInventory(this.playerData.inventory);
            }

            // Sync cosmetics
            if (this.config.features.cosmetics) {
                this.updateCosmetics(this.playerData.cosmetics);
            }

            // Sync webstore
            if (this.config.features.webstore) {
                this.updateWebstore(this.playerData.webstore);
            }
        }, this.config.syncInterval);
    }
}

// Export for different module systems
if (typeof module !== 'undefined' && module.exports) {
    module.exports = XBedrock;
} else if (typeof define === 'function' && define.amd) {
    define([], function() { return XBedrock; });
} else {
    window.XBedrock = XBedrock;
} 