package plugin;

import com.hypixel.hytale.server.core.util.Config;
import plugin.config.SimpleProtectWorldConfig;

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
        return "mods.simpleprotect.bypass";
    }
    public String getAdministratePermission() { return "mods.simpleprotect.administrate"; }

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

    public boolean isVerbose() { return getData().verbose; }

    public void setVerbose(Boolean value) {
        getData().verbose = value;
        config.save();
    }

    public void updateGlobalConfig(boolean isProtected, boolean canNotify, boolean isVerbose) {
        getData().protectionEnabled = isProtected;
        getData().notifyPlayer = canNotify;
        getData().verbose = isVerbose;
        config.save();
    }

    public SimpleProtectWorldConfig getWorldProtectionConfig(String world) {
        // Try to get the queried world config, if it doesn't exist return default
        return getData().worlds.getOrDefault(world, getData().defaultWorldConfig);
    }

    public void setWorldProtectionConfig(String world, SimpleProtectWorldConfig worldConfig) {
        getData().setWorld(world, worldConfig);
        config.save();
    }

    public void deleteWorldProtectionConfig(String world) {
        // Do not allow the default world to be deleted, an empty list of worlds will cause the CODEC to fail
        // So this is the guard entry against that failure
        if (!world.equals("default")) getData().removeWorld(world);
        config.save();
    }

    public SimpleProtectWorldConfig getDefaultWorldConfig() {
        return getData().getDefaultWorldConfig();
    }

    public void updateDefaultWorldConfig(SimpleProtectWorldConfig worldConfig) {
        getData().setDefaultConfig(worldConfig);
        config.save();
    }

    public void initializeDefaults() {
        // Try to set defaults, if the defaults had to be set (first run) then save the config
        if (getData().initializeDefaults()) config.save();
    }

    public String[] getWorldNames() {
        return getData().worlds.keySet().toArray(String[]::new);
    }
}
