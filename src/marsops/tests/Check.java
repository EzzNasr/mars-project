package marsops.tests;
/** Tiny test helper (no JUnit needed). Every test file calls Check.* and ends with Check.summary(). */
public final class Check {
    private static int passed = 0;
    private static int failed = 0;
    private Check() { }
    public static void section(String title) { System.out.println("\n== " + title + " =="); }
    public static void isTrue(String name, boolean condition) {
        if (condition) { passed++; System.out.println("  PASS  " + name); }
        else { failed++; System.out.println("  FAIL  " + name); }
    }
    public static void equal(String name, Object expected, Object actual) {
        boolean ok = java.util.Objects.equals(expected, actual);
        isTrue(ok ? name : name + "  [expected=" + expected + ", actual=" + actual + "]", ok);
    }
    /** Passes if the text contains the part, ignoring upper/lower case. */
    public static void contains(String name, String text, String part) {
        boolean ok = text != null && text.toLowerCase().contains(part.toLowerCase());
        isTrue(ok ? name : name + "  [text=\"" + text + "\" should contain \"" + part + "\"]", ok);
    }
    public static void throwsException(String name, Class<? extends Throwable> type, Runnable action) {
        try { action.run(); isTrue(name + "  [no exception thrown]", false); }
        catch (Throwable t) { isTrue(name, type.isInstance(t)); }
    }
    public static void summary() {
        System.out.println("\nRESULT: " + passed + " passed, " + failed + " failed");
        if (failed > 0) System.exit(1);
    }
}
