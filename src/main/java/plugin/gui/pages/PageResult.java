package plugin.gui.pages;

import java.util.List;

public final class PageResult<T> {

    private final List<T> items;
    private final int page;        // zero-based
    private final int pageSize;
    private final int totalItems;

    public PageResult(
            List<T> items,
            int page,
            int pageSize,
            int totalItems
    ) {
        this.items = List.copyOf(items);
        this.page = page;
        this.pageSize = pageSize;
        this.totalItems = totalItems;
    }

    public List<T> items() {
        return items;
    }

    public int page() {
        return page;
    }

    public int pageSize() {
        return pageSize;
    }

    public int totalItems() {
        return totalItems;
    }

    public int totalPages() {
        if (totalItems == 0) return 0;
        return (totalItems + pageSize - 1) / pageSize;
    }

    public boolean hasNext() {
        return page + 1 < totalPages();
    }

    public boolean hasPrevious() {
        return page > 0;
    }
}
