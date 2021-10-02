package dev.rikka.tools.refine;

@RefineFor(String.class)
public class StringHidden {
    public static class Inner {
        public static void innerMethod() {
            throw new IllegalArgumentException("Stub!");
        }
    }

    @RefineName("hiddenMethod")
    public static void hiddenMethodV24() {
        throw new IllegalArgumentException("Stub!");
    }

    public static String FIELD;
}
