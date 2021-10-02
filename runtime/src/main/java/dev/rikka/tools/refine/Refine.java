package dev.rikka.tools.refine;

@RefineUse(StringHidden.class)
public final class Refine {
    @SuppressWarnings("unchecked")
    public static <T> T unsafeCast(Object obj) {
        java.lang.String.Inner.innerMethod();

        return (T) obj;
    }
}
