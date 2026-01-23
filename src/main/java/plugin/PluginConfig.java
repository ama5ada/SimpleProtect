package plugin;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.codecs.map.MapCodec;
import plugin.types.SimpleProtectWorldConfig;

import java.util.HashMap;
import java.util.Map;

/**
 * Plain data class representing plugin config
 */
public class PluginConfig {

    // Default values for config
    public boolean protectionEnabled = true;
    public boolean notifyPlayer = true;
    public boolean verbose = true;

    public SimpleProtectWorldConfig defaultWorldConfig = new SimpleProtectWorldConfig();
    public Map<String, SimpleProtectWorldConfig> worlds = new HashMap<>();

    public boolean initializeDefaults() {
        if (this.defaultWorldConfig == null) {
            this.defaultWorldConfig = new SimpleProtectWorldConfig();
        }

        this.worlds.entrySet().removeIf(entry -> entry.getKey() == null);
        if (this.worlds.isEmpty()) {
            SimpleProtectWorldConfig copyDefault = new SimpleProtectWorldConfig();
            copyDefault.applyDefaults(defaultWorldConfig);
            this.worlds.put("default", copyDefault);
            return true;
        }
        return false;
    }

    public static final Codec<Map<String, SimpleProtectWorldConfig>> WORLDS_CODEC =
            new MapCodec<>(SimpleProtectWorldConfig.CODEC, HashMap::new, false);

    public static final BuilderCodec<PluginConfig> CODEC = BuilderCodec
            .builder(PluginConfig.class, PluginConfig::new)
            .append(
                    new KeyedCodec<>("ProtectionEnabled", Codec.BOOLEAN),
                    (cfg, val) -> cfg.protectionEnabled = val,
                    cfg -> cfg.protectionEnabled)
            .add()
            .append(
                    new KeyedCodec<>("NotifyPlayer", Codec.BOOLEAN),
                    (cfg, val) -> cfg.notifyPlayer = val,
                    cfg -> cfg.notifyPlayer)
            .add()
            .append(
                    new KeyedCodec<>("Verbose", Codec.BOOLEAN),
                    (cfg, val) -> cfg.verbose = val,
                    cfg ->cfg.verbose)
            .add()
            .append(
                    new KeyedCodec<>("DefaultWorld", SimpleProtectWorldConfig.CODEC),
                    (cfg, val) -> cfg.defaultWorldConfig = val,
                    cfg -> cfg.defaultWorldConfig
            ).add()
            .append(
                    new KeyedCodec<>("Worlds", WORLDS_CODEC),
                    (cfg, val) -> cfg.worlds = val,
                    cfg -> cfg.worlds
            ).add()
            .build();


    public void setProtectionEnabled(boolean value) { this.protectionEnabled = value; }
    public void setNotifyPlayer(boolean value) { this.notifyPlayer = value; }
    public void setVerbose(boolean value) { this.verbose = value; }

    public void setWorlds(Map<String, SimpleProtectWorldConfig> worlds) {
        if (worlds == null) {
            worlds = new HashMap<String, SimpleProtectWorldConfig>();
            worlds.put("default", new SimpleProtectWorldConfig());
        }
        worlds.entrySet().removeIf(entry -> entry.getKey() == null);
        worlds.forEach((worldId, worldCfg) -> worldCfg.applyDefaults(this.defaultWorldConfig));
        this.worlds = worlds;
    }

    public void addWorld(String worldId) {
        SimpleProtectWorldConfig config = new SimpleProtectWorldConfig();
        config.applyDefaults(this.defaultWorldConfig);
        this.worlds.put(worldId, config);
    }
}
