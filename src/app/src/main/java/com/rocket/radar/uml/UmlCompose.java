package com.rocket.radar.uml;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.ElementType;
import java.lang.annotation.Target;


@Retention(RetentionPolicy.CLASS)
@Target(ElementType.FIELD)
public @interface UmlCompose {
    String selfCard() default "1";
    String label() default "";
    String otherCard() default "1";
}
