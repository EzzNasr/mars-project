package marsops.common;
/** A piece of equipment (e.g. "Spectrometer"). Can be damaged and repaired. */
public class Equipment {
    private final String name;
    private boolean functional = true;
    public Equipment(String name) {
        if (name == null || name.isBlank()) throw new IllegalArgumentException("name required");
        this.name = name;
    }
    public String getName() { return name; }
    public boolean isFunctional() { return functional; }
    public void damage() { functional = false; }
    public void repair() { functional = true; }
    @Override public String toString() { return name + (functional ? "" : " (damaged)"); }
}
