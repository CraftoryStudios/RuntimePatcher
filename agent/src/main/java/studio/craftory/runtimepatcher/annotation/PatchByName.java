package studio.craftory.runtimepatcher.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;


@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface PatchByName {

    /**
     * @return The string representation of the patch target as returned by {@link Class#getName()}
     */
    String value();

}
