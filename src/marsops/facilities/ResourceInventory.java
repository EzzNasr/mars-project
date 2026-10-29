package marsops.facilities;

import java.util.EnumMap;
import java.util.Map;
import marsops.common.ResourceType;

/**
 * Plain stock book-keeping for one facility. Stock never goes negative.
 *
 * @author A
 */
public final class ResourceInventory {
    private final Map<ResourceType, Integer> stock = new EnumMap<>(ResourceType.class);

    /**
     * Returns the stock of a resource.
     *
     * @param type the resource
     * @return units stored (0 if none)
     */
    public int get(ResourceType type) {
        return stock.getOrDefault(type, 0);
    }

    /**
     * Adds units of a resource.
     *
     * @param type   the resource
     * @param amount units to add (must be positive)
     * @throws IllegalArgumentException if type is null or amount is not positive
     */
    public void add(ResourceType type, int amount) {
        if (type == null || amount <= 0) {
            throw new IllegalArgumentException("type required and amount must be > 0");
        }
        stock.merge(type, amount, Integer::sum);
    }

    /**
     * Removes units only if enough are stored.
     *
     * @param type   the resource
     * @param amount units to remove (must be positive)
     * @return true if removed; false (and no change) if stock is lower than amount
     * @throws IllegalArgumentException if type is null or amount is not positive
     */
    public boolean remove(ResourceType type, int amount) {
        if (type == null || amount <= 0) {
            throw new IllegalArgumentException("type required and amount must be > 0");
        }
        int current = get(type);
        if (current < amount) return false;
        stock.put(type, current - amount);
        return true;
    }

    /**
     * Removes as many units as available, up to amount.
     *
     * @param type   the resource
     * @param amount maximum units to remove (not negative)
     * @return units actually removed
     * @throws IllegalArgumentException if type is null or amount is negative
     */
    public int removeUpTo(ResourceType type, int amount) {
        if (type == null || amount < 0) {
            throw new IllegalArgumentException("type required and amount must be >= 0");
        }
        int removed = Math.min(amount, get(type));
        stock.put(type, get(type) - removed);
        return removed;
    }

    /** @return total units of all resources */
    public int total() {
        int sum = 0;
        for (int v : stock.values()) sum += v;
        return sum;
    }

    /** @return a copy of the stock, never null */
    public Map<ResourceType, Integer> snapshot() {
        return new EnumMap<>(stock);
    }
}