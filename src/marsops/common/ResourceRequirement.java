package marsops.common;
import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
/** "How much of each resource an operation needs", e.g. new ResourceRequirement().with(SPARE_PARTS, 2).with(BATTERY_POWER, 5). */
public class ResourceRequirement {
    private final Map<ResourceType, Integer> amounts = new EnumMap<>(ResourceType.class);
    public ResourceRequirement with(ResourceType type, int amount) {
        if (type == null || amount <= 0) throw new IllegalArgumentException("type required and amount must be > 0");
        amounts.put(type, amount);
        return this;
    }
    public int get(ResourceType type) { return amounts.getOrDefault(type, 0); }
    public Map<ResourceType, Integer> asMap() { return Collections.unmodifiableMap(amounts); }
    public boolean isEmpty() { return amounts.isEmpty(); }
    @Override public String toString() { return amounts.toString(); }
}
