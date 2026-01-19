package plugin;

import com.hypixel.hytale.server.core.util.Config;

public class ConfigState {

    private static ConfigState instance;
    private final Config<PluginConfig> config;

    private ConfigState(Config<PluginConfig> config) {
        this.config = config;
    }

    public static void init(Config<PluginConfig> config) {
        if (instance == null) {
            instance = new ConfigState(config);
        }
    }

    public static ConfigState get() {
        if (instance == null) throw new IllegalStateException("ConfigState not initialized!");
        return instance;
    }

    private PluginConfig getData() {
        try {
            return config.get();
        } catch (IllegalStateException e) {
            throw new IllegalStateException("Plugin config is not loaded yet!");
        }
    }

    public String getPermission() {
        return "com.simpleprotect.bypass";
    }

    public boolean isProtected() { return getData().protectionEnabled; }

    public void setProtected(Boolean value) {
        getData().protectionEnabled = value;
        config.save();
    }

    public boolean notifyPlayer() { return getData().notifyPlayer; }

    public void setNotifyPlayer(Boolean value) {
        getData().notifyPlayer = value;
        config.save();
    }
}
