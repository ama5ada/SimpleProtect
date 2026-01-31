package plugin.gui.pages;

import plugin.UUIDCache;
import plugin.db.PlayerInfoDB;
import plugin.types.PLAYER_ROLE;
import plugin.config.SimpleProtectWorldConfig;

import java.util.*;
import java.util.concurrent.CompletableFuture;

public final class PlayerListService {
    public record PlayerEntry(UUID uuid, String displayName) {}

    public CompletableFuture<PageResult<PlayerEntry>> buildAllowedPlayersPageAsync(
            SimpleProtectWorldConfig config,
            PLAYER_ROLE role,
            String search,
            int page,
            int pageSize
    ) {
        return CompletableFuture.supplyAsync(() -> {

            Set<UUID> source = switch (role) {
                case OWNER -> config.administrators;
                case MODERATOR -> config.moderators;
                case MEMBER -> config.members;
            };

            String searchLower = normalize(search);

            List<UUID> candidates = new ArrayList<>(source);
            List<UUID> cacheMisses = new ArrayList<>();

            for (UUID uuid : candidates) {
                if (UUIDCache.get().getNameFromUUID(uuid) == null) {
                    cacheMisses.add(uuid);
                }
            }

            if (!cacheMisses.isEmpty()) {
                PlayerInfoDB.queryPlayersByUUIDsAsync(cacheMisses, dbResult -> {
                    dbResult.forEach(UUIDCache.get()::putPlayerInfo);
                });
            }

            List<PlayerEntry> entries = new ArrayList<>();

            for (UUID uuid : candidates) {
                String name = UUIDCache.get().getNameFromUUID(uuid);
                if (name == null) {
                    name = uuid.toString();
                    entries.add(new PlayerEntry(uuid, name));
                } else if (searchLower.isEmpty()
                        || name.toLowerCase().startsWith(searchLower)){
                    entries.add(new PlayerEntry(uuid, name));
                }
            }

            entries.sort(Comparator.comparing(
                    PlayerEntry::displayName,
                    String.CASE_INSENSITIVE_ORDER
            ));

            return paginate(entries, page, pageSize);
        });
    }

    public CompletableFuture<PageResult<PlayerEntry>> buildDisallowedPlayersPageAsync(
            SimpleProtectWorldConfig config,
            String search,
            int page,
            int pageSize
    ) {
        return CompletableFuture.supplyAsync(() -> {

            Set<UUID> excluded = new HashSet<>();
            excluded.addAll(config.administrators);
            excluded.addAll(config.moderators);
            excluded.addAll(config.members);
            excluded.add(config.owner);

            String searchLower = normalize(search);

            PlayerInfoDB.queryPlayersAsync(searchLower, dbResult -> {
                dbResult.forEach(UUIDCache.get()::putPlayerInfo);
            });

            List<PlayerEntry> entries = new ArrayList<>();

            for (Map.Entry<UUID, String> entry : UUIDCache.get().getEntries()) {
                UUID uuid = entry.getKey();
                String name = entry.getValue();

                if (excluded.contains(uuid)) continue;

                if (searchLower.isEmpty()
                        || name.toLowerCase().startsWith(searchLower)) {
                    entries.add(new PlayerEntry(uuid, name));
                }
                if (name == null) {
                    name = uuid.toString();
                    entries.add(new PlayerEntry(uuid, name));
                }
            }

            entries.sort(Comparator.comparing(
                    PlayerEntry::displayName,
                    String.CASE_INSENSITIVE_ORDER
            ));

            return paginate(entries, page, pageSize);
        });
    }

    private static String normalize(String search) {
        return search == null ? "" : search.toLowerCase();
    }

    private static <T> PageResult<T> paginate(
            List<T> entries,
            int page,
            int pageSize
    ) {
        int total = entries.size();
        int from = Math.min(page * pageSize, total);
        int to = Math.min(from + pageSize, total);

        return new PageResult<>(
                entries.subList(from, to),
                page,
                pageSize,
                total
        );
    }
}
