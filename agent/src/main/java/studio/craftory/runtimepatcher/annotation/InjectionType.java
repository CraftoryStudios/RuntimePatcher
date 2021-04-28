package studio.craftory.runtimepatcher.annotation;


public enum InjectionType {
    /**
     * Insert code at the beginning of the method
     */
    INSERT,

    /**
     * Overwrite method
     */
    OVERRIDE,

    /**
     * Append at end of method
     */
    APPEND
}
