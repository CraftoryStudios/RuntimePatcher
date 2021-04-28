package studio.craftory.runtimepatcher.annotation;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;


@Retention(RetentionPolicy.RUNTIME)
public @interface Inject {

    /**
     * @return How to inject the code
     */
    InjectionType value() default InjectionType.OVERRIDE;

}
