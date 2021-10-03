package dev.rikka.tools.refine;

import java.lang.annotation.*;

@Retention(RetentionPolicy.CLASS)
@Target({ElementType.TYPE})
public @interface UseRefines {
    Class<?>[] value();
}
