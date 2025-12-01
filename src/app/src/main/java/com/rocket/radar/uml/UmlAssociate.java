package com.rocket.radar.uml;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.ElementType;
import java.lang.annotation.Target;

/**
 * Annotation to mark fields that represent an association relationship in UML diagrams.
 * This is used for documentation purposes to indicate a general relationship between classes
 * where instances of one class are connected to instances of another.
 */
@Retention(RetentionPolicy.CLASS)
@Target(ElementType.FIELD)
public @interface UmlAssociate {
    /**
     * The cardinality on the side of the class containing this field.
     * @return The cardinality string (e.g., "1", "*", "0..1").
     */
    String selfCard() default "1";

    /**
     * A descriptive label for the relationship.
     * @return The relationship label.
     */
    String label() default "";

    /**
     * The cardinality on the side of the other class in the relationship.
     * @return The cardinality string (e.g., "1", "*", "0..1").
     */
    String otherCard() default "1";
}
