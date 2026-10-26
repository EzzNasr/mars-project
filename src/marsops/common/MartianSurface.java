package marsops.common;
/** The open Martian surface (single shared instance). */
public final class MartianSurface implements Location {
    public static final MartianSurface INSTANCE = new MartianSurface();
    private MartianSurface() { }
    @Override public String getName() { return "Martian Surface"; }
    @Override public String toString() { return getName(); }
}
