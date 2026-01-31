package plugin.config;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import plugin.types.EVENT_TYPE;

import java.util.*;

public class SimpleProtectWorldConfig {
    public boolean protectionEnabled = true;
    public boolean notifyPlayer = true;
    public EnumSet<EVENT_TYPE> enabledProtections = EnumSet.allOf(EVENT_TYPE.class);
    public Set<UUID> members = new HashSet<>();
    public Set<UUID> moderators = new HashSet<>();
    public Set<UUID> administrators = new HashSet<>();
    public UUID owner;

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
                                for (String protection : val) {
                                    try {
                                        set.add(EVENT_TYPE.valueOf(protection));
                                    } catch (IllegalArgumentException ignored) {
                                        System.out.println("Invalid protection ignored : " + protection);
                                    }
                                }
                                cfg.enabledProtections = set;
                            },
                            cfg -> cfg.enabledProtections.stream()
                                    .map(Enum::name)
                                    .toArray(String[]::new)
                    ).add()
                    .append(
                            new KeyedCodec<>("AllowedPlayers", Codec.STRING_ARRAY),
                            (cfg, val) -> cfg.members.addAll(parseUUIDSet(val)),
                            _ -> null
                    ).add()
                    .append(
                            new KeyedCodec<>("Members", Codec.STRING_ARRAY),
                            (cfg, val) -> cfg.members.addAll(parseUUIDSet(val)),
                            cfg -> cfg.members.stream()
                                    .map(UUID::toString)
                                    .toArray(String[]::new)
                    ).add()
                    .append(
                            new KeyedCodec<>("Moderators", Codec.STRING_ARRAY),
                            (cfg, val) -> cfg.moderators.addAll(parseUUIDSet(val)),
                            cfg -> cfg.moderators.stream()
                                    .map(UUID::toString)
                                    .toArray(String[]::new)
                    ).add()
                    .append(
                            new KeyedCodec<>("Administrators", Codec.STRING_ARRAY),
                            (cfg, val) -> cfg.administrators.addAll(parseUUIDSet(val)),
                            cfg -> cfg.administrators.stream()
                                    .map(UUID::toString)
                                    .toArray(String[]::new)
                    ).add()
                    .append(
                            new KeyedCodec<>("Owner", Codec.STRING),
                            (cfg, val) -> {
                                if (val != null && !val.isEmpty()) {
                                    try {
                                        cfg.owner = UUID.fromString(val);
                                    } catch (IllegalArgumentException e) {
                                        System.out.println("Invalid Owner UUID ignored: " + val);
                                        cfg.owner = null;
                                    }
                                } else {
                                    cfg.owner = null;
                                }
                            },
                            cfg -> cfg.owner != null ? cfg.owner.toString() : null
                    )
                    .add()
                    .build();

    private static Set<UUID> parseUUIDSet(String[] val) {
        Set<UUID> set = new HashSet<>();
        if (val != null) {
            for (String uuid : val) {
                try {
                    set.add(UUID.fromString(uuid));
                } catch (IllegalArgumentException ignored) {
                    System.out.println("Invalid UUID ignored : " + uuid);
                }
            }
        }
        return set;
    }

    public SimpleProtectWorldConfig(SimpleProtectWorldConfig target) {
        protectionEnabled = target.protectionEnabled;
        notifyPlayer = target.notifyPlayer;

        enabledProtections = target.enabledProtections != null ? target.enabledProtections.clone() : EnumSet.noneOf(EVENT_TYPE.class);
        members = target.members != null ? new HashSet<>(target.members) : new HashSet<>();
        moderators = target.moderators != null ? new HashSet<>(target.moderators) : new HashSet<>();
        administrators = target.administrators != null ? new HashSet<>(target.administrators) : new HashSet<>();
        owner = target.owner;
    }

    public SimpleProtectWorldConfig() {}
}
