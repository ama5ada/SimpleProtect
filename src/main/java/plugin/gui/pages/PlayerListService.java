package plugin.gui.pages;

import plugin.UUIDCache;
import plugin.db.PlayerInfoDB;
import plugin.types.PLAYER_ROLE;
import plugin.config.SimpleProtectWorldConfig;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

public final class PlayerListService {
    public record PlayerEntry(UUID uuid, String displayName) {}

    public CompletableFuture<PageResult<PlayerEntry>> buildAllowedPlayersPageAsync(
            SimpleProtectWorldConfig config,
            PLAYER_ROLE role,
            String search,
            int page,
            int pageSize,
            Consumer<PageResult<PlayerEntry>> dbUpdate
    ) {
        return CompletableFuture.supplyAsync(() -> {
            Set<UUID> source = switch (role) {
                case ADMINISTRATOR -> config.administrators;
                case MODERATOR -> config.moderators;
                default -> config.members;
            };

            List<UUID> cacheMisses = new ArrayList<>();
            for (UUID uuid : source) {
                if (UUIDCache.get().getNameFromUUID(uuid) == null) {
                    cacheMisses.add(uuid);
                }
            }

            if (!cacheMisses.isEmpty()) {
                PlayerInfoDB.queryPlayersByUUIDsAsync(cacheMisses, dbResult -> {
                    dbResult.forEach(UUIDCache.get()::putPlayerInfo);

                    dbUpdate.accept(
                            buildCachedPlayerResult(source, search, page, pageSize)
                    );
                });
            }

            return buildCachedPlayerResult(source, search, page, pageSize);
        });
    }

    private PageResult<PlayerEntry> buildCachedPlayerResult(
            Set<UUID> source,
            String search,
            int page,
            int pageSize
    ) {
        String searchLower = normalize(search);

        List<PlayerEntry> entries = new ArrayList<>();

        for (UUID uuid : source) {
            String name = UUIDCache.get().getNameFromUUID(uuid);

            if (name == null) {
                entries.add(new PlayerEntry(uuid, uuid.toString()));
            } else if (searchLower.isEmpty()
                    || name.toLowerCase().startsWith(searchLower)) {
                entries.add(new PlayerEntry(uuid, name));
            }
        }

        entries.sort(Comparator.comparing(
                PlayerEntry::displayName,
                String.CASE_INSENSITIVE_ORDER
        ));

        return paginate(entries, page, pageSize);
    }

    public CompletableFuture<PageResult<PlayerEntry>> buildDisallowedPlayersPageAsync(
            SimpleProtectWorldConfig config,
            String search,
            int page,
            int pageSize,
            Consumer<PageResult<PlayerEntry>> dbUpdate
    ) {
        return CompletableFuture.supplyAsync(() -> {
            Set<UUID> excluded = new HashSet<>();
            excluded.addAll(config.administrators);
            excluded.addAll(config.moderators);
            excluded.addAll(config.members);
            excluded.add(config.owner);

            PlayerInfoDB.queryPlayersAsync(normalize(search), dbResult -> {
                dbResult.forEach(UUIDCache.get()::putPlayerInfo);

                dbUpdate.accept(
                        buildCachedDisallowedPlayerResult(
                                excluded, search, page, pageSize
                        )
                );
            });

            return buildCachedDisallowedPlayerResult(
                    excluded, search, page, pageSize
            );
        });
    }

    private PageResult<PlayerEntry> buildCachedDisallowedPlayerResult(
            Set<UUID> excluded,
            String search,
            int page,
            int pageSize
    ) {
        String searchLower = normalize(search);

        List<PlayerEntry> entries = new ArrayList<>();

        for (Map.Entry<UUID, String> entry : UUIDCache.get().getEntries()) {
            UUID uuid = entry.getKey();
            String name = entry.getValue();

            if (excluded.contains(uuid)) continue;

            if (name == null) {
                entries.add(new PlayerEntry(uuid, uuid.toString()));
            } else if (searchLower.isEmpty()
                    || name.toLowerCase().startsWith(searchLower)) {
                entries.add(new PlayerEntry(uuid, name));
            }
        }

        entries.sort(Comparator.comparing(
                PlayerEntry::displayName,
                String.CASE_INSENSITIVE_ORDER
        ));

        return paginate(entries, page, pageSize);
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
