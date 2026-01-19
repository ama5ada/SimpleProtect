package plugin;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;

/**
 * Plain data class representing plugin config
 */
public class PluginConfig {

    // Default values for config
    public boolean protectionEnabled = true;
    public boolean notifyPlayer = true;

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
            .build();

    public void setProtectionEnabled(boolean value) { this.protectionEnabled = value; }
    public void setNotifyPlayer(boolean value) { this.notifyPlayer = value; }
}
