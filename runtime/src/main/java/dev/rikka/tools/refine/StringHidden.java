package dev.rikka.tools.refine;

@RefineFor(String.class)
public class StringHidden {
    @RefineName("renamedHiddenMethod")
    public static void hiddenMethod() {
        throw new IllegalArgumentException("Stub!");
    }
}
