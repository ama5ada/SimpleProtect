package plugin.types;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;

import java.util.*;

public class SimpleProtectWorldConfig {
    public boolean protectionEnabled = true;
    public boolean notifyPlayer = true;
    public EnumSet<EVENT_TYPE> enabledProtections = EnumSet.allOf(EVENT_TYPE.class);
    public Set<UUID> allowedPlayers = new HashSet<>();

    public static final BuilderCodec<SimpleProtectWorldConfig> CODEC =
            BuilderCodec.builder(SimpleProtectWorldConfig.class, SimpleProtectWorldConfig::new)
                    .append(
                            new KeyedCodec<>("ProtectionEnabled", Codec.BOOLEAN),
                            (cfg, val) -> cfg.protectionEnabled = val,
                            cfg -> cfg.protectionEnabled
                    ).add()
                    .append(
                            new KeyedCodec<>("NotifyPlayer", Codec.BOOLEAN),
                            (cfg, val) -> cfg.notifyPlayer = val,
                            cfg -> cfg.notifyPlayer
                    ).add()
                    .append(
                            new KeyedCodec<>("EnabledProtections", Codec.STRING_ARRAY),
                            (cfg, val) -> {
                                EnumSet<EVENT_TYPE> set = EnumSet.noneOf(EVENT_TYPE.class);
                                for (String s : val) {
                                    set.add(EVENT_TYPE.valueOf(s));
                                }
                                cfg.enabledProtections = set;
                            },
                            cfg -> cfg.enabledProtections.stream()
                                    .map(Enum::name)
                                    .toArray(String[]::new)
                    ).add()
                    .append(
                            new KeyedCodec<>("AllowedPlayers", Codec.STRING_ARRAY),
                            (cfg, val) -> {
                                Set<UUID> set = new HashSet<>();
                                for (String s : val) {
                                    set.add(UUID.fromString(s));
                                }
                                cfg.allowedPlayers = set;
                            },
                            cfg -> cfg.allowedPlayers.stream()
                                    .map(UUID::toString)
                                    .toArray(String[]::new)
                    ).add()
                    .build();

    public void applyDefaults(SimpleProtectWorldConfig defaults) {
        if (enabledProtections.isEmpty()) {
            enabledProtections.addAll(defaults.enabledProtections);
        }
        if (allowedPlayers.isEmpty()) {
            allowedPlayers.addAll(defaults.allowedPlayers);
        }

        protectionEnabled = protectionEnabled || defaults.protectionEnabled;
        notifyPlayer = notifyPlayer || defaults.notifyPlayer;
    }

    public SimpleProtectWorldConfig(SimpleProtectWorldConfig target) {
        this.protectionEnabled = target.protectionEnabled;
        this.notifyPlayer = target.notifyPlayer;
        this.enabledProtections = target.enabledProtections.clone();
        this.allowedPlayers = new HashSet<>(target.allowedPlayers);
    }

    public SimpleProtectWorldConfig() {}
}
