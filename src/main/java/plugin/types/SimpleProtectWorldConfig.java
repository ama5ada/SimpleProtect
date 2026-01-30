package plugin.types;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;

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
                            new KeyedCodec<>("Members", Codec.STRING_ARRAY),
                            (cfg, val) -> {
                                Set<UUID> set = new HashSet<>();
                                for (String s : val) {
                                    set.add(UUID.fromString(s));
                                }
                                cfg.members = set;
                            },
                            cfg -> cfg.members.stream()
                                    .map(UUID::toString)
                                    .toArray(String[]::new)
                    ).add()
                    .append(
                            new KeyedCodec<>("Moderators", Codec.STRING_ARRAY),
                            (cfg, val) -> {
                                Set<UUID> set = new HashSet<>();
                                for (String s : val) {
                                    set.add(UUID.fromString(s));
                                }
                                cfg.moderators = set;
                            },
                            cfg -> cfg.moderators.stream()
                                    .map(UUID::toString)
                                    .toArray(String[]::new)
                    ).add()
                    .append(
                            new KeyedCodec<>("Administrators", Codec.STRING_ARRAY),
                            (cfg, val) -> {
                                Set<UUID> set = new HashSet<>();
                                for (String s : val) {
                                    set.add(UUID.fromString(s));
                                }
                                cfg.administrators = set;
                            },
                            cfg -> cfg.administrators.stream()
                                    .map(UUID::toString)
                                    .toArray(String[]::new)
                    ).add()
                    .append(
                            new KeyedCodec<>("Owner", Codec.STRING),
                            (cfg, val) -> cfg.owner = UUID.fromString(val),
                            cfg -> cfg.owner != null ? cfg.owner.toString() : ""
                    ).add()
                    .build();

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
