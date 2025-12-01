package com.rocket.radar.uml;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.ElementType;
import java.lang.annotation.Target;

/**
 * Annotation to mark types, fields, or methods that should be excluded from UML diagram generation.
 * This is useful for implementation details that don't need to be shown in architectural documentation.
 */
@Retention(RetentionPolicy.CLASS)
@Target({ElementType.TYPE, ElementType.FIELD, ElementType.METHOD})
public @interface UmlSkip {
}
